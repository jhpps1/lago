package com.example.LAGO.repository;

import com.example.LAGO.domain.Interest;
import com.example.LAGO.domain.StockInfo;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InterestRepository extends JpaRepository<Interest, Integer> {
    // 목록 조회 (특정 유저의 관심 종목)
    List<Interest> findByUserId(Long userId);

    // user_id + stock_info_id
    // 존재 여부 (중복 추가 방지에 유용)
    boolean existsByUserIdAndStockInfoId(Long userId, Integer stockInfoId);

    // 삭제 (관심종목 해제)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from Interest i where i.userId = :userId and i.stockInfoId = :sid")
    long deleteByUserIdAndStockInfoId(@Param("userId") Long userId, @Param("sid") Integer stockInfoId);

    // N+1 회피: stockInfo까지 한 번에 로드하고 싶을 때
    @Query("select i from Interest i join fetch i.stockInfo where i.userId = :userId")
    List<Interest> findWithStockInfoByUserId(@Param("userId") Long userId);

    // user_id + code(stock_info)
    // user_id + code 존재 여부
    boolean existsByUserIdAndStockInfo_Code(Long userId, String code);

    // user_id + code 삭제 (파생 삭제 쿼리)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    long deleteByUserIdAndStockInfo_Code(Long userId, String code);

    // 단건 조회
    Optional<Interest> findByUserIdAndStockInfo_Code(Long userId, String code);
}
