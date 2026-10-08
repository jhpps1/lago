package com.lago.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 주식 장시간 관련 유틸리티
 */
object MarketTimeUtils {
    
    private val KOREA_ZONE = ZoneId.of("Asia/Seoul")
    private val MARKET_OPEN_TIME = LocalTime.of(9, 0)  // 09:00
    private val MARKET_CLOSE_TIME = LocalTime.of(15, 30) // 15:30
    private val DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    
    /**
     * 현재 주식 장이 열려있는지 확인
     * @return true: 장 시간(09:00-15:30, 평일), false: 장 마감
     */
    fun isMarketOpen(): Boolean {
        val koreaTime = LocalDateTime.now(KOREA_ZONE)
        val dayOfWeek = koreaTime.dayOfWeek
        val time = koreaTime.toLocalTime()
        
        // 주말 제외
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return false
        }
        
        // 장시간: 09:00 ~ 15:30 (한국시간)
        return time >= MARKET_OPEN_TIME && time <= MARKET_CLOSE_TIME
    }
    
    /**
     * 장 마감 후인지 확인
     * @return true: 15:30 이후, false: 15:30 이전
     */
    fun isAfterMarketClose(): Boolean {
        val koreaTime = LocalDateTime.now(KOREA_ZONE)
        val dayOfWeek = koreaTime.dayOfWeek
        val time = koreaTime.toLocalTime()
        
        // 주말은 장 마감으로 간주
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return true
        }
        
        return time > MARKET_CLOSE_TIME
    }
    
    /**
     * 장 시작 전인지 확인
     * @return true: 09:00 이전, false: 09:00 이후
     */
    fun isBeforeMarketOpen(): Boolean {
        val koreaTime = LocalDateTime.now(KOREA_ZONE)
        val dayOfWeek = koreaTime.dayOfWeek
        val time = koreaTime.toLocalTime()
        
        // 주말은 장 시작 전으로 간주
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return true
        }
        
        return time < MARKET_OPEN_TIME
    }
    
    /**
     * 종가 데이터를 조회할 날짜를 반환
     * - 09:00 이전: 전날 종가
     * - 15:30 이후: 당일 종가 (없으면 전날)
     * @return "yyyy-MM-dd" 형식의 날짜 문자열
     */
    fun getTargetDateForClosePrice(): String {
        val koreaTime = LocalDateTime.now(KOREA_ZONE)
        val time = koreaTime.toLocalTime()
        val today = koreaTime.toLocalDate()
        
        return when {
            time.isBefore(MARKET_OPEN_TIME) -> {
                // 09:00 이전: 전날 종가 사용
                getPreviousBusinessDay(today).format(DATE_FORMAT)
            }
            time.isAfter(MARKET_CLOSE_TIME) -> {
                // 15:30 이후: 당일 종가 사용
                today.format(DATE_FORMAT)
            }
            else -> {
                // 09:00~15:30: 실시간 모드 (이 함수 호출되지 않음)
                today.format(DATE_FORMAT)
            }
        }
    }
    
    /**
     * 이전 영업일 계산 (주말 건너뛰기)
     * @param date 기준 날짜
     * @return 이전 영업일
     */
    private fun getPreviousBusinessDay(date: LocalDate): LocalDate {
        var previousDay = date.minusDays(1)
        
        // 주말이면 금요일까지 이동
        while (previousDay.dayOfWeek == DayOfWeek.SATURDAY || 
               previousDay.dayOfWeek == DayOfWeek.SUNDAY) {
            previousDay = previousDay.minusDays(1)
        }
        
        return previousDay
    }
    
    /**
     * 오늘 날짜 문자열 반환
     * @return "yyyy-MM-dd" 형식의 오늘 날짜
     */
    fun getTodayString(): String {
        return LocalDate.now(KOREA_ZONE).format(DATE_FORMAT)
    }
    
    /**
     * 어제 날짜 문자열 반환
     * @return "yyyy-MM-dd" 형식의 어제 날짜
     */
    fun getYesterdayString(): String {
        return LocalDate.now(KOREA_ZONE).minusDays(1).format(DATE_FORMAT)
    }
    
    /**
     * 현재 한국 시간
     * @return LocalDateTime 한국 시간
     */
    fun getCurrentKoreaTime(): LocalDateTime {
        return LocalDateTime.now(KOREA_ZONE)
    }
    
    /**
     * 디버깅용 시장 상태 문자열
     * @return 현재 시장 상태 설명
     */
    fun getMarketStatusString(): String {
        return when {
            isMarketOpen() -> "장 중 (실시간 모드)"
            isAfterMarketClose() -> "장 마감 (종가 모드)"
            isBeforeMarketOpen() -> "장 시작 전 (전날 종가 모드)"
            else -> "시장 상태 불명"
        }
    }
}