package com.example.LAGO.repository;

import com.example.LAGO.domain.HistoryChallengeData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoryChallengeDataRepository extends JpaRepository<HistoryChallengeData, Integer> {

    /**
     * 특정 챌린지의 가장 최신 주가 데이터를 조회합니다.
     * @param targetDate 조회시각
     * @return 역사챌린지 최신 주가 데이터
     */
    @Query("SELECT hcd FROM HistoryChallengeData hcd " +
            "WHERE hcd.eventDateTime <= :targetDate " +
            "ORDER BY hcd.eventDateTime DESC LIMIT 1 " )
    HistoryChallengeData findLatestChallengeData(@Param("targetDate") LocalDateTime targetDate);

    /**
     * 특정 챌린지의 지정된 과거 내의 모든 주가 데이터를 조회합니다.
     * @param challengeId 챌린지 ID
     * @param interval 조회 간격
     * @param fromDateTime 조회 시작구간
     * @param toDateTime 조회 종료구간
     * @return 해당 시간 범위의 주가 데이터 목록 (시간순)
     */
    @Query(value = "SELECT \n" +
            "    ROW_NUMBER() OVER (ORDER BY bucket) AS row_id, " +
            "    sub.* " +
            "FROM (" +
                "SELECT " +
                    "time_bucket(CAST(:interval AS INTERVAL), hcd.origin_date_time) AS bucket, " +
                    "FIRST(hcd.event_date_time, hcd.origin_date_time) AS event_date, " +
                    "FIRST(hcd.open_price, hcd.origin_date_time) AS open, " +
                    "MAX(hcd.high_price) AS high, " +
                    "MIN(hcd.low_price) AS low, " +
                    "LAST(hcd.close_price, hcd.origin_date_time) AS close, " +
                    "SUM(hcd.volume) AS volume " +
                "FROM history_challenge_data hcd " +
                "WHERE hcd.challenge_id = :challengeId " +
                    "AND hcd.event_date_time BETWEEN :fromDateTime AND :toDateTime " +
                "GROUP BY bucket " +
                "ORDER BY bucket " +
            ") sub", nativeQuery = true)
    List<Object[]> findAggregatedByChallengeIdAndDate(
            @Param("challengeId") Integer challengeId,
            @Param("interval") String interval,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime
    );

    /**
     * 특정 종목 코드에 해당하는 역사챌린지 데이터를 집계하여 조회합니다.
     * @param stockCode 종목 코드
     * @param interval 집계 간격
     * @param fromDateTime 조회 시작구간
     * @param toDateTime 조회 종료구간
     * @return 집계된 주가 데이터 목록
     */
    @Query(value = "SELECT \n" +
            "    time_bucket(CAST(:interval AS INTERVAL), hcd.origin_date_time) AS bucket, " +
            "    FIRST(hcd.open_price, hcd.origin_date_time) AS open, " +
            "    MAX(hcd.high_price) AS high, " +
            "    MIN(hcd.low_price) AS low, " +
            "    LAST(hcd.close_price, hcd.origin_date_time) AS close, " +
            "    SUM(hcd.volume) AS volume " +
            "FROM history_challenge_data hcd " +
                "JOIN history_challenge hc ON hcd.challenge_id = hc.challenge_id " +
            "WHERE hc.stock_code = :stockCode " +
                "AND hcd.origin_date_time BETWEEN :fromDateTime AND :toDateTime " +
            "GROUP BY bucket " +
            "ORDER BY bucket", nativeQuery = true)
    List<Object[]> findAggregatedByStockCodeAndDateRangeAndInterval(
            @Param("stockCode") String stockCode,
            @Param("interval") String interval,
            @Param("fromDateTime") LocalDateTime fromDateTime,
            @Param("toDateTime") LocalDateTime toDateTime
    );
}
