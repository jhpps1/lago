package com.lago.app.util

/**
 * 차트 인터벌 상수
 * API에서 사용하는 interval 파라미터 값들
 */
enum class ChartInterval(val value: String) {
    MINUTE("MINUTE"),
    MINUTE3("MINUTE3"),
    MINUTE5("MINUTE5"),
    MINUTE10("MINUTE10"),
    MINUTE15("MINUTE15"),
    MINUTE30("MINUTE30"),
    MINUTE60("MINUTE60"),
    DAY("DAY"),
    WEEK("WEEK"),
    MONTH("MONTH"),
    YEAR("YEAR");
    
    companion object {
        /**
         * 가장 많이 사용되는 인터벌들
         */
        val COMMON_INTERVALS = listOf(
            MINUTE, MINUTE5, MINUTE15, MINUTE30, MINUTE60, DAY, WEEK, MONTH
        )
        
        /**
         * 실시간 차트용 짧은 인터벌들
         */
        val REALTIME_INTERVALS = listOf(
            MINUTE, MINUTE3, MINUTE5, MINUTE10, MINUTE15, MINUTE30
        )
        
        /**
         * 장기 분석용 긴 인터벌들
         */
        val LONGTERM_INTERVALS = listOf(
            DAY, WEEK, MONTH, YEAR
        )
    }
}