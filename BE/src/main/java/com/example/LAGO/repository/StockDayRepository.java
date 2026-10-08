package com.example.LAGO.repository;

import com.example.LAGO.domain.StockDay;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface StockDayRepository extends JpaRepository<StockDay, Integer> {

    /**
     * 종목 정보 ID와 날짜로 일봉 데이터 조회
     *
     * @param stockInfoId 종목 정보 ID
     * @param date        거래일
     * @return 일봉 데이터
     */
    Optional<StockDay> findByStockInfoStockInfoIdAndDate(Integer stockInfoId, LocalDate date);

    /**
     * 종목 정보 ID로 최근 N일 일봉 데이터 조회 (최신순)
     *
     * @param stockInfoId 종목 정보 ID
     * @param pageable    페이징 정보
     * @return 일봉 데이터 리스트 (최신순)
     */
    @Query("SELECT sd FROM StockDay sd WHERE sd.stockInfo.stockInfoId = :stockInfoId " +
            "ORDER BY sd.date DESC")
    List<StockDay> findByStockInfoStockInfoIdOrderByDateDesc(Integer stockInfoId, Pageable pageable);


    /**
     * 종목 정보 ID로 특정 기간 일봉 데이터 조회
     *
     * @param stockInfoId 종목 정보 ID
     * @param startDate   시작일
     * @param endDate     종료일
     * @return 일봉 데이터 리스트 (날짜순)
     */
    List<StockDay> findByStockInfoStockInfoIdAndDateBetweenOrderByDateAsc(Integer stockInfoId,
                                                                 LocalDate startDate,
                                                                 LocalDate endDate);

    // newDate 필드가 제거되어 이 메서드는 사용하지 않음
    // 대신 위의 findByStockInfoStockInfoIdAndDateBetweenOrderByDateAsc 메서드 사용

    /**
     * 종목 정보 ID로 최신 일봉 데이터 조회
     *
     * @param stockInfoId 종목 정보 ID
     * @return 최신 일봉 데이터
     */
    Optional<StockDay> findTopByStockInfoStockInfoIdOrderByDateDesc(Integer stockInfoId);

    /**
     * 특정 날짜의 모든 종목 일봉 데이터 조회
     *
     * @param date 거래일
     * @return 일봉 데이터 리스트
     */
    List<StockDay> findByDateOrderByStockInfoStockInfoIdAsc(LocalDate date);

    // StockInfo.code 대신 StockInfo.stockCode로 조회
    List<StockDay> findByStockInfo_CodeAndDateBetweenOrderByDateAsc(
            String stockCode,
            LocalDate start,
            LocalDate end
    );
    StockDay findTopByStockInfo_CodeAndDateOrderByDateDesc(String stockCode, LocalDate date);

}
