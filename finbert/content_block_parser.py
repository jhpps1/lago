
# -*- coding: utf-8 -*-
"""
content_block_parser.py
-----------------------
Robust content parser (no newspaper3k). 
- Requests + BeautifulSoup primary parsing
- Optional JS rendering retry via Selenium (toggle in config)
- Final fallback: news_crawler.extract_news_content_with_title
Produces: ParsedContent with summary_text and ordered ContentBlocks.
"""

from __future__ import annotations

import logging
import re
import time
from dataclasses import dataclass, field
from typing import List, Optional, Tuple
from urllib.parse import urljoin

import requests
from bs4 import BeautifulSoup

# ---- Config (safe import + defaults) -------------------------------------
try:
    import config as _cfg
    MIN_ARTICLE_LEN: int = getattr(_cfg, "MIN_ARTICLE_LEN", 200)
    RENDER_JS: bool = getattr(_cfg, "RENDER_JS", False)
    JS_RENDER_TIMEOUT: int = getattr(_cfg, "JS_RENDER_TIMEOUT", 10)
    TEXT_MAX_LENGTH: int = getattr(_cfg, "TEXT_MAX_LENGTH", 4000)
    USER_AGENT: str = getattr(_cfg, "USER_AGENT", "Mozilla/5.0")
    REQUEST_TIMEOUT: int = getattr(_cfg, "REQUEST_TIMEOUT", 10)
except Exception:
    MIN_ARTICLE_LEN, RENDER_JS, JS_RENDER_TIMEOUT = 200, False, 10
    TEXT_MAX_LENGTH, USER_AGENT, REQUEST_TIMEOUT = 4000, "Mozilla/5.0", 10

# news_crawler fallback (optional)
try:
    from news_crawler import extract_news_content_with_title  # type: ignore
    HAS_NEWS_CRAWLER = True
except Exception:
    HAS_NEWS_CRAWLER = False

log = logging.getLogger(__name__)

# ---- Data models ---------------------------------------------------------
@dataclass
class ContentBlock:
    type: str  # "text" | "image"
    content: str
    position: int
    confidence: float = 0.8  # 콘텐츠 블록 신뢰도

@dataclass
class ParsedContent:
    title: str = ""  # 뉴스 제목
    summary_text: str = ""
    blocks: List[ContentBlock] = field(default_factory=list)
    total_images: int = 0
    parser_used: str = "content_block_parser"
    confidence: float = 0.6

# ---- Parser --------------------------------------------------------------
class ContentBlockParser:
    def __init__(self) -> None:
        self.session = requests.Session()
        self.session.headers.update({
            "User-Agent": USER_AGENT,
            "Accept-Language": "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7",
        })
        self._driver = None  # lazy Selenium driver

        # common selectors (order matters)
        self._CONTENT_SELECTORS: List[str] = [
            "article",
            "#newsct_article",           # Naver News
            ".newsct_article",
            ".article-content",
            ".article_body",
            ".article-body",
            ".entry-content",
            ".post-content",
            "#articeBody",
            "#articleBody",
            ".news_body_area",
            ".container--article",
        ]

    # ---------------------- public API -----------------------------------
    def parse_url(self, url: str) -> ParsedContent:
        result = ParsedContent()

        # 1) primary fetch & parse
        html = self._fetch_html(url)
        text, images, blocks, title = self._extract_blocks(html, base_url=url)

        # 2) if too short, try JS rendered HTML (when enabled)
        if len(text) < MIN_ARTICLE_LEN:
            rendered = self._render_html_if_enabled(url)
            if rendered:
                text2, images2, blocks2, title2 = self._extract_blocks(rendered, base_url=url)
                if len(text2) > len(text):
                    text, images, blocks, title = text2, (images or images2), blocks2, title2

        # 3) still short? try news_crawler fallback (title+content)
        if len(text) < MIN_ARTICLE_LEN and HAS_NEWS_CRAWLER:
            try:
                title2, content2 = extract_news_content_with_title(url)  # type: ignore
                if content2 and len(content2) > len(text):
                    merged = (text + ("\n" if text else "") + content2).strip()
                    text = merged
                    # if blocks empty, at least put one text block
                    if not blocks:
                        blocks.append(ContentBlock(type="text", content=content2, position=0))
            except Exception as e:
                log.debug("news_crawler fallback failed: %s", e)

        # 4) finalize result
        if not blocks and text:
            blocks.append(ContentBlock(type="text", content=text, position=0))

        # cap summary length
        summary_cap = max(MIN_ARTICLE_LEN * 5, TEXT_MAX_LENGTH)
        result.title = title or ""
        result.summary_text = (text or "")[:summary_cap]
        result.blocks = blocks
        result.total_images = sum(1 for b in blocks if b.type == "image")
        result.parser_used = "content_block_parser" + ("+js" if self._driver else "")
        result.confidence = 0.9 if len(result.summary_text) >= MIN_ARTICLE_LEN else 0.6

        # clean driver if used headlessly
        self._cleanup_driver_if_needed()

        return result

    # ---------------------- internals ------------------------------------
    def _fetch_html(self, url: str) -> str:
        try:
            r = self.session.get(url, timeout=REQUEST_TIMEOUT, allow_redirects=True)
            r.raise_for_status()
            return r.text
        except Exception as e:
            log.debug("HTTP GET failed: %s", e)
            return ""

    def _extract_blocks(self, html: str, base_url: str) -> Tuple[str, List[str], List[ContentBlock], str]:
        if not html:
            return "", [], [], ""

        soup = BeautifulSoup(html, "html.parser")
        
        # Extract title from HTML
        title = ""
        title_tag = soup.find("title")
        if title_tag:
            title = title_tag.get_text(strip=True)
        # Try to get article title from meta or h1
        if not title:
            meta_title = soup.find("meta", property="og:title") or soup.find("meta", attrs={"name": "title"})
            if meta_title:
                title = meta_title.get("content", "")
        if not title:
            h1_tag = soup.find("h1")
            if h1_tag:
                title = h1_tag.get_text(strip=True)

        # Prefer domain-specific or common article containers
        node = None
        for sel in self._CONTENT_SELECTORS:
            node = soup.select_one(sel)
            if node:
                break

        # If no main node, use relaxed paragraph collection
        if not node:
            paragraphs = [p.get_text(" ", strip=True) for p in soup.find_all("p")]
            text = " ".join([t for t in paragraphs if t])[:100000]
            images: List[str] = []
            blocks: List[ContentBlock] = []
            if text:
                blocks.append(ContentBlock(type="text", content=text, position=0))
            return text, images, blocks, title

        # Remove non-content tags
        for t in node.find_all(["script", "style", "nav", "footer", "aside", "noscript", "form"]):
            t.decompose()

        blocks: List[ContentBlock] = []
        images: List[str] = []

        # Walk immediate children to preserve rough order
        pos = 0
        def add_text(txt: str):
            nonlocal pos
            clean = re.sub(r"\s+", " ", txt or "").strip()
            if not clean:
                return
            blocks.append(ContentBlock(type="text", content=clean, position=pos))
            pos += 1

        def add_img(src: str):
            nonlocal pos
            if not src:
                return
            # absolutize & filter
            full = urljoin(base_url, src)
            if full.startswith("http"):
                images.append(full)
                blocks.append(ContentBlock(type="image", content=full, position=pos))
                pos += 1

        for child in list(node.children):
            # text nodes
            if getattr(child, "name", None) is None:
                add_text(str(child))
                continue

            # paragraphs
            if child.name in ("p", "h1", "h2", "h3", "h4"):
                add_text(child.get_text(" ", strip=True))
                # inline images inside p
                for im in child.find_all("img"):
                    add_img(im.get("src") or im.get("data-src") or "")

            # figures / images
            elif child.name in ("figure", "picture"):
                im = child.find("img")
                if im:
                    add_img(im.get("src") or im.get("data-src") or "")

            # generic blocks that may contain text/images
            elif child.name in ("div", "section"):
                # text
                txt = child.get_text(" ", strip=True)
                # but avoid re-adding giant text if it already captured by parent; keep it modest
                if txt and len(txt) > 20:
                    add_text(txt)
                # images
                for im in child.find_all("img"):
                    add_img(im.get("src") or im.get("data-src") or "")

        # If no text captured from children, fallback to the node text
        if not any(b.type == "text" for b in blocks):
            add_text(node.get_text(" ", strip=True))

        return (
            " ".join([b.content for b in blocks if b.type == "text"]).strip(),
            images,
            blocks,
            title,
        )

    # ---------- Selenium (optional) ----------
    def _ensure_driver(self) -> bool:
        if self._driver is not None:
            return True

        if not RENDER_JS:
            return False

        try:
            from selenium import webdriver  # type: ignore
            from selenium.webdriver.chrome.options import Options  # type: ignore
            from selenium.webdriver.chrome.service import Service  # type: ignore
            from webdriver_manager.chrome import ChromeDriverManager  # type: ignore

            opts = Options()
            opts.add_argument("--headless=new")
            opts.add_argument("--no-sandbox")
            opts.add_argument("--disable-dev-shm-usage")
            self._driver = webdriver.Chrome(service=Service(ChromeDriverManager().install()), options=opts)
            self._driver.set_page_load_timeout(JS_RENDER_TIMEOUT + 5)
            return True
        except Exception as e:
            log.debug("Selenium not available: %s", e)
            self._driver = None
            return False

    def _render_html_if_enabled(self, url: str) -> Optional[str]:
        if not self._ensure_driver():
            return None
        try:
            from selenium.webdriver.support.ui import WebDriverWait  # type: ignore
            self._driver.get(url)
            WebDriverWait(self._driver, JS_RENDER_TIMEOUT).until(
                lambda d: d.execute_script("return document.readyState") == "complete"
            )
            time.sleep(1)
            return self._driver.page_source
        except Exception as e:
            log.debug("JS render failed: %s", e)
            return None

    def _cleanup_driver_if_needed(self) -> None:
        # Keep driver alive across calls to amortize cost. If you prefer one-shot, uncomment below.
        # if self._driver:
        #     try: self._driver.quit()
        #     except Exception: pass
        #     self._driver = None
        return
