package com.example.LAGO.service;

import com.example.LAGO.constants.Interval;
import com.example.LAGO.dto.response.HistoryChallengeDataResponse;
import com.example.LAGO.dto.response.HistoryChallengeNewsResponse;
import com.example.LAGO.dto.response.HistoryChallengeResponse;

import java.time.LocalDateTime;
import java.util.List;

public interface HistoryChallengeService {
    HistoryChallengeDataResponse getLatestData();

    /**
     * 현재 진행 중인 역사챌린지 정보를 반환합니다.
     *
     * @return 역사챌린지 정보
     */
    HistoryChallengeResponse getHistoryChallenge();

    /**
     * 현재 시점 까지의 역사챌린지 주가 정보 목록을 반환합니다.
     *
     * @param challengeId 챌린지 ID
     * @param interval 간격
     * @return 역사챌린지 주가 정보 목록
     */
    List<HistoryChallengeDataResponse> getHistoryChallengeData(Integer challengeId, Interval interval, LocalDateTime fromDateTime, LocalDateTime toDateTime);

    List<HistoryChallengeNewsResponse> getChallengeNewsList(Integer challengeId, LocalDateTime targetDateTime);

    HistoryChallengeNewsResponse getChallengeNews(Integer challengeId, Integer challengeNewsId);
}