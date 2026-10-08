"""
뉴스 크롤링 관련 함수들 - 개선된 버전
"""
import requests
from bs4 import BeautifulSoup
import re
import logging
from config import REQUEST_TIMEOUT, USER_AGENT, MIN_ARTICLE_LEN
from text_processing import clean_text_advanced, extract_sentences, clean_title, combine_title_and_content

logger = logging.getLogger(__name__)


def get_unwanted_selectors():
    """제거할 HTML 요소들의 CSS 선택자 목록"""
    return [
        'script', 'style', 'nav', 'header', 'footer', 'aside',
        '.show-for-sr', '.sr-only', '.screen-reader-text', '.visually-hidden',
        '.ad', '.advertisement', '.related', '.recommend',
        '.comment', '.share', '.social', '.sns',
        '.menu', '.navigation', '.breadcrumb',
        '.footer-info', '.copyright',
        '.tags', '.category', '.reporter', '.date-info',
        '.article-bottom', '.article-sns-share', '.article-print',
        '.news-sns', '.news-share', '.news-print',
        '.reporter-area', '.journalist', '.byline',
        '.related-article', '.related-news', '.recommend-news',
        '.hot-news', '.most-view', '.ranking',
        'iframe', 'embed', 'object',
        '.photo-table', '.image-caption', '.img-desc',
        '.vod-area', '.video-area',
        '[class*="banner"]', '[class*="popup"]',
        '.subscribe', '.newsletter',
        '.article-ad', '.content-ad',
        '.end-ad', '.mid-ad',
        '[id*="taboola"]', '[class*="taboola"]',
        '[class*="outbrain"]',
        '.dable', '[class*="dable"]',
    ]


def remove_unwanted_elements(soup):
    """불필요한 HTML 요소들 제거"""
    unwanted_selectors = get_unwanted_selectors()

    for selector in unwanted_selectors:
        for element in soup.select(selector):
            element.decompose()


def extract_images_from_article(soup, base_url):
    """기사에서 이미지 URL들을 추출하는 함수"""
    images = []

    # 기사 내 이미지 선택자들
    image_selectors = [
        'article img',
        '.article img',
        '.news_body img',
        '.article_body img',
        '.newsct_article img',
        '#articleBodyContents img',
        '.read_body img',
        '.view_txt img',
        '.articleView img',
        'div[itemprop="articleBody"] img',
        '.content img'
    ]

    img_tags = []
    for selector in image_selectors:
        img_tags.extend(soup.select(selector))

    # 일반적인 img 태그도 확인
    if not img_tags:
        img_tags = soup.find_all('img')

    for img in img_tags:
        src = img.get('src') or img.get('data-src') or img.get('data-original')
        if src:
            # 상대 경로를 절대 경로로 변환
            if src.startswith('//'):
                src = 'https:' + src
            elif src.startswith('/'):
                from urllib.parse import urljoin
                src = urljoin(base_url, src)

            # 유효한 이미지 URL인지 확인
            if src.startswith('http') and any(ext in src.lower() for ext in ['.jpg', '.jpeg', '.png', '.gif', '.webp']):
                # 광고나 아이콘 이미지 필터링
                if not any(unwanted in src.lower() for unwanted in ['logo', 'icon', 'banner', 'ad', 'btn', 'bullet']):
                    if src not in images:
                        images.append(src)

    # 최대 5개까지만 반환
    return images[:5]


def extract_naver_news(soup, url):
    """네이버 뉴스 추출"""
    title = ""
    content = ""

    # 제목 추출 - 다양한 방법 시도
    title_element = soup.find('h2', {'id': 'title_area'})
    if title_element:
        title_span = title_element.find('span')
        if title_span:
            title = title_span.get_text(strip=True)
        else:
            title = title_element.get_text(strip=True)

    # 메타 태그에서 제목 추출
    if not title:
        meta_title = soup.find('meta', {'property': 'og:title'})
        if meta_title and meta_title.get('content'):
            title = meta_title.get('content', '')

    # title 태그에서 제목 추출
    if not title:
        title_tag = soup.find('title')
        if title_tag:
            title = title_tag.get_text(strip=True)

    # 본문 추출
    article_element = soup.find('article', {'id': 'dic_area'}) or soup.find('div', {'id': 'dic_area'})

    if article_element:
        # 불필요한 요소 제거
        for elem in article_element.find_all(['script', 'style']):
            elem.decompose()

        # 이미지 관련 요소 제거
        for elem in article_element.find_all('div', class_=['ab_photo', 'end_photo_org', 'image']):
            elem.decompose()

        # span 태그 중 이미지 관련 제거
        for elem in article_element.find_all('span', class_=['end_photo_org', 'mask']):
            elem.decompose()

        content = article_element.get_text(separator=' ', strip=True)

    # 본문이 없으면 메타 태그에서 추출
    if not content or len(content) < 50:
        meta_desc = soup.find('meta', {'property': 'og:description'})
        if meta_desc and meta_desc.get('content'):
            content = meta_desc.get('content', '')

    logger.info(f"네이버 뉴스 - 제목 길이: {len(title)}, 내용 길이: {len(content)}")

    return title, content


def extract_daum_news(soup, url):
    """다음 뉴스 추출"""
    title_element = soup.find('h3', {'class': 'tit_view'}) or soup.find('h1', {'class': 'title_news'})
    title = title_element.get_text(strip=True) if title_element else ""

    article_body = soup.find('div', {'class': ['article_view', 'news_view']}) or soup.find('section',
                                                                                           {'class': 'news_view'})
    content = ""

    if article_body:
        paragraphs = article_body.find_all('p')
        if paragraphs:
            content = ' '.join([p.get_text(strip=True) for p in paragraphs if len(p.get_text(strip=True)) > 20])
        else:
            content = article_body.get_text(strip=True)

    return title, content


def extract_hankyung_news(soup, url):
    """한국경제 뉴스 추출"""
    title_element = soup.find('h1', {'class': 'headline'}) or soup.find('h1', {'class': 'title'})
    title = title_element.get_text(strip=True) if title_element else ""

    article_body = soup.find('div', {'id': 'articletxt'}) or soup.find('div', {'class': 'article-body'})
    content = article_body.get_text(separator=' ', strip=True) if article_body else ""

    return title, content


def extract_mk_news(soup, url):
    """매일경제 뉴스 추출"""
    title_element = soup.find('h1', {'class': 'news_ttl'}) or soup.find('h2', {'class': 'news_ttl'})
    title = title_element.get_text(strip=True) if title_element else ""

    article_body = soup.find('div', {'class': 'news_cnt_detail_wrap'}) or \
                   soup.find('div', {'id': 'article_body'}) or \
                   soup.find('div', {'class': 'art_txt'})

    content = ""
    if article_body:
        for tag in article_body.find_all(['script', 'style']):
            tag.decompose()
        content = article_body.get_text(separator=' ', strip=True)

    return title, content


def extract_chosun_news(soup, url):
    """조선일보 뉴스 추출"""
    title_element = soup.find('h1', {'class': 'article-header__headline'})
    title = title_element.get_text(strip=True) if title_element else ""

    article_body = soup.find('section', {'class': 'article-body'})
    content = ""

    if article_body:
        paragraphs = article_body.find_all('p')
        content = ' '.join([p.get_text(strip=True) for p in paragraphs if len(p.get_text(strip=True)) > 20])

    return title, content


def extract_general_news(soup, url):
    """일반적인 뉴스 사이트 추출"""
    title = ""
    content = ""

    # 메타 태그에서 정보 추출
    og_title = soup.find('meta', {'property': 'og:title'})
    if og_title:
        title = og_title.get('content', '')

    og_desc = soup.find('meta', {'property': 'og:description'})
    article_desc = soup.find('meta', {'name': 'description'})

    if og_desc:
        content = og_desc.get('content', '')
    elif article_desc:
        content = article_desc.get('content', '')

    # 본문 추출 시도
    if not content or len(content) < 100:
        article_selectors = [
            'article',
            '[role="main"]',
            'div[class*="article_body"]',
            'div[class*="articleBody"]',
            'div[class*="content_text"]',
            'div[class*="news_text"]',
            'div[id*="article"]',
            'main'
        ]

        for selector in article_selectors:
            article_elem = soup.select_one(selector)
            if article_elem:
                paragraphs = article_elem.find_all(['p', 'div'])
                temp_content = ' '.join(
                    [p.get_text(strip=True) for p in paragraphs if len(p.get_text(strip=True)) > 30])
                if len(temp_content) > len(content):
                    content = temp_content

    return title, content


def extract_news_content_with_title(url):
    """뉴스 URL에서 제목과 본문 내용을 추출하는 함수 - 개선된 버전"""
    try:
        headers = {'User-Agent': USER_AGENT}
        response = requests.get(url, headers=headers, timeout=REQUEST_TIMEOUT)
        response.raise_for_status()
        response.encoding = response.apparent_encoding

        soup = BeautifulSoup(response.content, 'html.parser')
        remove_unwanted_elements(soup)

        # 사이트별 추출 로직
        if 'news.naver.com' in url:
            title, content = extract_naver_news(soup, url)
        elif 'news.daum.net' in url or 'v.daum.net' in url:
            title, content = extract_daum_news(soup, url)
        elif 'hankyung.com' in url:
            title, content = extract_hankyung_news(soup, url)
        elif 'mk.co.kr' in url:
            title, content = extract_mk_news(soup, url)
        elif 'chosun.com' in url:
            title, content = extract_chosun_news(soup, url)
        else:
            title, content = extract_general_news(soup, url)

        # 제목이 없으면 title 태그에서 추출
        if not title:
            title_tag = soup.find('title')
            if title_tag:
                title = title_tag.get_text(strip=True)
                title = re.split(r'[-|:]', title)[0].strip()

        # 텍스트 정제
        title = clean_text_advanced(title)
        content = clean_text_advanced(content)

        # 핵심 문장만 추출 (너무 긴 경우)
        if len(content) > 3000:
            content = extract_sentences(content)

        # 제목 정제
        title = clean_title(title)

        # 이미지 추출
        images = extract_images_from_article(soup, url)

        # 최종 결합
        combined_text = combine_title_and_content(title, content)

        logger.info(f"제목: {title[:50]}...")
        logger.info(f"내용 길이: {len(content)}자")
        logger.info(f"결합 텍스트 길이: {len(combined_text)}자")
        logger.info(f"이미지 개수: {len(images)}개")

        return {
            'title': title,
            'content': content,
            'combined_text': combined_text,
            'images': images
        }

    except Exception as e:
        logger.error(f"뉴스 내용 추출 실패 ({url}): {e}")
        raise e


def extract_naver_news_original(soup, url):
    """네이버 뉴스 추출 (일반 뉴스 + 증권 뉴스) - 기존 함수 유지"""
    title = ""
    content = ""

    # 네이버 증권 뉴스인지 확인
    is_finance_news = 'finance.naver.com' in url

    if is_finance_news:
        # 네이버 증권 뉴스 페이지 구조
        # 제목 추출
        title_element = soup.find('div', {'class': 'article_info'})
        if title_element:
            h3_title = title_element.find('h3')
            if h3_title:
                title = h3_title.get_text(strip=True)

        # 본문 추출
        content_element = soup.find('div', {'class': 'articleCont'}) or soup.find('div', {'id': 'content'})
        if content_element:
            # 불필요한 요소 제거
            for elem in content_element.find_all(['script', 'style', 'iframe']):
                elem.decompose()
            content = content_element.get_text(separator=' ', strip=True)
    else:
        # 일반 네이버 뉴스 페이지 구조
        # 제목 추출 - 다양한 방법 시도
        title_element = soup.find('h2', {'id': 'title_area'})
        if title_element:
            title_span = title_element.find('span')
            if title_span:
                title = title_span.get_text(strip=True)
            else:
                title = title_element.get_text(strip=True)

        # 본문 추출
        article_element = soup.find('article', {'id': 'dic_area'}) or soup.find('div', {'id': 'dic_area'})

        if article_element:
            # 불필요한 요소 제거
            for elem in article_element.find_all(['script', 'style']):
                elem.decompose()

            # 이미지 관련 요소 제거
            for elem in article_element.find_all('div', class_=['ab_photo', 'end_photo_org', 'image']):
                elem.decompose()

            # span 태그 중 이미지 관련 제거
            for elem in article_element.find_all('span', class_=['end_photo_org', 'mask']):
                elem.decompose()

            content = article_element.get_text(separator=' ', strip=True)

    # 제목이 없으면 메타 태그에서 추출
    if not title:
        meta_title = soup.find('meta', {'property': 'og:title'})
        if meta_title and meta_title.get('content'):
            title = meta_title.get('content', '')

    # title 태그에서 제목 추출
    if not title:
        title_tag = soup.find('title')
        if title_tag:
            title = title_tag.get_text(strip=True)

    # 본문이 없으면 메타 태그에서 추출
    if not content or len(content) < 50:
        meta_desc = soup.find('meta', {'property': 'og:description'})
        if meta_desc and meta_desc.get('content'):
            content = meta_desc.get('content', '')

    logger.info(f"네이버 뉴스 - 제목 길이: {len(title)}, 내용 길이: {len(content)}, 증권뉴스: {is_finance_news}")

    return title, content


def extract_daum_news(soup, url):
    """다음 뉴스 추출"""
    title_element = soup.find('h3', {'class': 'tit_view'}) or soup.find('h1', {'class': 'title_news'})
    title = title_element.get_text(strip=True) if title_element else ""

    article_body = soup.find('div', {'class': ['article_view', 'news_view']}) or soup.find('section',
                                                                                           {'class': 'news_view'})
    content = ""

    if article_body:
        paragraphs = article_body.find_all('p')
        if paragraphs:
            content = ' '.join([p.get_text(strip=True) for p in paragraphs if len(p.get_text(strip=True)) > 20])
        else:
            content = article_body.get_text(strip=True)

    return title, content


def extract_hankyung_news(soup, url):
    """한국경제 뉴스 추출"""
    title_element = soup.find('h1', {'class': 'headline'}) or soup.find('h1', {'class': 'title'})
    title = title_element.get_text(strip=True) if title_element else ""

    article_body = soup.find('div', {'id': 'articletxt'}) or soup.find('div', {'class': 'article-body'})
    content = article_body.get_text(separator=' ', strip=True) if article_body else ""

    return title, content


def extract_mk_news(soup, url):
    """매일경제 뉴스 추출"""
    title_element = soup.find('h1', {'class': 'news_ttl'}) or soup.find('h2', {'class': 'news_ttl'})
    title = title_element.get_text(strip=True) if title_element else ""

    article_body = soup.find('div', {'class': 'news_cnt_detail_wrap'}) or \
                   soup.find('div', {'id': 'article_body'}) or \
                   soup.find('div', {'class': 'art_txt'})

    content = ""
    if article_body:
        for tag in article_body.find_all(['script', 'style']):
            tag.decompose()
        content = article_body.get_text(separator=' ', strip=True)

    return title, content


def extract_chosun_news(soup, url):
    """조선일보 뉴스 추출"""
    title_element = soup.find('h1', {'class': 'article-header__headline'})
    title = title_element.get_text(strip=True) if title_element else ""

    article_body = soup.find('section', {'class': 'article-body'})
    content = ""

    if article_body:
        paragraphs = article_body.find_all('p')
        content = ' '.join([p.get_text(strip=True) for p in paragraphs if len(p.get_text(strip=True)) > 20])

    return title, content


def extract_general_news(soup, url):
    """일반적인 뉴스 사이트 추출"""
    title = ""
    content = ""

    # 메타 태그에서 정보 추출
    og_title = soup.find('meta', {'property': 'og:title'})
    if og_title:
        title = og_title.get('content', '')

    og_desc = soup.find('meta', {'property': 'og:description'})
    article_desc = soup.find('meta', {'name': 'description'})

    if og_desc:
        content = og_desc.get('content', '')
    elif article_desc:
        content = article_desc.get('content', '')

    # 본문 추출 시도
    if not content or len(content) < 100:
        article_selectors = [
            'article',
            '[role="main"]',
            'div[class*="article_body"]',
            'div[class*="articleBody"]',
            'div[class*="content_text"]',
            'div[class*="news_text"]',
            'div[id*="article"]',
            'main'
        ]

        for selector in article_selectors:
            article_elem = soup.select_one(selector)
            if article_elem:
                paragraphs = article_elem.find_all(['p', 'div'])
                temp_content = ' '.join(
                    [p.get_text(strip=True) for p in paragraphs if len(p.get_text(strip=True)) > 30])
                if len(temp_content) > len(content):
                    content = temp_content

    return title, content


def extract_news_content_with_title(url):
    """뉴스 URL에서 제목과 본문 내용을 추출하는 함수"""
    try:
        headers = {'User-Agent': USER_AGENT}
        response = requests.get(url, headers=headers, timeout=REQUEST_TIMEOUT)
        response.raise_for_status()
        response.encoding = response.apparent_encoding

        soup = BeautifulSoup(response.content, 'html.parser')
        remove_unwanted_elements(soup)

        # 사이트별 추출 로직
        if 'news.naver.com' in url:
            title, content = extract_naver_news(soup, url)
        elif 'news.daum.net' in url or 'v.daum.net' in url:
            title, content = extract_daum_news(soup, url)
        elif 'hankyung.com' in url:
            title, content = extract_hankyung_news(soup, url)
        elif 'mk.co.kr' in url:
            title, content = extract_mk_news(soup, url)
        elif 'chosun.com' in url:
            title, content = extract_chosun_news(soup, url)
        else:
            title, content = extract_general_news(soup, url)

        # 제목이 없으면 title 태그에서 추출
        if not title:
            title_tag = soup.find('title')
            if title_tag:
                title = title_tag.get_text(strip=True)
                title = re.split(r'[-|:]', title)[0].strip()

        # 텍스트 정제
        title = clean_text_advanced(title)
        content = clean_text_advanced(content)
        # 본문이 너무 짧으면 선택자 확장으로 보강 시도
        try:
            if not content or len(content) < MIN_ARTICLE_LEN:
                html_raw = response.text
                soup2 = BeautifulSoup(html_raw, "html.parser")
                for sel in ["#newsct_article", ".article-body", ".article-content", "article", ".entry-content", ".post-content"]:
                    node = soup2.select_one(sel)
                    if not node:
                        continue
                    for t in node.find_all(["script","style","nav","footer","aside"]):
                        t.decompose()
                    txt = node.get_text(" ", strip=True)
                    if len(txt) >= MIN_ARTICLE_LEN:
                        content = txt
                        break
        except Exception:
            pass
        

        # 핵심 문장만 추출 (너무 긴 경우)
        if len(content) > 3000:
            content = extract_sentences(content)

        # 제목 정제
        title = clean_title(title)

        # 이미지 추출
        images = extract_images_from_article(soup, url)

        # 최종 결합
        combined_text = combine_title_and_content(title, content)

        logger.info(f"제목: {title[:50]}...")
        logger.info(f"내용 길이: {len(content)}자")
        logger.info(f"결합 텍스트 길이: {len(combined_text)}자")
        logger.info(f"추출된 이미지: {len(images)}개")

        return {
            'title': title,
            'content': content,
            'combined_text': combined_text,
            'images': images
        }

    except Exception as e:
        logger.error(f"뉴스 내용 추출 실패 ({url}): {e}")
        raise e