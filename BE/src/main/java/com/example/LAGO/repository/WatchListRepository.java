package com.example.LAGO.repository;

import com.example.LAGO.domain.WatchList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WatchListRepository extends JpaRepository<WatchList, Long> {
    
    // 활성화된 모든 관심종목 조회
    @Query("SELECT w FROM WatchList w WHERE w.isActive = true")
    List<WatchList> findAllActive();
    
    // 특정 사용자의 활성화된 관심종목 조회
    @Query("SELECT w FROM WatchList w WHERE w.user.userId = :userId AND w.isActive = true")
    List<WatchList> findActiveByUserId(@Param("userId") Long userId);
    
    // 특정 사용자의 활성화된 관심종목 종목코드 조회
    @Query("SELECT w.stock.code FROM WatchList w WHERE w.user.userId = :userId AND w.isActive = true")
    List<String> findActiveStockCodesByUserId(@Param("userId") Long userId);
    
    // 사용자와 종목으로 관심종목 조회
    @Query("SELECT w FROM WatchList w WHERE w.user.userId = :userId AND w.stock.code = :stockCode")
    Optional<WatchList> findByUserIdAndStockCode(@Param("userId") Long userId, @Param("stockCode") String stockCode);
    
    // 특정 종목의 관심종목 등록 수 카운트
    @Query("SELECT COUNT(w) FROM WatchList w WHERE w.stock.code = :stockCode AND w.isActive = true")
    Long countActiveByStockCode(@Param("stockCode") String stockCode);
    
    // 우선순위가 높은 순으로 관심종목 조회
    @Query("SELECT w FROM WatchList w WHERE w.user.userId = :userId AND w.isActive = true ORDER BY w.priority DESC NULLS LAST")
    List<WatchList> findActiveByUserIdOrderByPriority(@Param("userId") Long userId);
}