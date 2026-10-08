package com.lago.app.presentation.ui.chart.v5

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * 차트 시간 처리 통합 관리자
 * 모든 시간 관련 변환과 포맷팅을 중앙집중화
 */
object ChartTimeManager {

    private val KST_ZONE = ZoneId.of("Asia/Seoul")
    private val UTC_ZONE = ZoneId.of("UTC")

    // API 요청용 포맷터
    private val API_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")

    // 표시용 포맷터들
    private val DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val DATE_ONLY_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 모든 타임스탬프를 epoch seconds로 정규화
     * @param timestamp milliseconds 또는 seconds
     * @return epoch seconds (10자리)
     */
    fun normalizeToEpochSeconds(timestamp: Long): Long {
        return if (timestamp > 9_999_999_999L) {
            timestamp / 1000 // milliseconds -> seconds
        } else {
            timestamp // 이미 seconds
        }
    }


    /**
     * JavaScript 차트용 시간 포맷 (항상 epoch seconds)
     * @param timestamp milliseconds 또는 seconds
     * @return epoch seconds (JavaScript 호환)
     */
    fun formatForJavaScript(timestamp: Long): Long {
        return normalizeToEpochSeconds(timestamp)
    }

    /**
     * 표시용 시간 포맷팅 (KST 기준)
     * @param epochSeconds epoch seconds
     * @param includeTime 시간 포함 여부 (false면 날짜만)
     * @return 포맷된 시간 문자열
     */
    fun formatForDisplay(epochSeconds: Long, includeTime: Boolean = true): String {
        val zonedDateTime = Instant.ofEpochSecond(epochSeconds).atZone(KST_ZONE)
        return if (includeTime) {
            zonedDateTime.format(DISPLAY_FORMATTER)
        } else {
            zonedDateTime.format(DATE_ONLY_FORMATTER)
        }
    }

    /**
     * API 요청용 시간 포맷 (KST 기준)
     * @param epochSeconds epoch seconds
     * @return API 요청용 시간 문자열 ("yyyy-MM-ddTHH:mm:ss")
     */
    fun formatForApi(epochSeconds: Long): String {
        val zonedDateTime = Instant.ofEpochSecond(epochSeconds).atZone(KST_ZONE)
        return zonedDateTime.format(API_FORMATTER)
    }

    /**
     * 현재 KST 시간을 epoch seconds로 반환
     */
    fun getCurrentKstEpochSeconds(): Long {
        return ZonedDateTime.now(KST_ZONE).toEpochSecond()
    }

    /**
     * 시간프레임별 적절한 과거 시간 계산
     * @param timeFrame 시간프레임
     * @return 과거 시간 (epoch seconds)
     */
    fun calculatePastTime(timeFrame: String): Long {
        val now = ZonedDateTime.now(KST_ZONE)
        val pastTime = when (timeFrame) {
            "1" -> now.minusHours(24) // 1분봉: 1일 전
            "3" -> now.minusDays(3) // 3분봉: 3일 전
            "5" -> now.minusDays(5) // 5분봉: 5일 전
            "10" -> now.minusDays(10) // 10분봉: 10일 전
            "15" -> now.minusDays(15) // 15분봉: 15일 전
            "30" -> now.minusDays(30) // 30분봉: 30일 전
            "60" -> now.minusDays(60) // 60분봉: 60일 전
            "D" -> now.minusMonths(6) // 일봉: 6개월 전
            "W" -> now.minusYears(2) // 주봉: 2년 전
            "M" -> now.minusYears(5) // 월봉: 5년 전
            "Y" -> now.minusYears(10) // 년봉: 10년 전
            else -> now.minusDays(30) // 기본값: 30일 전
        }
        return pastTime.toEpochSecond()
    }


    /**
     * 시간프레임이 분봉인지 확인
     */
    fun isMinuteFrame(timeFrame: String): Boolean {
        return timeFrame in listOf("1", "3", "5", "10", "15", "30", "60")
    }

    /**
     * 시간프레임이 일봉 이상인지 확인
     */
    fun isDailyOrAbove(timeFrame: String): Boolean {
        return timeFrame in listOf("D", "W", "M", "Y")
    }

    /**
     * 날짜 문자열을 epoch seconds로 파싱 (yyyy-MM-dd 형식)
     */
    fun parseDate(dateString: String): Long {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        return try {
            normalizeToEpochSeconds(format.parse(dateString)?.time ?: 0L)
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 날짜시간 문자열을 epoch seconds로 파싱 (yyyy-MM-ddTHH:mm:ss 형식)
     */
    fun parseDateTime(dateTimeString: String): Long {
        val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        return try {
            normalizeToEpochSeconds(format.parse(dateTimeString)?.time ?: 0L)
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * 월 정수를 epoch seconds로 변환 (YYYYMM 형식)
     */
    fun parseMonthDate(monthInt: Int): Long {
        val year = monthInt / 100
        val month = (monthInt % 100) - 1
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, month)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return normalizeToEpochSeconds(calendar.timeInMillis)
    }

    /**
     * 년도를 epoch seconds로 변환
     */
    fun parseYearDate(year: Int): Long {
        val calendar = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.YEAR, year)
            set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return normalizeToEpochSeconds(calendar.timeInMillis)
    }

    /**
     * 역사챌린지 날짜시간 문자열을 milliseconds로 파싱
     * 여러 형식을 지원 (yyyy-MM-ddTHH:mm:ss, yyyy-MM-dd HH:mm:ss)
     */
    fun parseHistoryChallengeDateTime(dateTimeString: String): Long {
        return try {
            val format = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            format.parse(dateTimeString)?.time ?: 0L
        } catch (e: Exception) {
            try {
                val format2 = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                format2.parse(dateTimeString)?.time ?: 0L
            } catch (e2: Exception) {
                0L
            }
        }
    }

    /**
     * 내부 시간프레임을 API 시간프레임으로 변환
     * @param timeFrame 내부 시간프레임 ("1", "3", "5", "15", "30", "60", "D", "W", "M", "Y")
     * @return API 시간프레임 ("MINUTE", "MINUTE3", "MINUTE5", "MINUTE15", "MINUTE30", "MINUTE60", "DAY", "WEEK", "MONTH", "YEAR")
     */
    fun toApiTimeFrame(timeFrame: String): String {
        return when (timeFrame) {
            "1" -> "MINUTE"
            "3" -> "MINUTE3"
            "5" -> "MINUTE5"
            "10" -> "MINUTE10"
            "15" -> "MINUTE15"
            "30" -> "MINUTE30"
            "60" -> "MINUTE60"
            "D" -> "DAY"
            "W" -> "WEEK"
            "M" -> "MONTH"
            "Y" -> "YEAR"
            else -> "MINUTE" // 기본값
        }
    }

    /**
     * API 시간프레임을 내부 시간프레임으로 변환
     * @param apiTimeFrame API 시간프레임 ("MINUTE", "MINUTE3", "DAY" 등)
     * @return 내부 시간프레임 ("1", "3", "D" 등)
     */
    fun fromApiTimeFrame(apiTimeFrame: String): String {
        return when (apiTimeFrame.uppercase()) {
            "MINUTE" -> "1"
            "MINUTE3" -> "3"
            "MINUTE5" -> "5"
            "MINUTE10" -> "10"
            "MINUTE15" -> "15"
            "MINUTE30" -> "30"
            "MINUTE60" -> "60"
            "DAY" -> "D"
            "WEEK" -> "W"
            "MONTH" -> "M"
            "YEAR" -> "Y"
            else -> "1" // 기본값
        }
    }

    /**
     * 디버깅용 시간 정보 출력
     */
    fun debugTimeInfo(label: String, timestamp: Long) {
        val normalized = normalizeToEpochSeconds(timestamp)
        val kstTime = formatForDisplay(normalized)

        android.util.Log.d("ChartTimeManager", buildString {
            append("$label: ")
            append("original=$timestamp, ")
            append("normalized=$normalized, ")
            append("KST=$kstTime")
        })
    }
}