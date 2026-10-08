package com.lago.app.data.local.prefs

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 차트 패턴 분석 횟수 관리를 위한 SharedPreferences
 * 하루 3회 제한, 자정에 리셋
 */
@Singleton
class PatternAnalysisPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        "pattern_analysis_prefs", 
        Context.MODE_PRIVATE
    )
    
    companion object {
        private const val PATTERN_ANALYSIS_COUNT = "pattern_analysis_count"
        private const val LAST_RESET_DATE = "last_reset_date"
        private const val MAX_DAILY_COUNT = 3
        
        private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    }
    
    /**
     * 남은 분석 횟수 조회
     */
    fun getRemainingCount(): Int {
        checkAndResetIfNewDay()
        return prefs.getInt(PATTERN_ANALYSIS_COUNT, MAX_DAILY_COUNT)
    }
    
    /**
     * 사용 가능 여부 확인 (차감하지 않음)
     * @return 사용 가능하면 true
     */
    fun canUse(): Boolean {
        checkAndResetIfNewDay()
        val currentCount = prefs.getInt(PATTERN_ANALYSIS_COUNT, MAX_DAILY_COUNT)
        return currentCount > 0
    }
    
    /**
     * 분석 횟수 1회 차감
     * @return 차감 성공 시 true, 횟수 부족 시 false
     */
    fun useCount(): Boolean {
        checkAndResetIfNewDay()
        val currentCount = prefs.getInt(PATTERN_ANALYSIS_COUNT, MAX_DAILY_COUNT)
        
        return if (currentCount > 0) {
            prefs.edit()
                .putInt(PATTERN_ANALYSIS_COUNT, currentCount - 1)
                .apply()
            android.util.Log.d("PatternAnalysisPreferences", "📊 분석 횟수 사용: ${currentCount} → ${currentCount - 1}")
            true
        } else {
            android.util.Log.w("PatternAnalysisPreferences", "📊 분석 횟수 부족: $currentCount")
            false
        }
    }
    
    /**
     * 분석 횟수 1회 복구 (API 호출 실패 시)
     */
    fun restoreCount() {
        val currentCount = prefs.getInt(PATTERN_ANALYSIS_COUNT, MAX_DAILY_COUNT)
        val restoredCount = kotlin.math.min(currentCount + 1, MAX_DAILY_COUNT)
        
        prefs.edit()
            .putInt(PATTERN_ANALYSIS_COUNT, restoredCount)
            .apply()
        android.util.Log.d("PatternAnalysisPreferences", "📊 분석 횟수 복구: ${currentCount} → ${restoredCount}")
    }
    
    /**
     * 일일 횟수를 최대치로 리셋
     */
    fun resetDailyCount() {
        val today = dateFormat.format(Date())
        prefs.edit()
            .putInt(PATTERN_ANALYSIS_COUNT, MAX_DAILY_COUNT)
            .putString(LAST_RESET_DATE, today)
            .apply()
        android.util.Log.d("PatternAnalysisPreferences", "📊 일일 분석 횟수 리셋: $MAX_DAILY_COUNT")
    }
    
    /**
     * 새로운 날이면 자동으로 횟수 리셋
     */
    fun checkAndResetIfNewDay() {
        val today = dateFormat.format(Date())
        val lastResetDate = prefs.getString(LAST_RESET_DATE, "")
        
        if (lastResetDate != today) {
            android.util.Log.d("PatternAnalysisPreferences", "📊 새로운 날 감지: $lastResetDate → $today")
            resetDailyCount()
        }
    }
    
    /**
     * 최대 일일 분석 횟수 반환
     */
    fun getMaxDailyCount(): Int = MAX_DAILY_COUNT
}