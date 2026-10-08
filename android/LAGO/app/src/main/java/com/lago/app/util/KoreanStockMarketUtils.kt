package com.lago.app.util

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.format.DateTimeFormatter

/**
 * 한국 주식시장 영업일 계산 유틸리티
 */
object KoreanStockMarketUtils {
    
    /**
     * 한국 공휴일 목록 (고정 공휴일 + 주요 대체공휴일)
     */
    private fun getKoreanHolidays(year: Int): Set<LocalDate> {
        val holidays = mutableSetOf<LocalDate>()
        
        // 고정 공휴일
        holidays.add(LocalDate.of(year, Month.JANUARY, 1))     // 신정
        holidays.add(LocalDate.of(year, Month.MARCH, 1))       // 삼일절
        holidays.add(LocalDate.of(year, Month.MAY, 5))         // 어린이날
        holidays.add(LocalDate.of(year, Month.JUNE, 6))        // 현충일
        holidays.add(LocalDate.of(year, Month.AUGUST, 15))     // 광복절
        holidays.add(LocalDate.of(year, Month.OCTOBER, 3))     // 개천절
        holidays.add(LocalDate.of(year, Month.OCTOBER, 9))     // 한글날
        holidays.add(LocalDate.of(year, Month.DECEMBER, 25))   // 크리스마스
        
        // 2025년 공휴일 (매년 변동되는 음력 공휴일들)
        when (year) {
            2025 -> {
                // 설날 연휴 (1/28-1/30)
                holidays.add(LocalDate.of(2025, Month.JANUARY, 28))
                holidays.add(LocalDate.of(2025, Month.JANUARY, 29))
                holidays.add(LocalDate.of(2025, Month.JANUARY, 30))
                
                // 추석 연휴 (10/5-10/8)
                holidays.add(LocalDate.of(2025, Month.OCTOBER, 5))
                holidays.add(LocalDate.of(2025, Month.OCTOBER, 6))
                holidays.add(LocalDate.of(2025, Month.OCTOBER, 7))
                holidays.add(LocalDate.of(2025, Month.OCTOBER, 8))
                
                // 어린이날 대체공휴일 (5/6)
                holidays.add(LocalDate.of(2025, Month.MAY, 6))
                
                // 부처님오신날 (5/5) - 어린이날과 겹쳐서 대체공휴일
                // 이미 5/6이 대체공휴일로 추가됨
            }
            2024 -> {
                // 설날 연휴 (2/9-2/12)
                holidays.add(LocalDate.of(2024, Month.FEBRUARY, 9))
                holidays.add(LocalDate.of(2024, Month.FEBRUARY, 10))
                holidays.add(LocalDate.of(2024, Month.FEBRUARY, 11))
                holidays.add(LocalDate.of(2024, Month.FEBRUARY, 12))
                
                // 부처님오신날 (5/15)
                holidays.add(LocalDate.of(2024, Month.MAY, 15))
                
                // 추석 연휴 (9/16-9/18)
                holidays.add(LocalDate.of(2024, Month.SEPTEMBER, 16))
                holidays.add(LocalDate.of(2024, Month.SEPTEMBER, 17))
                holidays.add(LocalDate.of(2024, Month.SEPTEMBER, 18))
            }
            2026 -> {
                // 설날 연휴 (2/16-2/18)
                holidays.add(LocalDate.of(2026, Month.FEBRUARY, 16))
                holidays.add(LocalDate.of(2026, Month.FEBRUARY, 17))
                holidays.add(LocalDate.of(2026, Month.FEBRUARY, 18))
                
                // 부처님오신날 (5/24)
                holidays.add(LocalDate.of(2026, Month.MAY, 24))
                
                // 추석 연휴 (9/25-9/27)
                holidays.add(LocalDate.of(2026, Month.SEPTEMBER, 25))
                holidays.add(LocalDate.of(2026, Month.SEPTEMBER, 26))
                holidays.add(LocalDate.of(2026, Month.SEPTEMBER, 27))
            }
        }
        
        return holidays
    }
    
    /**
     * 주어진 날짜가 영업일인지 확인 (주말 + 공휴일 제외)
     */
    fun isTradingDay(date: LocalDate): Boolean {
        // 주말 체크
        if (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) {
            return false
        }
        
        // 공휴일 체크
        val holidays = getKoreanHolidays(date.year)
        return !holidays.contains(date)
    }
    
    /**
     * 가장 최근 영업일을 반환
     * 주말과 공휴일을 모두 제외한 가장 최근 일자
     */
    fun getLastTradingDay(): LocalDate {
        var date = LocalDate.now()
        
        // 영업일이 아니면 이전 영업일로 이동
        while (!isTradingDay(date)) {
            date = date.minusDays(1)
        }
        
        return date
    }
    
    /**
     * 지정된 날짜로부터 N일 전의 영업일을 반환
     */
    fun getTradingDaysBefore(fromDate: LocalDate, days: Int): LocalDate {
        var date = fromDate
        var count = 0
        
        while (count < days) {
            date = date.minusDays(1)
            // 영업일인 경우에만 카운트
            if (isTradingDay(date)) {
                count++
            }
        }
        
        return date
    }
    
    /**
     * 차트 API용 날짜 범위 생성
     * 최근 영업일 기준으로 충분한 데이터를 가져올 수 있는 범위 반환
     */
    fun getChartDateRange(): Pair<String, String> {
        val lastTradingDay = getLastTradingDay()
        
        // 최근 영업일로부터 10일 전부터 (주말 포함해서 실제로는 더 이전)
        val startDate = lastTradingDay.minusDays(15) // 여유있게 15일 전부터
        val endDate = lastTradingDay
        
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return Pair(startDate.format(formatter), endDate.format(formatter))
    }
    
    /**
     * 차트 API용 DateTime 범위 생성 (시간 포함)
     */
    fun getChartDateTimeRange(): Pair<String, String> {
        val (startDate, endDate) = getChartDateRange()
        return Pair("${startDate}T09:00:00", "${endDate}T15:30:00")
    }
    
    /**
     * 디버그용: 최근 영업일 정보 출력
     */
    fun logTradingDayInfo() {
        val today = LocalDate.now()
        val lastTradingDay = getLastTradingDay()
        val (startDate, endDate) = getChartDateRange()
        val holidays = getKoreanHolidays(today.year)
        
        android.util.Log.d("KoreanStockMarket", "📅 오늘: $today (${today.dayOfWeek}) - 영업일: ${isTradingDay(today)}")
        android.util.Log.d("KoreanStockMarket", "📅 최근 영업일: $lastTradingDay (${lastTradingDay.dayOfWeek})")
        android.util.Log.d("KoreanStockMarket", "📅 차트 데이터 범위: $startDate ~ $endDate")
        
        // 오늘이 공휴일인지 확인
        if (holidays.contains(today)) {
            android.util.Log.w("KoreanStockMarket", "🔴 오늘은 공휴일입니다: $today")
        }
        
        // 이번 달 공휴일 목록
        val thisMonthHolidays = holidays.filter { 
            it.month == today.month && it.year == today.year 
        }
        if (thisMonthHolidays.isNotEmpty()) {
            android.util.Log.d("KoreanStockMarket", "📋 이번 달 공휴일: ${thisMonthHolidays.joinToString(", ")}")
        }
    }
}