package com.example.LAGO.repository;

import com.example.LAGO.domain.HistoryChallengeNews;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistoryChallengeNewsRepository extends JpaRepository<HistoryChallengeNews, Integer> {

    /**
     * 역사챌린지의 가장 최근 뉴스 목록을 조회합니다.
     *
     * @param challengeId 챌린지 ID
     * @param targetDate 조회시각
     * @return 역사챌린지 최신 뉴스 목록
     */
    @Query("SELECT hcn FROM HistoryChallengeNews hcn " +
            "WHERE hcn.challengeId = :challengeId " +
            "AND hcn.publishedAt <= :targetDate " +
            "ORDER BY hcn.publishedAt DESC LIMIT 10 ")
    List<HistoryChallengeNews> findLatestChallengeNewsList(@Param("challengeId") Integer challengeId, @Param("targetDate") LocalDateTime targetDate);
}
