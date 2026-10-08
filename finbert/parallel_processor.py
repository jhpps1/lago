"""
병렬 처리 뉴스 프로세서
최대 3-5배 성능 향상을 위한 병렬 처리 구현
"""
import concurrent.futures
import threading
import time
import logging
from typing import List, Dict, Optional, Callable
from dataclasses import dataclass
from queue import Queue, Empty
import multiprocessing

logger = logging.getLogger(__name__)

@dataclass
class ProcessingTask:
    """처리 작업 단위"""
    task_id: int
    task_type: str  # 'url_redirect', 'content_extract', 'sentiment', 'summary'
    data: Dict
    retry_count: int = 0
    max_retries: int = 2


class ParallelNewsProcessor:
    """병렬 뉴스 처리 엔진"""
    
    def __init__(self, max_workers: int = None):
        """
        병렬 처리 초기화
        max_workers: 최대 워커 수 (None이면 CPU 코어 수 기반 자동 설정)
        """
        self.max_workers = max_workers or min(multiprocessing.cpu_count(), 4)
        self.executor = concurrent.futures.ThreadPoolExecutor(max_workers=self.max_workers)
        self.processing_stats = {
            'total': 0,
            'success': 0,
            'failed': 0,
            'cached': 0,
            'processing_time': 0
        }
        logger.info(f"병렬 처리 엔진 초기화 - 워커 수: {self.max_workers}")
    
    def process_news_batch(self, news_items: List, 
                          url_processor: Callable,
                          content_processor: Callable,
                          sentiment_processor: Callable,
                          summary_processor: Callable) -> List[Optional[Dict]]:
        """
        뉴스 배치 병렬 처리
        """
        start_time = time.time()
        total_items = len(news_items)
        logger.info(f"📊 병렬 처리 시작 - {total_items}개 뉴스, {self.max_workers}개 워커")
        
        # 결과 저장용 딕셔너리
        results = {}
        futures_map = {}
        
        # Phase 1: URL 리디렉션 병렬 처리
        logger.info("🔄 Phase 1: URL 리디렉션 병렬 처리")
        url_futures = []
        for idx, news_item in enumerate(news_items):
            future = self.executor.submit(
                self._process_with_retry,
                url_processor,
                news_item.url,
                idx + 1,
                total_items
            )
            url_futures.append((idx, future))
            futures_map[future] = ('url', idx)
        
        # URL 처리 결과 수집
        url_results = {}
        for idx, future in url_futures:
            try:
                result = future.result(timeout=15)
                if result:
                    url_results[idx] = result
                    logger.debug(f"✅ URL [{idx+1}/{total_items}] 처리 완료")
                else:
                    logger.warning(f"❌ URL [{idx+1}/{total_items}] 처리 실패")
            except Exception as e:
                logger.error(f"❌ URL [{idx+1}/{total_items}] 예외: {e}")
        
        # Phase 2: 콘텐츠 추출 병렬 처리 (성공한 URL만)
        logger.info("📄 Phase 2: 콘텐츠 추출 병렬 처리")
        content_futures = []
        for idx, final_url in url_results.items():
            future = self.executor.submit(
                self._process_with_retry,
                content_processor,
                final_url,
                idx + 1,
                total_items
            )
            content_futures.append((idx, future))
            futures_map[future] = ('content', idx)
        
        # 콘텐츠 처리 결과 수집
        content_results = {}
        for idx, future in content_futures:
            try:
                result = future.result(timeout=30)
                if result:
                    content_results[idx] = result
                    logger.debug(f"✅ 콘텐츠 [{idx+1}/{total_items}] 추출 완료")
                else:
                    logger.warning(f"❌ 콘텐츠 [{idx+1}/{total_items}] 추출 실패")
            except Exception as e:
                logger.error(f"❌ 콘텐츠 [{idx+1}/{total_items}] 예외: {e}")
        
        # Phase 3: 감성분석 & 요약 동시 병렬 처리
        logger.info("🧠 Phase 3: 감성분석 & 요약 동시 병렬 처리")
        
        analysis_futures = []
        for idx, content_data in content_results.items():
            # 감성분석 작업
            analysis_text = content_data.get('content_text') or content_data.get('summary_text')
            if analysis_text:
                sentiment_future = self.executor.submit(
                    sentiment_processor,
                    analysis_text,
                    idx + 1,
                    total_items
                )
                analysis_futures.append((idx, 'sentiment', sentiment_future))
            
            # 요약 작업 (Claude API는 Rate Limit 있으므로 순차적으로)
            # 여기서는 병렬 처리하지 않고 나중에 순차 처리
        
        # 감성분석 결과 수집
        sentiment_results = {}
        for idx, task_type, future in analysis_futures:
            if task_type == 'sentiment':
                try:
                    result = future.result(timeout=10)
                    sentiment_results[idx] = result
                    logger.debug(f"✅ 감성분석 [{idx+1}/{total_items}] 완료")
                except Exception as e:
                    logger.error(f"❌ 감성분석 [{idx+1}/{total_items}] 예외: {e}")
                    sentiment_results[idx] = self._get_default_sentiment()
        
        # Phase 4: Claude 요약은 Rate Limit 때문에 순차 처리 (하지만 캐싱으로 빠르게)
        logger.info("📝 Phase 4: Claude 요약 순차 처리 (캐싱 활용)")
        summary_results = {}
        for idx, content_data in content_results.items():
            try:
                summary = summary_processor(
                    content_data.get('title', ''),
                    content_data.get('content_text', ''),
                    idx + 1,
                    total_items
                )
                summary_results[idx] = summary
                logger.debug(f"✅ 요약 [{idx+1}/{total_items}] 완료")
            except Exception as e:
                logger.error(f"❌ 요약 [{idx+1}/{total_items}] 예외: {e}")
                summary_results[idx] = []
        
        # 최종 결과 조합
        final_results = []
        for idx, news_item in enumerate(news_items):
            if idx not in content_results:
                final_results.append(None)
                continue
            
            result = {
                'title': content_results[idx].get('title'),
                'url': url_results.get(idx),
                'original_url': news_item.url,
                'source': news_item.source,
                'published_at': news_item.published_at.isoformat(),
                'category': news_item.category,
                'symbol': news_item.symbol,
                'collection_type': 'REALTIME',
                
                'content_text': content_results[idx].get('content_text', '')[:5000],
                'summary_text': content_results[idx].get('summary_text'),
                'summary_lines': summary_results.get(idx, []),
                
                'images': content_results[idx].get('images', [])[:10],
                'main_image_url': content_results[idx].get('main_image'),
                'total_images': len(content_results[idx].get('images', [])),
                
                'content_blocks': content_results[idx].get('blocks', [])[:20],
                
                **sentiment_results.get(idx, self._get_default_sentiment()),
                
                'parser_info': {
                    'parser_used': content_results[idx].get('parser_used'),
                    'confidence': content_results[idx].get('confidence'),
                    'processing_time': round(time.time() - start_time, 2)
                },
                'processing_status': 'success'
            }
            final_results.append(result)
        
        # 통계 업데이트
        processing_time = time.time() - start_time
        self.processing_stats['total'] += total_items
        self.processing_stats['success'] += len([r for r in final_results if r])
        self.processing_stats['failed'] += len([r for r in final_results if not r])
        self.processing_stats['processing_time'] += processing_time
        
        logger.info(f"""
        🎯 병렬 처리 완료 통계:
        - 전체: {total_items}개
        - 성공: {len([r for r in final_results if r])}개
        - 실패: {len([r for r in final_results if not r])}개
        - 처리 시간: {processing_time:.2f}초
        - 평균 처리 시간: {processing_time/total_items:.2f}초/개
        - 성능 향상: {total_items * 60 / processing_time:.1f}x (순차 대비 추정)
        """)
        
        return final_results
    
    def _process_with_retry(self, func: Callable, *args, **kwargs):
        """재시도 로직이 포함된 처리"""
        max_retries = 2
        for attempt in range(max_retries):
            try:
                return func(*args, **kwargs)
            except Exception as e:
                if attempt < max_retries - 1:
                    logger.warning(f"처리 실패 (시도 {attempt+1}/{max_retries}): {e}")
                    time.sleep(1)  # 재시도 전 대기
                else:
                    logger.error(f"최종 처리 실패: {e}")
                    raise
        return None
    
    def _get_default_sentiment(self) -> Dict:
        """기본 감성분석 결과"""
        return {
            'label': 'NEUTRAL',
            'score': 0.0,
            'confidence': 0.0,
            'confidence_level': 'LOW',
            'trading_signal': 'HOLD',
            'signal_strength': 'WEAK',
            'error': 'Processing failed'
        }
    
    def get_stats(self) -> Dict:
        """처리 통계 반환"""
        if self.processing_stats['total'] > 0:
            avg_time = self.processing_stats['processing_time'] / self.processing_stats['total']
        else:
            avg_time = 0
        
        return {
            **self.processing_stats,
            'avg_processing_time': round(avg_time, 2),
            'max_workers': self.max_workers
        }
    
    def shutdown(self):
        """병렬 처리 엔진 종료"""
        self.executor.shutdown(wait=True)
        logger.info("병렬 처리 엔진 종료 완료")