package com.example.LAGO.repository;

import com.example.LAGO.domain.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    // 일반 투자 뉴스 (종목 특정되지 않은 일반 뉴스) - title 기반 검색으로 변경
    @Query("SELECT n FROM News n WHERE " +
           "(LOWER(n.title) LIKE LOWER(:keyword1) OR LOWER(n.title) LIKE LOWER(:keyword2) OR " +
           " LOWER(n.title) LIKE LOWER(:keyword3) OR LOWER(n.title) LIKE LOWER(:keyword4) OR " +
           " LOWER(n.title) LIKE LOWER(:keyword5) OR LOWER(n.title) LIKE LOWER(:keyword6)) " +
           "ORDER BY n.publishedAt DESC")
    Page<News> findGeneralInvestmentNews(
        @Param("keyword1") String keyword1,
        @Param("keyword2") String keyword2, 
        @Param("keyword3") String keyword3,
        @Param("keyword4") String keyword4,
        @Param("keyword5") String keyword5,
        @Param("keyword6") String keyword6,
        Pageable pageable
    );
    
    // 관심종목별 뉴스 (새 스키마에는 stock_code 없음 - 제거 필요)
    // 특정 종목 뉴스 (새 스키마에는 stock_code 없음 - 제거 필요)
    
    // 최신 뉴스 (전체)
    Page<News> findAllByOrderByPublishedAtDesc(Pageable pageable);
    
    // 특정 기간 뉴스
    @Query("SELECT n FROM News n WHERE n.publishedAt BETWEEN :startDate AND :endDate " +
           "ORDER BY n.publishedAt DESC")
    List<News> findByPublishedAtBetween(
        @Param("startDate") LocalDateTime startDate, 
        @Param("endDate") LocalDateTime endDate
    );
    
    // 키워드 검색 - title과 content 기반 (keywords 필드 제거)
    @Query("SELECT n FROM News n WHERE " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           " LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY n.publishedAt DESC")
    Page<News> findByKeywordSearch(@Param("keyword") String keyword, Pageable pageable);
    
    // 삼성전자 뉴스 필터링 (AutoTradingBotService용)
    @Query("SELECT n FROM News n WHERE n.publishedAt > :since AND " +
           "(LOWER(n.title) LIKE LOWER(CONCAT('%', :titleKeyword, '%')) OR " +
           " LOWER(n.content) LIKE LOWER(CONCAT('%', :contentKeyword, '%'))) " +
           "ORDER BY n.publishedAt DESC")
    List<News> findByPublishedAtAfterAndTitleContainingOrContentContaining(
        @Param("since") LocalDateTime since,
        @Param("titleKeyword") String titleKeyword,
        @Param("contentKeyword") String contentKeyword
    );
    
    // PostgreSQL 전용: 감정 분석 통계 (전체)
    @Query(value = "SELECT sentiment, COUNT(*) as count FROM news " +
                   "WHERE sentiment IS NOT NULL " +
                   "GROUP BY sentiment", nativeQuery = true)
    List<Object[]> getSentimentStatistics();
    
    // ID 리스트로 뉴스 조회 (관심종목 뉴스용)
    @Query("SELECT n FROM News n WHERE n.id IN :ids ORDER BY n.publishedAt DESC")
    Page<News> findByIdInOrderByPublishedAtDesc(@Param("ids") List<Long> ids, Pageable pageable);
    
    // type 필드로 뉴스 조회 (관심종목 뉴스용 - 올바른 방법)
    @Query("SELECT n FROM News n WHERE n.type IN :types ORDER BY n.publishedAt DESC")
    Page<News> findByTypeInOrderByPublishedAtDesc(@Param("types") List<String> types, Pageable pageable);
}