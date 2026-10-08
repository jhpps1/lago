"""
간단한 메모리 기반 캐시 매니저 (Redis 없이 작동)
"""
import hashlib
import json
import time
from typing import Optional, Dict, Any
import logging

logger = logging.getLogger(__name__)

class SimpleCache:
    """메모리 기반 간단한 캐시"""
    
    def __init__(self):
        self.cache = {}
        self.enabled = True
        logger.info("메모리 기반 캐시 활성화")
    
    def _get_hash(self, text: str) -> str:
        """텍스트를 MD5 해시로 변환"""
        return hashlib.md5(text.encode('utf-8')).hexdigest()
    
    def _is_expired(self, cached_item: Dict) -> bool:
        """캐시 만료 확인"""
        return time.time() > cached_item['expires_at']
    
    def get_sentiment_cache(self, text: str) -> Optional[Dict]:
        """감성분석 캐시 조회"""
        if not self.enabled:
            return None
            
        key = f"sentiment:{self._get_hash(text)}"
        if key in self.cache:
            cached = self.cache[key]
            if not self._is_expired(cached):
                logger.debug(f"캐시 히트: sentiment")
                return cached['data']
            else:
                del self.cache[key]
        return None
    
    def set_sentiment_cache(self, text: str, result: Dict, ttl: int = 86400):
        """감성분석 결과 캐싱"""
        if not self.enabled:
            return
            
        key = f"sentiment:{self._get_hash(text)}"
        self.cache[key] = {
            'data': result,
            'expires_at': time.time() + ttl
        }
        logger.debug(f"캐시 저장: sentiment (TTL: {ttl}초)")
    
    def get_summary_cache(self, title: str, content: str) -> Optional[list]:
        """요약 캐시 조회"""
        if not self.enabled:
            return None
            
        combined = f"{title}:{content[:500]}"
        key = f"summary:{self._get_hash(combined)}"
        if key in self.cache:
            cached = self.cache[key]
            if not self._is_expired(cached):
                logger.debug(f"캐시 히트: summary")
                return cached['data']
            else:
                del self.cache[key]
        return None
    
    def set_summary_cache(self, title: str, content: str, summary: list, ttl: int = 604800):
        """요약 결과 캐싱"""
        if not self.enabled:
            return
            
        combined = f"{title}:{content[:500]}"
        key = f"summary:{self._get_hash(combined)}"
        self.cache[key] = {
            'data': summary,
            'expires_at': time.time() + ttl
        }
        logger.debug(f"캐시 저장: summary (TTL: {ttl}초)")
    
    def get_url_redirect_cache(self, google_url: str) -> Optional[str]:
        """URL 리디렉션 캐시 조회"""
        if not self.enabled:
            return None
            
        key = f"url:{self._get_hash(google_url)}"
        if key in self.cache:
            cached = self.cache[key]
            if not self._is_expired(cached):
                logger.debug(f"캐시 히트: URL redirect")
                return cached['data']
            else:
                del self.cache[key]
        return None
    
    def set_url_redirect_cache(self, google_url: str, real_url: str, ttl: int = 3600):
        """URL 리디렉션 결과 캐싱"""
        if not self.enabled:
            return
            
        key = f"url:{self._get_hash(google_url)}"
        self.cache[key] = {
            'data': real_url,
            'expires_at': time.time() + ttl
        }
        logger.debug(f"캐시 저장: URL redirect (TTL: {ttl}초)")
    
    def get_stats(self) -> Dict[str, Any]:
        """캐시 통계"""
        if not self.enabled:
            return {"enabled": False}
        
        # 만료된 항목 정리
        current_time = time.time()
        expired_keys = [k for k, v in self.cache.items() if v['expires_at'] < current_time]
        for key in expired_keys:
            del self.cache[key]
        
        # 통계 계산
        sentiment_keys = len([k for k in self.cache.keys() if k.startswith("sentiment:")])
        summary_keys = len([k for k in self.cache.keys() if k.startswith("summary:")])
        url_keys = len([k for k in self.cache.keys() if k.startswith("url:")])
        
        return {
            "enabled": True,
            "type": "memory",
            "total_keys": len(self.cache),
            "sentiment_keys": sentiment_keys,
            "summary_keys": summary_keys,
            "url_keys": url_keys,
            "expired_cleaned": len(expired_keys)
        }
    
    def clear_cache(self, pattern: str = None):
        """캐시 삭제"""
        if not self.enabled:
            return
            
        if pattern:
            keys_to_delete = [k for k in self.cache.keys() if pattern in k]
            for key in keys_to_delete:
                del self.cache[key]
            logger.info(f"{len(keys_to_delete)}개 캐시 키 삭제: {pattern}")
        else:
            self.cache.clear()
            logger.info("전체 캐시 삭제 완료")

# 싱글톤 인스턴스
cache_manager = SimpleCache()