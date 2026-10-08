package com.example.LAGO.service;

import com.example.LAGO.dto.response.ChartAnalysisResponse;
import com.example.LAGO.constants.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface ChartAnalysisService {

    /**
     * 주어진 주식 ID에 대한 차트 패턴 분석을 요청합니다.
     *
     * @param stockCode 종목코드
     * @param chartMode 모의투자/챌린지
     * @param interval 간격
     * @param fromTimeDate 분석시작일시
     * @param toDateTime 분석종료일시
     * @return 감지된 차트 패턴 목록
     */
    List<ChartAnalysisResponse> analyzePatterns(String stockCode, ChartMode chartMode, Interval interval, LocalDateTime fromTimeDate, LocalDateTime toDateTime);
}