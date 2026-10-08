package com.example.LAGO.repository;

import com.example.LAGO.domain.HistoryChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface HistoryChallengeRepository extends JpaRepository<HistoryChallenge, Integer> {
    /**
     * 특정 날짜에 진행 중인 역사챌린지를 조회합니다.
     *
     * @param targetDate 조회일시
     * @return 역사챌린지 정보
     */
    @Query("SELECT hc FROM HistoryChallenge hc WHERE :targetDate BETWEEN hc.startDate AND hc.endDate")
    HistoryChallenge findByDate(@Param("targetDate") LocalDateTime targetDate);

    /**
     * 특정 종목 코드에 해당하는 현재 진행 중인 역사챌린지를 조회합니다.
     * @param stockCode 종목 코드
     * @param now 현재 시간
     * @return 현재 진행 중인 역사챌린지 정보
     */
    @Query("SELECT hc FROM HistoryChallenge hc WHERE hc.stockCode = :stockCode AND :now BETWEEN hc.startDate AND hc.endDate")
    HistoryChallenge findActiveChallengeByStockCode(@Param("stockCode") String stockCode, @Param("now") LocalDateTime now);
}

