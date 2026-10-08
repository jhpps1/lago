from flask import Flask, request, jsonify
import logging
from typing import List, Optional, Dict
import threading
import time
from functools import wraps
import requests
from datetime import datetime, timedelta

# 기존 import들
from config import HOST, PORT, DEBUG, LOG_LEVEL, LOG_FORMAT, TEXT_MAX_LENGTH
from news_crawler import extract_news_content_with_title
from sentiment_analysis import load_model, analyze_sentiment, is_model_loaded
from google_rss_crawler import GoogleRSSCrawler, NewsItem
from content_block_parser import ContentBlockParser
from parallel_processor import ParallelNewsProcessor  # 병렬 처리 추가
try:
    from cache_manager import cache_manager  # Redis 기반 캐시 매니저
except ImportError:
    from simple_cache import cache_manager  # Redis 없을 때 메모리 캐시

# 로깅 설정
logging.basicConfig(level=LOG_LEVEL, format=LOG_FORMAT)
logger = logging.getLogger(__name__)

app = Flask(__name__)


class StrictSequentialNewsProcessor:
    """엄격한 순차 처리 뉴스 수집 서비스 (병렬 처리 지원)"""

    def __init__(self):
        self._local = threading.local()
        self.claude_request_delay = 3.0  # Claude API 요청 간 3초 대기
        self.last_claude_request = 0
        self.url_redirect_timeout = 10  # URL 리디렉션 타임아웃
        self.min_content_length = 200  # 최소 콘텐츠 길이
        self.collection_stats = {}  # 수집 통계
        self.parallel_processor = ParallelNewsProcessor(max_workers=4)  # 병렬 처리 엔진
        self.enable_parallel = True  # 병렬 처리 활성화 플래그

    def get_crawler(self):
        """스레드별 크롤러 인스턴스 반환"""
        if not hasattr(self._local, 'crawler'):
            try:
                self._local.crawler = GoogleRSSCrawler()
                logger.debug(f"새 크롤러 인스턴스 생성 - 스레드: {threading.current_thread().name}")
            except Exception as e:
                logger.error(f"크롤러 인스턴스 생성 실패: {e}")
                raise
        return self._local.crawler

    def get_content_parser(self):
        """스레드별 콘텐츠 파서 인스턴스 반환"""
        if not hasattr(self._local, 'content_parser'):
            try:
                self._local.content_parser = ContentBlockParser()
                logger.debug(f"새 콘텐츠 파서 인스턴스 생성 - 스레드: {threading.current_thread().name}")
            except Exception as e:
                logger.error(f"콘텐츠 파서 인스턴스 생성 실패: {e}")
                raise
        return self._local.content_parser

    def cleanup_local_resources(self):
        """현재 스레드의 리소스 정리"""
        if hasattr(self._local, 'crawler'):
            try:
                if hasattr(self._local.crawler, 'cleanup'):
                    self._local.crawler.cleanup()
                logger.debug(f"크롤러 리소스 정리 완료")
            except Exception as e:
                logger.error(f"크롤러 정리 오류: {e}")
            finally:
                del self._local.crawler

        if hasattr(self._local, 'content_parser'):
            try:
                if hasattr(self._local.content_parser, 'cleanup'):
                    self._local.content_parser.cleanup()
                del self._local.content_parser
            except Exception as e:
                logger.error(f"콘텐츠 파서 정리 오류: {e}")

    def wait_for_claude_rate_limit(self):
        """Claude API Rate Limiting"""
        current_time = time.time()
        elapsed = current_time - self.last_claude_request

        if elapsed < self.claude_request_delay:
            wait_time = self.claude_request_delay - elapsed
            logger.info(f"⏳ Claude API Rate Limit: {wait_time:.1f}초 대기")
            time.sleep(wait_time)

        self.last_claude_request = time.time()

    def verify_and_redirect_url(self, url: str, index: int, total: int) -> Optional[str]:
        """URL 유효성 검증 및 리디렉션 처리 (캐싱 적용)"""
        try:
            # 캐시 확인
            cached_url = cache_manager.get_url_redirect_cache(url)
            if cached_url:
                logger.info(f"🎯 [{index}/{total}] URL 캐시 히트")
                return cached_url if cached_url != "FAILED" else None
            
            logger.info(f"🔗 [{index}/{total}] URL 검증 및 리디렉션 중...")
            logger.debug(f"   원본 RSS URL: {url}")

            response = requests.head(url, allow_redirects=True, timeout=self.url_redirect_timeout)
            final_url = response.url

            if response.status_code >= 400:
                logger.error(f"❌ [{index}/{total}] URL 접근 실패 (HTTP {response.status_code})")
                # 실패도 캐싱 (짧은 시간)
                cache_manager.set_url_redirect_cache(url, "FAILED", ttl=300)  # 5분
                return None

            # URL이 변경되었는지 확인
            if final_url != url:
                logger.info(f"✅ [{index}/{total}] URL 리디렉션 완료")
                logger.info(f"   RSS URL: {url[:80]}...")
                logger.info(f"   실제 URL: {final_url[:80]}...")
            else:
                logger.info(f"✅ [{index}/{total}] URL 검증 완료 (리디렉션 없음)")
            
            # 성공 캐싱
            cache_manager.set_url_redirect_cache(url, final_url, ttl=3600)  # 1시간
            return final_url

        except requests.exceptions.Timeout:
            logger.error(f"⏰ [{index}/{total}] URL 리디렉션 타임아웃")
            cache_manager.set_url_redirect_cache(url, "FAILED", ttl=300)
            return None
        except Exception as e:
            logger.error(f"❌ [{index}/{total}] URL 검증 실패: {e}")
            cache_manager.set_url_redirect_cache(url, "FAILED", ttl=300)
            return None

    def extract_content_strict(self, url: str, content_parser: ContentBlockParser,
                               index: int, total: int) -> Optional[Dict]:
        """엄격한 콘텐츠 추출"""
        try:
            logger.info(f"📄 [{index}/{total}] 콘텐츠 추출 시작...")

            parsed_content = content_parser.parse_url(url)

            if not parsed_content:
                logger.error(f"❌ [{index}/{total}] 파싱 결과가 없음")
                return None

            content_text = " ".join(
                b.content for b in parsed_content.blocks if b.type == "text"
            ).strip()

            if len(content_text) < self.min_content_length:
                logger.warning(f"⚠️ [{index}/{total}] 콘텐츠가 너무 짧음 ({len(content_text)}자)")

                try:
                    fallback = extract_news_content_with_title(url)
                    if fallback and len(fallback.get("content", "")) >= self.min_content_length:
                        content_text = fallback["content"]
                        logger.info(f"✅ [{index}/{total}] Fallback 파서로 보완 성공")
                    else:
                        logger.error(f"❌ [{index}/{total}] Fallback도 실패")
                        return None
                except Exception as e:
                    logger.error(f"❌ [{index}/{total}] Fallback 파서 실패: {e}")
                    return None

            images = [block.content for block in parsed_content.blocks if block.type == 'image']

            logger.info(f"✅ [{index}/{total}] 콘텐츠 추출 성공 ({len(content_text)}자)")

            return {
                'title': parsed_content.title,
                'content_text': content_text,
                'summary_text': parsed_content.summary_text,
                'images': images,
                'main_image': images[0] if images else None,
                'parser_used': parsed_content.parser_used,
                'confidence': parsed_content.confidence,
                'blocks': parsed_content.blocks[:20]
            }

        except Exception as e:
            logger.error(f"❌ [{index}/{total}] 콘텐츠 추출 실패: {e}")
            return None

    def analyze_sentiment_strict(self, url: str, index: int, total: int) -> Dict:
        """엄격한 감성 분석 (캐싱 적용) - URL 기반"""
        try:
            logger.info(f"🧠 [{index}/{total}] FinBERT 감성 분석 시작...")

            if not url or not url.startswith(('http://', 'https://')):
                logger.warning(f"⚠️ [{index}/{total}] 분석할 URL이 유효하지 않음")
                raise ValueError("URL이 유효하지 않음")

            # 캐시 확인 (URL 기준)
            cached_result = cache_manager.get_sentiment_cache(url)
            if cached_result:
                logger.info(f"🎯 [{index}/{total}] FinBERT 캐시 히트: {cached_result.get('label')}")
                return cached_result

            # FinBERT Flask 서버에 URL 전송하여 분석
            import requests
            finbert_url = "http://finbert-service:8000/analyze"
            
            payload = {"url": url}
            headers = {"Content-Type": "application/json"}
            
            response = requests.post(finbert_url, json=payload, headers=headers, timeout=30)
            response.raise_for_status()
            
            analysis_result = response.json()
            logger.info(f"✅ [{index}/{total}] FinBERT 분석 완료: {analysis_result.get('label')}")
            
            # 캐시 저장 (URL 기준)
            cache_manager.set_sentiment_cache(url, analysis_result, ttl=86400)  # 24시간
            return analysis_result

        except Exception as e:
            logger.error(f"❌ [{index}/{total}] FinBERT 분석 실패: {e}")
            return {
                'label': 'NEUTRAL',
                'score': 0.0,
                'confidence': 0.0,
                'confidence_level': 'LOW',
                'trading_signal': 'HOLD',
                'signal_strength': 'WEAK',
                'error': str(e)
            }

    def generate_gpt_summary_strict(self, title: str, content: str,
                                   index: int, total: int) -> List[str]:
        """엄격한 GPT 요약 생성 (캐싱 및 재시도 로직 포함)"""
        try:
            # 캐시 확인
            cached_summary = cache_manager.get_summary_cache(title, content)
            if cached_summary:
                logger.info(f"🎯 [{index}/{total}] GPT 요약 캐시 히트")
                return cached_summary
            
            self.wait_for_claude_rate_limit()  # Rate limiting 유지

            logger.info(f"📝 [{index}/{total}] GPT 요약 요청 시작...")

            # OpenAI GPT API 호출
            import openai
            
            # API 키는 환경변수에서 가져오기 (없으면 fallback)
            import os
            openai.api_key = os.getenv('OPENAI_API_KEY', 'your-api-key-here')
            
            if openai.api_key == 'your-api-key-here':
                logger.warning(f"⚠️ [{index}/{total}] OpenAI API 키가 설정되지 않음, Fallback 사용")
                return self._generate_fallback_summary(title, content)

            # GPT 프롬프트 준비
            prompt = f"""다음 뉴스를 3줄로 요약해주세요:

제목: {title[:200]}

내용: {content[:1500]}

요구사항:
- 3줄 이내로 요약
- 각 줄은 완전한 문장으로 작성
- 핵심 내용과 중요한 정보 위주로 요약
- 투자/경제 관련 키워드 포함"""

            # 재시도 로직 추가 (최대 2회 시도)
            max_retries = 2
            for attempt in range(max_retries):
                try:
                    response = openai.ChatCompletion.create(
                        model="gpt-3.5-turbo",
                        messages=[
                            {"role": "system", "content": "당신은 금융 뉴스 요약 전문가입니다. 간결하고 정확한 요약을 제공합니다."},
                            {"role": "user", "content": prompt}
                        ],
                        max_tokens=200,
                        temperature=0.3,
                        timeout=30
                    )
                    
                    break  # 성공 시 루프 종료
                    
                except Exception as e:
                    if attempt < max_retries - 1:
                        logger.warning(f"⏰ [{index}/{total}] GPT API 오류, 재시도 {attempt+1}/{max_retries-1}: {e}")
                        time.sleep(2)  # 재시도 전 2초 대기
                        continue
                    else:
                        logger.error(f"❌ [{index}/{total}] GPT API 실패 (모든 재시도 실패): {e}")
                        return self._generate_fallback_summary(title, content)

            summary_text = response.choices[0].message.content.strip()

            if not summary_text:
                logger.warning(f"⚠️ [{index}/{total}] GPT 응답이 비어있음")
                return self._generate_fallback_summary(title, content)

            sentences = [
                s.strip() for s in summary_text.replace('\n', '.').split('.')
                if len(s.strip()) > 5
            ]

            result = sentences[:3] if sentences else []
            logger.info(f"✅ [{index}/{total}] GPT 요약 성공 ({len(result)}개 문장)")
            
            # 캐시 저장
            cache_manager.set_summary_cache(title, content, result, ttl=604800)  # 7일
            return result

        except Exception as e:
            logger.error(f"❌ [{index}/{total}] GPT 요약 실패: {e}")
            return self._generate_fallback_summary(title, content)
    
    def _generate_fallback_summary(self, title: str, content: str) -> List[str]:
        """GPT API 실패 시 기본 요약 생성"""
        try:
            # 제목과 컨텐츠 첫 부분을 활용한 간단한 요약
            summary = []
            
            # 제목 활용
            if title:
                summary.append(title[:100])
            
            # 컨텐츠 첫 200자 활용
            if content and len(content) > 50:
                first_sentence = content[:200].split('.')[0]
                if first_sentence:
                    summary.append(first_sentence.strip())
            
            logger.info(f"ℹ️ Fallback 요약 생성 완료")
            return summary[:2]  # 최대 2개 문장 반환
            
        except Exception as e:
            logger.error(f"Fallback 요약 생성 실패: {e}")
            return []

    def process_single_news_strict(self, news_item: NewsItem, content_parser: ContentBlockParser,
                                   index: int, total: int, collection_type: str = "REALTIME") -> Optional[Dict]:
        """엄격한 순차 처리"""
        try:
            logger.info(f"🔄 [{index}/{total}] 뉴스 처리 시작: {news_item.title[:50]}...")
            start_time = time.time()

            # STEP 1: URL 검증 및 리디렉션 (실패하면 스킵)
            final_url = self.verify_and_redirect_url(news_item.url, index, total)
            if not final_url:
                logger.error(f"❌ [{index}/{total}] URL 리디렉션 실패 - 실제 뉴스 URL을 찾을 수 없어 스킵")
                return None

            # STEP 2: 콘텐츠 추출 (실패하면 스킵)
            content_data = self.extract_content_strict(final_url, content_parser, index, total)
            if not content_data:
                logger.error(f"❌ [{index}/{total}] 콘텐츠 추출 실패 - 실제 뉴스 내용을 가져올 수 없어 스킵")
                return None

            # STEP 3: FinBERT 감성 분석 (실제 URL 전송)
            sentiment_result = self.analyze_sentiment_strict(final_url, index, total)

            # STEP 4: GPT 요약 생성
            summary_lines = self.generate_gpt_summary_strict(
                content_data['title'],
                content_data['content_text'],
                index,
                total
            )

            # STEP 5: 최종 결과 구성
            # 실제 URL과 RSS URL 명확히 구분
            logger.debug(f"📦 [{index}/{total}] 최종 URL 설정 - 실제: {final_url[:80]}..., RSS: {news_item.url[:80]}...")
            
            result = {
                'title': content_data['title'],
                'url': final_url,  # 실제 뉴스 페이지 URL (리디렉션 후)
                'original_url': news_item.url,  # RSS에서 제공한 원본 URL
                'source': news_item.source,
                'published_at': news_item.published_at.isoformat(),
                'category': news_item.category,
                'symbol': news_item.symbol,
                'collection_type': collection_type,  # 수집 타입 추가

                'content_text': content_data['content_text'][:5000],
                'summary_text': content_data['summary_text'],
                'summary_lines': summary_lines,

                'images': content_data['images'][:10],
                'main_image_url': content_data['main_image'],
                'total_images': len(content_data['images']),

                'content_blocks': [
                    {
                        'type': block.type,
                        'content': block.content[:1000] if block.type == 'text' else block.content,
                        'position': block.position,
                        'confidence': block.confidence
                    } for block in content_data['blocks']
                ],

                **sentiment_result,

                'parser_info': {
                    'parser_used': content_data['parser_used'],
                    'confidence': content_data['confidence'],
                    'processing_time': round(time.time() - start_time, 2)
                },
                'processing_status': 'success'
            }

            processing_time = time.time() - start_time
            logger.info(f"🎉 [{index}/{total}] 뉴스 처리 완료 ({processing_time:.2f}초)")
            return result

        except Exception as e:
            logger.error(f"❌ [{index}/{total}] 뉴스 처리 실패: {e}")
            return None
    
    def process_news_batch_parallel(self, news_items: List[NewsItem], 
                                   collection_type: str = "REALTIME") -> List[Optional[Dict]]:
        """뉴스 배치 병렬 처리"""
        if not news_items:
            return []
        
        logger.info(f"🚀 병렬 처리 모드로 {len(news_items)}개 뉴스 처리 시작")
        
        # content_parser를 가져와서 전달
        content_parser = self.get_content_parser()
        
        # 콘텐츠 추출 함수를 래핑
        def extract_content_wrapper(url, idx, total):
            return self.extract_content_strict(url, content_parser, idx, total)
        
        # 병렬 처리 엔진 사용
        results = self.parallel_processor.process_news_batch(
            news_items,
            url_processor=self.verify_and_redirect_url,
            content_processor=extract_content_wrapper,
            sentiment_processor=self.analyze_sentiment_strict,
            summary_processor=self.generate_gpt_summary_strict
        )
        
        # collection_type 설정
        for result in results:
            if result:
                result['collection_type'] = collection_type
        
        return results


# 서비스 인스턴스
news_processor = StrictSequentialNewsProcessor()


def monitor_performance(func):
    """성능 모니터링 데코레이터"""

    @wraps(func)
    def wrapper(*args, **kwargs):
        start_time = time.time()
        try:
            result = func(*args, **kwargs)
            duration = time.time() - start_time
            logger.info(f"✅ {func.__name__} 실행시간: {duration:.2f}초")
            return result
        except Exception as e:
            duration = time.time() - start_time
            logger.error(f"❌ {func.__name__} 실패 ({duration:.2f}초): {e}")
            raise

    return wrapper


@app.route('/health', methods=['GET'])
def health_check():
    """서버 상태 확인"""
    return jsonify({
        "status": "healthy",
        "model_loaded": is_model_loaded(),
        "thread_count": threading.active_count(),
        "processing_mode": "strict_sequential",
        "timestamp": datetime.now().isoformat()
    })


@app.route('/collect/realtime', methods=['POST'])
@monitor_performance
def collect_realtime_news():
    """실시간 뉴스 수집 API"""
    try:
        data = request.get_json() or {}
        requested_limit = data.get('limit', 10)

        limit = min(requested_limit, 10)

        logger.info(f"🚀 실시간 뉴스 순차 수집 시작 (요청: {requested_limit}, 실제: {limit})")

        crawler = news_processor.get_crawler()
        content_parser = news_processor.get_content_parser()

        # RSS에서 뉴스 URL 수집
        logger.info("📡 RSS에서 뉴스 URL 수집 중...")
        news_items = crawler.get_realtime_news(limit)

        if not news_items:
            logger.warning("⚠️ 수집된 뉴스가 없습니다")
            return jsonify({
                'success': True,
                'count': 0,
                'news': [],
                'metadata': {
                    'requested': requested_limit,
                    'collected': 0,
                    'processed': 0,
                    'failed': 0,
                    'collection_type': 'REALTIME'
                }
            })

        logger.info(f"✅ {len(news_items)}개 뉴스 URL 수집 완료")

        # 병렬 처리 모드 확인 (현재 비활성화)
        if False and news_processor.enable_parallel and len(news_items) >= 3:
            # 병렬 처리 (3개 이상일 때)
            logger.info(f"🚀 병렬 처리 모드 활성화 ({len(news_items)}개)")
            results = news_processor.process_news_batch_parallel(news_items, "REALTIME")
            
            # None 제거 및 통계 계산
            valid_results = [r for r in results if r is not None]
            failed_count = len(results) - len(valid_results)
            results = valid_results
            
        else:
            # 순차 처리 (소량일 때)
            logger.info(f"📝 순차 처리 모드 ({len(news_items)}개)")
            results = []
            failed_count = 0

            for idx, news_item in enumerate(news_items, 1):
                try:
                    logger.info(f"\n{'=' * 60}")
                    logger.info(f"⏳ [{idx}/{len(news_items)}] 처리 중...")

                    result = news_processor.process_single_news_strict(
                        news_item, content_parser, idx, len(news_items), "REALTIME"
                    )

                    if result:
                        results.append(result)
                        logger.info(f"✅ [{idx}/{len(news_items)}] 성공")
                    else:
                        failed_count += 1
                        logger.warning(f"⚠️ [{idx}/{len(news_items)}] 실패")

                    if idx < len(news_items):
                        time.sleep(1)

                except Exception as e:
                    failed_count += 1
                    logger.error(f"❌ [{idx}/{len(news_items)}] 예외 발생: {e}")
                    continue

        success_rate = len(results) / len(news_items) * 100 if news_items else 0
        logger.info(f"🎉 완료 - 성공: {len(results)}, 실패: {failed_count}, 성공률: {success_rate:.1f}%")

        return jsonify({
            'success': True,
            'count': len(results),
            'news': results,
            'metadata': {
                'requested': requested_limit,
                'collected': len(news_items),
                'processed': len(results),
                'failed': failed_count,
                'success_rate': f"{success_rate:.1f}%",
                'collection_type': 'REALTIME'
            }
        })

    except Exception as e:
        logger.error(f"❌ 실시간 뉴스 수집 실패: {e}")
        return jsonify({'error': f'Failed to collect realtime news: {str(e)}'}), 500

    finally:
        try:
            news_processor.cleanup_local_resources()
            logger.info("🧹 리소스 정리 완료")
        except Exception as e:
            logger.error(f"❌ 리소스 정리 오류: {e}")


@app.route('/collect/watchlist', methods=['POST'])
@monitor_performance
def collect_single_watchlist_news():
    """단일 관심종목 뉴스 수집 API (Spring Boot에서 호출)"""
    try:
        logger.info("📥 관심종목 뉴스 수집 API 호출됨")
        logger.info(f"Request headers: {dict(request.headers)}")
        logger.info(f"Request data: {request.data}")
        logger.info(f"Request content type: {request.content_type}")
        
        # 한글 인코딩 문제 해결 - 여러 인코딩 방식 시도
        data = None
        try:
            data = request.get_json(force=True)
            logger.info(f"Parsed JSON data: {data}")
        except Exception as json_error:
            logger.error(f"JSON 파싱 오류: {json_error}")
            # 여러 인코딩 방식으로 시도
            encoding_attempts = ['utf-8', 'euc-kr', 'cp949', 'iso-8859-1']
            
            for encoding in encoding_attempts:
                try:
                    import json
                    raw_data = request.data.decode(encoding)
                    logger.info(f"Raw data with {encoding}: {raw_data}")
                    data = json.loads(raw_data)
                    logger.info(f"Successfully parsed with {encoding}: {data}")
                    break
                except Exception as enc_error:
                    logger.debug(f"인코딩 {encoding} 실패: {enc_error}")
                    continue
            
            if data is None:
                # 마지막 시도: bytes를 직접 처리
                try:
                    import json
                    # raw bytes를 Latin-1로 디코딩한 후 다시 UTF-8로 인코딩/디코딩
                    raw_bytes = request.data
                    temp_str = raw_bytes.decode('latin-1')
                    final_str = temp_str.encode('latin-1').decode('utf-8')
                    logger.info(f"Final attempt data: {final_str}")
                    data = json.loads(final_str)
                    logger.info(f"Final attempt success: {data}")
                except Exception as final_error:
                    logger.error(f"모든 인코딩 시도 실패: {final_error}")
                    return jsonify({'error': f'JSON parsing failed with all encoding attempts: {str(json_error)}'}), 400
        
        if not data:
            logger.error("❌ JSON data가 비어있음")
            return jsonify({'error': 'JSON data required'}), 400

        symbol = data.get('symbol')
        company_name = data.get('company_name')
        aliases = data.get('aliases', [])
        limit = min(data.get('limit', 5), 10)  # 종목당 최대 10개

        if not symbol or not company_name:
            return jsonify({'error': 'symbol and company_name required'}), 400

        logger.info(f"🚀 관심종목 뉴스 수집: {company_name} ({symbol})")

        crawler = news_processor.get_crawler()
        content_parser = news_processor.get_content_parser()

        # Google RSS로 해당 종목 뉴스 수집
        news_items = crawler.get_watchlist_news(symbol, company_name, aliases, limit)

        if not news_items:
            logger.warning(f"⚠️ 뉴스가 없습니다: {company_name}")
            return jsonify({
                'success': True,
                'symbol': symbol,
                'company_name': company_name,
                'count': 0,
                'news': []
            })

        # 순차 처리
        results = []
        failed_count = 0

        for idx, news_item in enumerate(news_items, 1):
            try:
                # 종목 정보 설정
                news_item.symbol = symbol

                result = news_processor.process_single_news_strict(
                    news_item, content_parser, idx, len(news_items), "WATCHLIST"
                )

                if result:
                    # 종목 정보 확실히 설정
                    result['symbol'] = symbol
                    result['company_name'] = company_name
                    results.append(result)
                else:
                    failed_count += 1

                if idx < len(news_items):
                    time.sleep(1)

            except Exception as e:
                failed_count += 1
                logger.error(f"❌ 처리 실패: {e}")
                continue

        logger.info(f"🎉 {company_name} 뉴스 {len(results)}개 수집 완료")

        return jsonify({
            'success': True,
            'symbol': symbol,
            'company_name': company_name,
            'count': len(results),
            'failed': failed_count,
            'news': results
        })

    except Exception as e:
        logger.error(f"❌ 관심종목 뉴스 수집 실패: {e}")
        return jsonify({'error': f'Failed to collect watchlist news: {str(e)}'}), 500

    finally:
        try:
            news_processor.cleanup_local_resources()
        except Exception as e:
            logger.error(f"❌ 리소스 정리 오류: {e}")


@app.route('/collect/historical', methods=['POST'])
@monitor_performance
def collect_historical_news():
    """역사챌린지 뉴스 수집 API"""
    try:
        data = request.get_json()
        if not data:
            return jsonify({'error': 'JSON data required'}), 400

        symbol = data.get('symbol')
        company_name = data.get('company_name')
        date = data.get('date')
        aliases = data.get('aliases', [])
        limit = min(data.get('limit', 10), 10)

        if not all([symbol, company_name, date]):
            return jsonify({'error': 'symbol, company_name, and date required'}), 400

        logger.info(f"🚀 역사챌린지 뉴스 수집: {company_name} ({date})")

        crawler = news_processor.get_crawler()
        content_parser = news_processor.get_content_parser()

        # 특정 날짜 뉴스 수집
        news_items = crawler.get_historical_news(symbol, company_name, date, aliases, limit)

        if not news_items:
            logger.warning(f"⚠️ 뉴스가 없습니다: {company_name} ({date})")
            return jsonify({
                'success': True,
                'symbol': symbol,
                'company_name': company_name,
                'target_date': date,
                'count': 0,
                'news': []
            })

        # 순차 처리
        results = []
        failed_count = 0

        for idx, news_item in enumerate(news_items, 1):
            try:
                # 종목 정보 설정
                news_item.symbol = symbol

                result = news_processor.process_single_news_strict(
                    news_item, content_parser, idx, len(news_items), "HISTORICAL"
                )

                if result:
                    result['target_date'] = date
                    result['symbol'] = symbol
                    result['company_name'] = company_name
                    results.append(result)
                else:
                    failed_count += 1

                if idx < len(news_items):
                    time.sleep(1)

            except Exception as e:
                failed_count += 1
                logger.error(f"❌ 처리 실패: {e}")
                continue

        logger.info(f"🎉 역사챌린지 뉴스 {len(results)}개 수집 완료")

        return jsonify({
            'success': True,
            'symbol': symbol,
            'company_name': company_name,
            'target_date': date,
            'count': len(results),
            'failed': failed_count,
            'news': results
        })

    except Exception as e:
        logger.error(f"❌ 역사챌린지 뉴스 수집 실패: {e}")
        return jsonify({'error': f'Failed to collect historical news: {str(e)}'}), 500

    finally:
        try:
            news_processor.cleanup_local_resources()
        except Exception as e:
            logger.error(f"❌ 리소스 정리 오류: {e}")


@app.route('/stats', methods=['GET'])
def get_collection_stats():
    """수집 통계 조회"""
    return jsonify({
        'stats': news_processor.collection_stats,
        'cache_stats': cache_manager.get_stats(),  # 캐시 통계 추가
        'timestamp': datetime.now().isoformat()
    })

@app.route('/cache/stats', methods=['GET'])
def get_cache_stats():
    """캐시 통계 조회 API"""
    return jsonify(cache_manager.get_stats())

@app.route('/cache/clear', methods=['POST'])
def clear_cache():
    """캐시 삭제 API"""
    pattern = request.args.get('pattern', None)
    cache_manager.clear_cache(pattern)
    return jsonify({'message': 'Cache cleared', 'pattern': pattern})


# 기존 분석 API들 유지
@app.route('/analyze', methods=['POST'])
@monitor_performance
def analyze_sentiment_endpoint():
    """뉴스 URL 기반 감정분석 API"""
    try:
        if not is_model_loaded():
            return jsonify({"error": "Model not loaded"}), 500

        if not request.is_json:
            return jsonify({"error": "Content-Type must be application/json"}), 400

        data = request.get_json()
        url = data.get('url', '').strip()

        if not url:
            return jsonify({"error": "url field is required"}), 400

        if not url.startswith(('http://', 'https://')):
            return jsonify({"error": "Invalid URL format"}), 400

        logger.info(f"뉴스 분석 요청: {url}")

        extracted_data = extract_news_content_with_title(url)
        combined_text = extracted_data['combined_text']

        if len(combined_text) > TEXT_MAX_LENGTH:
            combined_text = combined_text[:TEXT_MAX_LENGTH]

        analysis_result = analyze_sentiment(combined_text)

        result = {
            "url": url,
            "title": extracted_data['title'],
            "content": extracted_data['content'],
            "images": extracted_data['images'],
            **analysis_result
        }

        return jsonify(result)

    except Exception as e:
        logger.error(f"감정분석 오류: {e}")
        return jsonify({"error": f"Internal server error: {str(e)}"}), 500


@app.route('/analyze-text', methods=['POST'])
@monitor_performance
def analyze_text_sentiment_endpoint():
    """텍스트 직접 입력 기반 감정분석 API"""
    try:
        if not is_model_loaded():
            return jsonify({"error": "Model not loaded"}), 500

        if not request.is_json:
            return jsonify({"error": "Content-Type must be application/json"}), 400

        data = request.get_json()
        text = data.get('text', '').strip()

        if not text:
            return jsonify({"error": "text field is required"}), 400

        logger.info(f"텍스트 분석 요청: {len(text)}자")

        if len(text) > TEXT_MAX_LENGTH:
            text = text[:TEXT_MAX_LENGTH]

        analysis_result = analyze_sentiment(text)

        return jsonify(analysis_result)

    except Exception as e:
        logger.error(f"텍스트 감정분석 오류: {e}")
        return jsonify({"error": f"Internal server error: {str(e)}"}), 500


if __name__ == '__main__':
    logger.info("Flask 서버 시작 중...")

    if not load_model():
        logger.error("모델 로딩 실패로 인해 서버를 시작할 수 없습니다")
        exit(1)

    logger.info("서버 시작 준비 완료!")

    app.run(host=HOST, port=PORT, debug=DEBUG, threaded=True)