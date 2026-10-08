"""
Redis 캐싱 관리 모듈
- FinBERT 감성분석 결과 캐싱
- Claude 요약 결과 캐싱
- URL 리디렉션 결과 캐싱
"""
import redis
import json
import hashlib
import logging
from typing import Optional, Dict, Any
from datetime import timedelta
import time

logger = logging.getLogger(__name__)

class CacheManager:
    """Redis 기반 캐시 매니저"""
    
    def __init__(self, host='redis', port=6379, db=0):
        """Redis 연결 초기화"""
        try:
            self.redis_client = redis.Redis(
                host=host,
                port=port,
                db=db,
                decode_responses=True,
                socket_connect_timeout=5,
                socket_timeout=5
            )
            # 연결 테스트
            self.redis_client.ping()
            logger.info(f"Redis 캐시 연결 성공: {host}:{port}")
            self.enabled = True
        except Exception as e:
            logger.warning(f"Redis 연결 실패, 캐싱 비활성화: {e}")
            self.redis_client = None
            self.enabled = False
    
    def _get_hash(self, text: str) -> str:
        """텍스트를 MD5 해시로 변환"""
        return hashlib.md5(text.encode('utf-8')).hexdigest()
    
    def get_sentiment_cache(self, text: str) -> Optional[Dict]:
        """감성분석 캐시 조회"""
        if not self.enabled:
            return None
            
        try:
            key = f"sentiment:{self._get_hash(text)}"
            cached = self.redis_client.get(key)
            if cached:
                logger.debug(f"캐시 히트: sentiment")
                return json.loads(cached)
        except Exception as e:
            logger.error(f"캐시 조회 실패: {e}")
        return None
    
    def set_sentiment_cache(self, text: str, result: Dict, ttl: int = 86400):
        """감성분석 결과 캐싱 (기본 24시간)"""
        if not self.enabled:
            return
            
        try:
            key = f"sentiment:{self._get_hash(text)}"
            self.redis_client.setex(
                key,
                ttl,
                json.dumps(result, ensure_ascii=False)
            )
            logger.debug(f"캐시 저장: sentiment (TTL: {ttl}초)")
        except Exception as e:
            logger.error(f"캐시 저장 실패: {e}")
    
    def get_summary_cache(self, title: str, content: str) -> Optional[list]:
        """요약 캐시 조회"""
        if not self.enabled:
            return None
            
        try:
            combined = f"{title}:{content[:500]}"  # 제목과 내용 일부 조합
            key = f"summary:{self._get_hash(combined)}"
            cached = self.redis_client.get(key)
            if cached:
                logger.debug(f"캐시 히트: summary")
                return json.loads(cached)
        except Exception as e:
            logger.error(f"캐시 조회 실패: {e}")
        return None
    
    def set_summary_cache(self, title: str, content: str, summary: list, ttl: int = 604800):
        """요약 결과 캐싱 (기본 7일)"""
        if not self.enabled:
            return
            
        try:
            combined = f"{title}:{content[:500]}"
            key = f"summary:{self._get_hash(combined)}"
            self.redis_client.setex(
                key,
                ttl,
                json.dumps(summary, ensure_ascii=False)
            )
            logger.debug(f"캐시 저장: summary (TTL: {ttl}초)")
        except Exception as e:
            logger.error(f"캐시 저장 실패: {e}")
    
    def get_url_redirect_cache(self, google_url: str) -> Optional[str]:
        """URL 리디렉션 캐시 조회"""
        if not self.enabled:
            return None
            
        try:
            key = f"url:{self._get_hash(google_url)}"
            cached = self.redis_client.get(key)
            if cached:
                logger.debug(f"캐시 히트: URL redirect")
                return cached
        except Exception as e:
            logger.error(f"캐시 조회 실패: {e}")
        return None
    
    def set_url_redirect_cache(self, google_url: str, real_url: str, ttl: int = 3600):
        """URL 리디렉션 결과 캐싱 (기본 1시간)"""
        if not self.enabled:
            return
            
        try:
            key = f"url:{self._get_hash(google_url)}"
            self.redis_client.setex(key, ttl, real_url)
            logger.debug(f"캐시 저장: URL redirect (TTL: {ttl}초)")
        except Exception as e:
            logger.error(f"캐시 저장 실패: {e}")
    
    def acquire_lock(self, key: str, timeout: int = 60) -> bool:
        """분산 락 획득 (캐시 스탬피드 방지)"""
        if not self.enabled:
            return True  # 캐시 비활성화시 항상 처리
            
        try:
            lock_key = f"lock:{key}"
            return self.redis_client.set(lock_key, "1", nx=True, ex=timeout)
        except Exception as e:
            logger.error(f"락 획득 실패: {e}")
            return True  # 실패시 처리 진행
    
    def release_lock(self, key: str):
        """분산 락 해제"""
        if not self.enabled:
            return
            
        try:
            lock_key = f"lock:{key}"
            self.redis_client.delete(lock_key)
        except Exception as e:
            logger.error(f"락 해제 실패: {e}")
    
    def get_with_lock(self, cache_key: str, compute_func, ttl: int = 3600):
        """락을 사용한 안전한 캐시 조회/계산"""
        if not self.enabled:
            return compute_func()
        
        # 1. 캐시 확인
        cached = self.redis_client.get(cache_key)
        if cached:
            return json.loads(cached)
        
        # 2. 락 시도
        if self.acquire_lock(cache_key):
            try:
                # 다시 한번 캐시 확인 (double-check)
                cached = self.redis_client.get(cache_key)
                if cached:
                    return json.loads(cached)
                
                # 실제 계산
                result = compute_func()
                
                # 캐시 저장
                self.redis_client.setex(
                    cache_key,
                    ttl,
                    json.dumps(result, ensure_ascii=False)
                )
                return result
            finally:
                self.release_lock(cache_key)
        else:
            # 락 획득 실패 - 다른 프로세스가 처리중
            # 잠시 대기 후 캐시 확인
            max_wait = 30
            for i in range(max_wait):
                time.sleep(1)
                cached = self.redis_client.get(cache_key)
                if cached:
                    return json.loads(cached)
            
            # 타임아웃 - 직접 계산
            logger.warning(f"락 대기 타임아웃, 직접 계산: {cache_key}")
            return compute_func()
    
    def get_stats(self) -> Dict[str, Any]:
        """캐시 통계 조회"""
        if not self.enabled:
            return {"enabled": False}
            
        try:
            info = self.redis_client.info("stats")
            memory = self.redis_client.info("memory")
            
            # 캐시 키 개수
            sentiment_keys = len(self.redis_client.keys("sentiment:*"))
            summary_keys = len(self.redis_client.keys("summary:*"))
            url_keys = len(self.redis_client.keys("url:*"))
            
            return {
                "enabled": True,
                "total_keys": self.redis_client.dbsize(),
                "sentiment_keys": sentiment_keys,
                "summary_keys": summary_keys,
                "url_keys": url_keys,
                "hits": info.get("keyspace_hits", 0),
                "misses": info.get("keyspace_misses", 0),
                "hit_rate": round(info.get("keyspace_hits", 0) / 
                                 max(1, info.get("keyspace_hits", 0) + info.get("keyspace_misses", 0)) * 100, 2),
                "memory_used": memory.get("used_memory_human", "0"),
                "memory_peak": memory.get("used_memory_peak_human", "0")
            }
        except Exception as e:
            logger.error(f"통계 조회 실패: {e}")
            return {"enabled": True, "error": str(e)}
    
    def clear_cache(self, pattern: str = None):
        """캐시 삭제"""
        if not self.enabled:
            return
            
        try:
            if pattern:
                keys = self.redis_client.keys(pattern)
                if keys:
                    self.redis_client.delete(*keys)
                    logger.info(f"{len(keys)}개 캐시 키 삭제: {pattern}")
            else:
                self.redis_client.flushdb()
                logger.info("전체 캐시 삭제 완료")
        except Exception as e:
            logger.error(f"캐시 삭제 실패: {e}")


# 싱글톤 인스턴스
cache_manager = CacheManager()