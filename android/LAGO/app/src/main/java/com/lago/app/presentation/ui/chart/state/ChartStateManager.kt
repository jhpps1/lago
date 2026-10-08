package com.lago.app.presentation.ui.chart.state

import kotlinx.coroutines.flow.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 차트 상태 정의
 */
sealed class ChartState {
    object Initializing : ChartState()
    object WebViewLoading : ChartState()
    object WebViewReady : ChartState()
    object DataLoading : ChartState()
    object ChartReady : ChartState()
    data class PatternAnalyzing(val progress: Float = 0f) : ChartState()
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val errorType: ErrorType = ErrorType.GENERAL
    ) : ChartState()
}

enum class ErrorType {
    NETWORK,
    WEBVIEW,
    DATA_PARSING,
    TIMEOUT,
    GENERAL
}

/**
 * 차트 이벤트
 */
sealed class ChartEvent {
    object WebViewInitialized : ChartEvent()
    object JavaScriptReady : ChartEvent()
    object DataReceived : ChartEvent()
    object ChartRendered : ChartEvent()
    data class PatternAnalysisStarted(val fromTime: String, val toTime: String) : ChartEvent()
    data class PatternAnalysisCompleted(val resultCount: Int) : ChartEvent()
    data class ErrorOccurred(val error: String, val errorType: ErrorType) : ChartEvent()
    object RetryRequested : ChartEvent()
}

/**
 * 차트 상태 관리자
 * 차트의 복잡한 초기화 과정과 상태 전환을 중앙집중식으로 관리
 */
@Singleton
class ChartStateManager @Inject constructor() {

    private val _state = MutableStateFlow<ChartState>(ChartState.Initializing)
    val state: StateFlow<ChartState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<ChartEvent>(replay = 0)
    val events: SharedFlow<ChartEvent> = _events.asSharedFlow()

    // 상태 전환 로그
    private val stateTransitionLog = mutableListOf<Pair<ChartState, Long>>()

    /**
     * 상태 전환
     */
    fun transitionTo(newState: ChartState) {
        val currentState = _state.value

        if (isValidTransition(currentState, newState)) {
            android.util.Log.d("ChartStateManager",
                "상태 전환: ${currentState::class.simpleName} → ${newState::class.simpleName}")

            _state.value = newState
            logStateTransition(newState)
            handleStateEntry(newState)
        } else {
            android.util.Log.w("ChartStateManager",
                "잘못된 상태 전환 시도: ${currentState::class.simpleName} → ${newState::class.simpleName}")
        }
    }

    /**
     * 이벤트 발생
     */
    fun emitEvent(event: ChartEvent) {
        android.util.Log.d("ChartStateManager", "이벤트 발생: ${event::class.simpleName}")
        CoroutineScope(kotlinx.coroutines.Dispatchers.Main).launch {
            _events.emit(event)
        }
    }

    /**
     * 상태 전환 유효성 검증
     */
    private fun isValidTransition(from: ChartState, to: ChartState): Boolean {
        return when (from) {
            is ChartState.Initializing -> {
                to is ChartState.WebViewLoading || to is ChartState.Error
            }
            is ChartState.WebViewLoading -> {
                to is ChartState.WebViewReady || to is ChartState.Error
            }
            is ChartState.WebViewReady -> {
                to is ChartState.DataLoading || to is ChartState.Error
            }
            is ChartState.DataLoading -> {
                to is ChartState.ChartReady || to is ChartState.Error
            }
            is ChartState.ChartReady -> {
                to is ChartState.PatternAnalyzing ||
                        to is ChartState.DataLoading ||  // 데이터 새로고침
                        to is ChartState.Error
            }
            is ChartState.PatternAnalyzing -> {
                to is ChartState.ChartReady || to is ChartState.Error
            }
            is ChartState.Error -> {
                // 에러 상태에서는 모든 상태로 전환 가능 (재시도)
                true
            }
        }
    }

    /**
     * 상태 진입 시 처리
     */
    private fun handleStateEntry(state: ChartState) {
        when (state) {
            is ChartState.WebViewLoading -> {
                emitEvent(ChartEvent.WebViewInitialized)
            }
            is ChartState.WebViewReady -> {
                emitEvent(ChartEvent.JavaScriptReady)
            }
            is ChartState.DataLoading -> {
                emitEvent(ChartEvent.DataReceived)
            }
            is ChartState.ChartReady -> {
                emitEvent(ChartEvent.ChartRendered)
            }
            is ChartState.PatternAnalyzing -> {
                // 패턴 분석 시작 이벤트는 별도로 발생
            }
            is ChartState.Error -> {
                emitEvent(ChartEvent.ErrorOccurred(state.message, state.errorType))
            }
            else -> {}
        }
    }

    /**
     * 상태 전환 로그 기록
     */
    private fun logStateTransition(state: ChartState) {
        val timestamp = System.currentTimeMillis()
        stateTransitionLog.add(state to timestamp)

        // 로그 크기 제한 (최대 50개)
        if (stateTransitionLog.size > 50) {
            stateTransitionLog.removeAt(0)
        }
    }

    /**
     * 현재 상태 확인
     */
    fun getCurrentState(): ChartState = _state.value

    /**
     * 특정 상태인지 확인
     */
    fun isInState(stateClass: Class<out ChartState>): Boolean {
        return stateClass.isInstance(_state.value)
    }

    /**
     * 차트가 준비된 상태인지 확인
     */
    fun isChartReady(): Boolean = _state.value is ChartState.ChartReady

    /**
     * 로딩 중인지 확인
     */
    fun isLoading(): Boolean {
        return when (_state.value) {
            is ChartState.Initializing,
            is ChartState.WebViewLoading,
            is ChartState.DataLoading,
            is ChartState.PatternAnalyzing -> true
            else -> false
        }
    }

    /**
     * 에러 상태인지 확인
     */
    fun isError(): Boolean = _state.value is ChartState.Error

    /**
     * 재시도 가능한지 확인
     */
    fun canRetry(): Boolean {
        return when (val currentState = _state.value) {
            is ChartState.Error -> currentState.canRetry
            else -> false
        }
    }

    /**
     * 상태 리셋 (새로운 차트 로드 시)
     */
    fun reset() {
        android.util.Log.d("ChartStateManager", "상태 리셋")
        _state.value = ChartState.Initializing
        stateTransitionLog.clear()
    }

    /**
     * 디버그 정보 출력
     */
    fun getDebugInfo(): String {
        val currentState = _state.value
        val recentTransitions = stateTransitionLog.takeLast(5)

        return buildString {
            appendLine("=== Chart State Manager Debug Info ===")
            appendLine("Current State: ${currentState::class.simpleName}")
            if (currentState is ChartState.Error) {
                appendLine("Error Message: ${currentState.message}")
                appendLine("Error Type: ${currentState.errorType}")
                appendLine("Can Retry: ${currentState.canRetry}")
            }
            appendLine("Recent Transitions:")
            recentTransitions.forEach { (state, timestamp) ->
                val timeStr = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault())
                    .format(java.util.Date(timestamp))
                appendLine("  $timeStr - ${state::class.simpleName}")
            }
            appendLine("======================================")
        }
    }
}

/**
 * 상태 관리자 확장 함수들
 */

/**
 * 안전한 상태 전환 (코루틴 스코프 내에서)
 */
suspend fun ChartStateManager.safeTransitionTo(
    newState: ChartState,
    onError: (String) -> Unit = {}
) {
    try {
        transitionTo(newState)
    } catch (e: Exception) {
        android.util.Log.e("ChartStateManager", "상태 전환 중 오류", e)
        onError(e.message ?: "알 수 없는 오류")
        transitionTo(ChartState.Error(
            message = "상태 전환 실패: ${e.message}",
            canRetry = true,
            errorType = ErrorType.GENERAL
        ))
    }
}

/**
 * 조건부 상태 전환
 */
fun ChartStateManager.transitionIf(
    condition: () -> Boolean,
    newState: ChartState
) {
    if (condition()) {
        transitionTo(newState)
    }
}

/**
 * 타임아웃을 가진 상태 전환
 */
suspend fun ChartStateManager.transitionWithTimeout(
    newState: ChartState,
    timeoutMs: Long = 5000L,
    onTimeout: () -> Unit = {}
) {
    transitionTo(newState)

    kotlinx.coroutines.delay(timeoutMs)

    // 여전히 같은 상태이면 타임아웃 처리
    if (getCurrentState() == newState && newState !is ChartState.ChartReady) {
        onTimeout()
        transitionTo(ChartState.Error(
            message = "상태 전환 타임아웃: ${newState::class.simpleName}",
            canRetry = true,
            errorType = ErrorType.TIMEOUT
        ))
    }
}