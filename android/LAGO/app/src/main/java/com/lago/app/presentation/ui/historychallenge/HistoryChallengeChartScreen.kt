package com.lago.app.presentation.ui.historychallenge

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.ui.res.painterResource
import com.lago.app.R
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.lago.app.presentation.viewmodel.historychallenge.HistoryChallengeChartViewModel
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
// 커스텀 바텀시트를 위한 imports
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.launch
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.clipToBounds
import kotlin.math.abs
// Theme imports
import com.lago.app.presentation.theme.*
// Widget imports
import com.lago.app.presentation.ui.widget.DockingHeader
import com.lago.app.presentation.ui.widget.AnimatedHeaderBox
// Domain imports
import com.lago.app.domain.entity.CandlestickData
import com.lago.app.domain.entity.ChartConfig
import com.lago.app.domain.entity.LineData
import com.lago.app.domain.entity.VolumeData
import com.lago.app.domain.entity.ChartStockInfo
import com.lago.app.domain.entity.MACDResult
import com.lago.app.domain.entity.BollingerBandsResult
import com.lago.app.domain.entity.PatternAnalysisResult
import com.lago.app.data.remote.dto.PatternAnalysisResponse
import com.lago.app.domain.entity.SignalSource
// ViewModel imports
import com.lago.app.presentation.viewmodel.chart.ChartUiEvent
import com.lago.app.presentation.viewmodel.chart.HoldingItem
import com.lago.app.presentation.viewmodel.chart.TradingItem
import com.lago.app.presentation.viewmodel.chart.ChartLoadingStage
import com.skydoves.flexible.core.pxToDp
// Chart imports - v5 Multi-Panel Chart
import com.lago.app.presentation.ui.chart.v5.MultiPanelChart
import com.lago.app.presentation.ui.chart.v5.DataConverter
import com.lago.app.presentation.ui.chart.v5.toEnabledIndicators
import com.lago.app.presentation.ui.chart.v5.MinuteAggregator
import com.lago.app.presentation.ui.chart.v5.Tick
// Import ChartScreen 패턴분석 컴포넌트들
import com.lago.app.presentation.ui.chart.PatternAnalysisWithResults
import com.lago.app.presentation.ui.chart.PatternAnalysisEmpty
import com.lago.app.presentation.ui.chart.PatternAnalysisLoading
import com.lago.app.presentation.ui.chart.PatternAnalysisError
import kotlin.math.absoluteValue
import java.time.LocalDate
import java.time.ZoneOffset


/**
 * 일봉 문자열을 UTC 자정 기준 epoch seconds로 변환
 * 예: "2020-07-08" → 1594166400L
 */
private fun String.toEpochSecUtcMidnight(): Long =
    LocalDate.parse(this).atStartOfDay(ZoneOffset.UTC).toEpochSecond()

/**
 * HistoryChallengeChartScreen - 역사 챌린지 전용 차트 화면
 * ChartScreen과 동일하지만 AI 버튼이 제거됨
 */

// 바텀시트 상태 열거형
enum class BottomSheetState {
    COLLAPSED,     // 하단 (200dp)
    HALF_EXPANDED, // 중단 (45%)
    EXPANDED       // 상단 (75%)
}

// Screen configuration for different device types
data class ScreenConfig(
    val collapsedHeightRatio: Float,
    val halfExpandedHeightRatio: Float,
    val expandedHeightRatio: Float,
    val buttonBarHeight: Dp,
    val chartBaseHeightRatio: Float,
    val minChartHeight: Dp,
    val headerHeight: Dp,
    val timeButtonHeight: Dp
)

// Safe zones to prevent UI overlap
data class SafeZones(
    val top: Dp,
    val bottom: Dp,
    val chartMin: Dp,
    val bottomSheetMin: Dp
)


@Composable
fun HistoryChallengeChartScreen(
    stockCode: String? = null,
    viewModel: HistoryChallengeChartViewModel = hiltViewModel(),
    onNavigateToStockPurchase: (String, String) -> Unit = { _, _ -> },
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    val screenWidth = configuration.screenWidthDp.dp
    val coroutineScope = rememberCoroutineScope()

    // 로딩 진행도 상태
    var loadingProgress by remember { mutableStateOf(0) }

    // Device classification
    val isCompactScreen = screenWidth < 400.dp || screenHeight < 700.dp
    val isLargeScreen = screenWidth > 600.dp
    val isTablet = screenWidth > 700.dp && screenHeight > 900.dp

    // Screen configuration based on device type - 네비게이션 바 없어서 차트 영역 증가
    val screenConfig = when {
        isCompactScreen -> ScreenConfig(
            collapsedHeightRatio = 0.20f,  // 네비게이션 바 공간만큼 줄임 (더 많은 차트 영역)
            halfExpandedHeightRatio = 0.45f,
            expandedHeightRatio = 0.85f,  // 더 높게 올라감
            buttonBarHeight = 68.dp,
            chartBaseHeightRatio = 0.38f,  // 차트 영역 증가
            minChartHeight = 200.dp,  // 최소 높이도 증가
            headerHeight = 72.dp,
            timeButtonHeight = 44.dp
        )
        isTablet -> ScreenConfig(
            collapsedHeightRatio = 0.18f,  // 네비게이션 바 공간만큼 줄임
            halfExpandedHeightRatio = 0.50f,
            expandedHeightRatio = 0.90f,  // 더 높게 올라감
            buttonBarHeight = 80.dp,
            chartBaseHeightRatio = 0.48f,  // 차트 영역 증가
            minChartHeight = 280.dp,  // 최소 높이도 증가
            headerHeight = 90.dp,
            timeButtonHeight = 56.dp
        )
        isLargeScreen -> ScreenConfig(
            collapsedHeightRatio = 0.19f,  // 네비게이션 바 공간만큼 줄임
            halfExpandedHeightRatio = 0.48f,
            expandedHeightRatio = 0.88f,  // 더 높게 올라감
            buttonBarHeight = 76.dp,
            chartBaseHeightRatio = 0.43f,  // 차트 영역 증가
            minChartHeight = 250.dp,  // 최소 높이도 증가
            headerHeight = 84.dp,
            timeButtonHeight = 52.dp
        )
        else -> ScreenConfig( // Standard devices (S23 등)
            collapsedHeightRatio = 0.20f,  // 네비게이션 바 공간만큼 줄임 (더 많은 차트 영역)
            halfExpandedHeightRatio = 0.47f,
            expandedHeightRatio = 0.87f,  // 더 높게 올라감
            buttonBarHeight = 72.dp,
            chartBaseHeightRatio = 0.40f,  // 차트 영역 증가
            minChartHeight = 220.dp,  // 최소 높이도 증가
            headerHeight = 80.dp,
            timeButtonHeight = 48.dp
        )
    }

    // 시스템 바 높이 계산 (먼저 정의)
    val systemNavBarHeight = with(density) {
        WindowInsets.navigationBars.getBottom(density).toDp()
    }
    val statusBarHeight = with(density) {
        WindowInsets.statusBars.getTop(density).toDp()
    }

    // Safe zones - 앱바는 원래 위치 유지
    val safeZones = SafeZones(
        top = 60.dp, // AppBar height only (상태표시줄 제외)
        bottom = screenConfig.buttonBarHeight,
        chartMin = screenConfig.minChartHeight,
        bottomSheetMin = if (isCompactScreen) 140.dp else 160.dp
    )

    // 투자 탭에서 선택된 주식 코드로 차트 데이터 로드
    LaunchedEffect(stockCode) {
        stockCode?.let { code ->
            viewModel.onEvent(ChartUiEvent.ChangeStock(code))
        }
    }

    // 3단계 높이 정의 - 반응형
    val collapsedHeight = screenHeight * screenConfig.collapsedHeightRatio
    val halfExpandedHeight = screenHeight * screenConfig.halfExpandedHeightRatio
    val expandedHeight = screenHeight * screenConfig.expandedHeightRatio
    val buttonBarHeight = screenConfig.buttonBarHeight

    // 바텀시트 상태와 현재 오프셋
    var bottomSheetState by rememberSaveable { mutableStateOf(BottomSheetState.COLLAPSED) }
    var isDragging by remember { mutableStateOf(false) }

    // Y 위치를 픽셀로 계산
    val screenHeightPx = with(density) { screenHeight.toPx() }
    val buttonBarHeightPx = with(density) { buttonBarHeight.toPx() }

    // Dynamic chart height calculation
    fun calculateDynamicChartHeight(bottomSheetProgress: Float): Dp {
        val baseHeight = screenHeight * screenConfig.chartBaseHeightRatio
        val maxCompression = baseHeight - screenConfig.minChartHeight
        val compressionFactor = bottomSheetProgress * 0.5f // 50% max compression
        return (baseHeight - (maxCompression * compressionFactor)).coerceAtLeast(screenConfig.minChartHeight)
    }

    // 각 상태별 Y 위치
    val sheetPositions = remember(screenHeightPx, density, isCompactScreen, systemNavBarHeight, statusBarHeight) {
        object {
            val tabHeight = if (isCompactScreen) 40.dp else 44.dp

            // 정확한 구매/판매 버튼 박스 높이 계산
            val buttonHeight = if (isCompactScreen) 44.dp else 48.dp
            val verticalPadding = if (isCompactScreen) 8.dp else 12.dp // Spacing.sm, Spacing.sm+xs
            val actualBuyButtonBoxHeight = buttonHeight + (verticalPadding * 2)

            // 드래그 핸들 영역 높이 (padding + handle)
            val dragHandleAreaHeight = 12.dp + 8.dp + 4.dp // top + bottom + handle = 24dp

            // Tab indicator 높이 (선택된 탭 밑줄) + 여유공간
            val tabIndicatorHeight = 2.dp + 2.dp // indicator + 추가 여유공간

            // collapsed: 바텀시트 상단이 구매/판매 버튼 바로 위에 위치하도록 (시스템 네비게이션 바 위)
            // 상태표시줄 패딩만큼 위로 올려줌
            val collapsed = screenHeightPx - with(density) {
                (statusBarHeight + systemNavBarHeight + actualBuyButtonBoxHeight + tabHeight + tabIndicatorHeight + dragHandleAreaHeight).toPx()
            }
            val halfExpanded = screenHeightPx - with(density) { (halfExpandedHeight + statusBarHeight).toPx() } - buttonBarHeightPx
            // expanded: 앱바 바로 아래에 완전히 붙이기
            // shadow(8dp)와 rounded corner 영향을 고려하여 추가로 올림
            val expanded = with(density) { (statusBarHeight + 60.dp - dragHandleAreaHeight - 8.dp).toPx() }
        }
    }

    // 현재 목표 Y 위치
    val targetY = remember(bottomSheetState, sheetPositions) {
        when (bottomSheetState) {
            BottomSheetState.COLLAPSED -> sheetPositions.collapsed
            BottomSheetState.HALF_EXPANDED -> sheetPositions.halfExpanded
            BottomSheetState.EXPANDED -> sheetPositions.expanded
        }
    }

    // Animatable로 부드러운 애니메이션 처리
    val sheetAnimY = remember { Animatable(sheetPositions.collapsed) }

    // 바텀시트 상태가 변경될 때 애니메이션
    LaunchedEffect(bottomSheetState) {
        sheetAnimY.animateTo(
            targetValue = targetY,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // 진행도 계산 (0.0 = 하단, 1.0 = 상단)
    val sheetProgress by remember(sheetPositions) {
        derivedStateOf {
            val progress = (sheetPositions.collapsed - sheetAnimY.value) / (sheetPositions.collapsed - sheetPositions.expanded)
            progress.coerceIn(0f, 1f)
        }
    }

    // 헤더 정렬 진행도 계산 - 중단까지만 애니메이션, 그 이후는 고정
    val headerAlignmentProgress by remember(sheetPositions) {
        derivedStateOf {
            val halfProgress = (sheetPositions.collapsed - sheetAnimY.value) / (sheetPositions.collapsed - sheetPositions.halfExpanded)
            halfProgress.coerceIn(0f, 1f)  // 0(하단) ~ 1(중단 이상)
        }
    }

    // 바텀시트 위치에 따른 콘텐츠 오프셋 (헤더용 - 원래대로)
    val contentOffsetY = with(density) {
        val maxOffset = -40.dp.toPx()
        val currentOffset = (sheetAnimY.value - sheetPositions.collapsed).coerceAtMost(0f)
        (currentOffset * 0.3f).coerceAtLeast(maxOffset).toDp()
    }

    // 시간버튼만 올라가도록 오프셋 설정
    val timeButtonOffsetY = with(density) {
        val currentOffset = (sheetAnimY.value - sheetPositions.collapsed).coerceAtMost(0f)
        currentOffset.toDp()
    }

    // 드래그 제스처 처리 함수
    fun handleDragEnd(velocity: Float, currentPosition: Float) {
        val fastSwipeThreshold = 800f

        when {
            // 빠른 아래 스와이프
            velocity > fastSwipeThreshold -> {
                bottomSheetState = when (bottomSheetState) {
                    BottomSheetState.EXPANDED -> BottomSheetState.HALF_EXPANDED
                    BottomSheetState.HALF_EXPANDED -> BottomSheetState.COLLAPSED
                    BottomSheetState.COLLAPSED -> BottomSheetState.COLLAPSED
                }
            }
            // 빠른 위 스와이프
            velocity < -fastSwipeThreshold -> {
                bottomSheetState = when (bottomSheetState) {
                    BottomSheetState.COLLAPSED -> BottomSheetState.HALF_EXPANDED
                    BottomSheetState.HALF_EXPANDED -> BottomSheetState.EXPANDED
                    BottomSheetState.EXPANDED -> BottomSheetState.EXPANDED
                }
            }
            // 느린 드래그 - 가장 가까운 상태로 스냅
            else -> {
                val distances = listOf(
                    BottomSheetState.COLLAPSED to abs(currentPosition - sheetPositions.collapsed),
                    BottomSheetState.HALF_EXPANDED to abs(currentPosition - sheetPositions.halfExpanded),
                    BottomSheetState.EXPANDED to abs(currentPosition - sheetPositions.expanded)
                )

                bottomSheetState = distances.minByOrNull { it.second }?.first ?: BottomSheetState.COLLAPSED
            }
        }

        isDragging = false
    }

    // LazyListState들을 상위에서 관리
    val holdingsListState = rememberLazyListState()
    val tradingHistoryListState = rememberLazyListState()

    // NestedScrollConnection
    val nestedScrollConnection = remember(bottomSheetState, sheetAnimY, coroutineScope) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (bottomSheetState == BottomSheetState.COLLAPSED && available.y < 0) {
                    coroutineScope.launch {
                        val newY = (sheetAnimY.value + available.y).coerceIn(
                            sheetPositions.expanded,
                            sheetPositions.collapsed
                        )
                        sheetAnimY.snapTo(newY)
                    }
                    return available
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val currentListState = when (uiState.selectedBottomTab) {
                    0 -> holdingsListState
                    1 -> tradingHistoryListState
                    else -> null
                }

                val isAtTop = currentListState?.firstVisibleItemIndex == 0 &&
                        currentListState.firstVisibleItemScrollOffset == 0

                if (isAtTop && available.y > 0 && consumed.y == 0f && bottomSheetState == BottomSheetState.EXPANDED) {
                    coroutineScope.launch {
                        val newY = (sheetAnimY.value + available.y).coerceIn(
                            sheetPositions.expanded,
                            sheetPositions.collapsed
                        )
                        sheetAnimY.snapTo(newY)

                        if (newY > sheetPositions.expanded + 100) {
                            bottomSheetState = BottomSheetState.HALF_EXPANDED
                        }
                    }
                    return available
                }
                return Offset.Zero
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 메인 콘텐츠 (앱바 아래까지만 올라감) - 바텀시트 높이에 따른 패딩 조정
        // 시간 버튼이 바텀시트로 이동했으므로 기본 패딩만 사용

        // 1. 배경 영역
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)  // 흰색 배경
        )

        // 차트 + 시간버튼 영역 - 바텀시트 실시간 위치에 따른 높이 계산
        val columnHeight = with(density) {
            // 현재 바텀시트의 상단 위치 (실시간)
            val currentBottomSheetTop = sheetAnimY.value.toDp()
            val halfExpandedTop = sheetPositions.halfExpanded.toDp()

            // 중단 위치보다 위에 있으면 중단 높이로 고정
            if (currentBottomSheetTop <= halfExpandedTop) {
                // 중단 이상: 중단 위치에서 고정
                (halfExpandedTop - safeZones.top).coerceAtLeast(200.dp)
            } else {
                // 하단->중단 사이: 실시간 바텀시트 위치에 따라 동적
                (currentBottomSheetTop - safeZones.top).coerceAtLeast(200.dp)
            }
        }

        // Column을 항상 앱바 아래에 고정 (방법 2)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .height(columnHeight)  // 바텀시트 위치에 따른 높이
                .offset(y = safeZones.top)  // 항상 앱바(60dp) 아래에 고정
        ) {

            // 헤더 영역 - 타이틀 공간 확보 (AnimatedHeaderBox와 동일한 높이)
            Spacer(modifier = Modifier.height(
                with(density) {
                    // AnimatedHeaderBox의 실제 높이를 고려
                    val baseHeight = 120.dp  // 타이틀이 완전히 보이도록 충분한 공간
                    val minHeight = 0.dp     // 중단 상태에서 완전히 사라짐
                    // headerAlignmentProgress 사용 (중단까지만 압축)
                    val compression = (baseHeight - minHeight) * headerAlignmentProgress
                    (baseHeight - compression)
                }
            ))

            // 차트 영역 - weight(1f)로 남은 공간 모두 차지
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)  // 자동으로 압축/확장
            ) {
                // TradingView v5 Multi-Panel Chart with Native API (기존 방식 유지)
                val multiPanelData = DataConverter.createMultiPanelData(
                    candlestickData = uiState.candlestickData,
                    volumeData = uiState.volumeData,
                    sma5Data = uiState.sma5Data,
                    sma20Data = uiState.sma20Data,
                    rsiData = uiState.rsiData,
                    macdData = uiState.macdData,
                    bollingerBands = uiState.bollingerBands,
                    enabledIndicators = uiState.config.indicators.toEnabledIndicators(),
                    timeFrame = uiState.config.timeFrame
                )

                // 차트를 완전한 "상태 섬"으로 고립 - 외부 상태와 완전 분리
                StableChartComponent(
                    viewModel = viewModel,
                    onLoadingProgress = { progress ->
                        // 🔥 순차적 로딩 진행도 콜백
                        loadingProgress = progress
                        android.util.Log.d("HistoryChallengeChartScreen", "📊 Loading Progress: $progress%")
                        
                        // 100% 완료 시 최종 확인
                        if (progress >= 100) {
                            android.util.Log.d("HistoryChallengeChartScreen", "🎉 역사챌린지 차트 로딩 완전 완료!")
                            viewModel.onChartLoadingCompleted()
                        }
                    }
                )
            }

            // 차트와 시간버튼 사이 간격 최소화
            Spacer(modifier = Modifier.height(Spacing.sm))

            // 차트와 시간버튼 사이 간격
            Spacer(modifier = Modifier.height(8.dp))

            // 시간버튼 영역
            TimeFrameSelection(
                selectedTimeFrame = uiState.config.timeFrame,
                onTimeFrameChange = { viewModel.onEvent(ChartUiEvent.ChangeTimeFrame(it)) },
                isCompact = isCompactScreen,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(screenConfig.timeButtonHeight)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(
                        horizontal = if (isCompactScreen) Spacing.sm else Spacing.md,
                        vertical = Spacing.xs
                    )
            )

            // Column 높이가 바텀시트 상단까지 정확히 계산되므로 추가 간격 불필요

        }

        // 2. 앱바 (중간 레이어) - AI 버튼 제거, 원래 위치 유지
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .background(MaterialTheme.colorScheme.surface)
                .zIndex(1f)
        ) {
            TopAppBar(
                onBackClick = {
                    viewModel.onEvent(ChartUiEvent.BackPressed)
                    onNavigateBack()
                },
                isFavorite = uiState.isFavorite,
                onFavoriteClick = { viewModel.onEvent(ChartUiEvent.ToggleFavorite) },
                stockInfo = uiState.currentStock,
                onSettingsClick = { viewModel.onEvent(ChartUiEvent.ToggleIndicatorSettings) },
                modifier = Modifier.fillMaxSize()
            )
        }

        // 3. 애니메이션되는 헤더 박스 (앱바 위로 올라감)
        AnimatedHeaderBox(
            stockInfo = uiState.currentStock,
            headerAlignmentProgress = headerAlignmentProgress,
            contentOffsetY = contentOffsetY.value
        )

        // 4. 커스텀 3단계 바텀시트
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = with(density) { sheetAnimY.value.toDp() })
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                    spotColor = Color.Black.copy(alpha = 0.1f)
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .pointerInput(Unit) {
                    var totalDragAmount = 0f
                    var dragStartTime = 0L

                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            totalDragAmount = 0f
                            dragStartTime = System.currentTimeMillis()
                        },
                        onDragEnd = {
                            val duration = System.currentTimeMillis() - dragStartTime
                            val velocity = if (duration > 0) totalDragAmount / duration * 1000 else 0f
                            handleDragEnd(velocity, sheetAnimY.value)
                        }
                    ) { _, dragAmount ->
                        totalDragAmount += dragAmount.y

                        // 드래그 중 실시간 업데이트
                        coroutineScope.launch {
                            val newY = (sheetAnimY.value + dragAmount.y).coerceIn(
                                sheetPositions.expanded,
                                sheetPositions.collapsed
                            )
                            sheetAnimY.snapTo(newY)
                        }
                    }
                }
        ) {
            Column {
                // 드래그 핸들
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                color = Gray300,
                                shape = RoundedCornerShape(2.dp)
                            )
                            .semantics {
                                contentDescription = "바텀시트 드래그 핸들"
                            }
                    )
                }

                // 동적 높이 바텀시트 콘텐츠 - 반응형
                BottomSheetContent(
                    viewModel = viewModel,
                    nestedScrollConnection = nestedScrollConnection,
                    holdingsListState = holdingsListState,
                    tradingHistoryListState = tradingHistoryListState,
                    bottomSheetState = bottomSheetState,
                    isCompact = isCompactScreen,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(
                            with(density) {
                                val currentProgress = (sheetPositions.collapsed - sheetAnimY.value) / (sheetPositions.collapsed - sheetPositions.expanded)
                                val progress = currentProgress.coerceIn(0f, 1f)

                                // 반응형 최소/최대 높이
                                val minHeight = safeZones.bottomSheetMin
                                val maxHeight = if (isCompactScreen) {
                                    screenHeight * 0.65f // 컴팩트 기기에서는 더 적게
                                } else if (isTablet) {
                                    screenHeight * 0.75f // 태블릿에서는 더 크게
                                } else {
                                    screenHeight * 0.70f // 표준 기기
                                }
                                minHeight + (maxHeight - minHeight) * progress
                            }
                        )
                )
            }
        }

        // 5. 화면 하단 고정 매수/매도 버튼 (시스템 네비게이션 바 위)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)  // 시스템 네비게이션 바 패딩
                .shadow(
                    elevation = 16.dp,  // 바텀시트보다 높은 elevation
                    spotColor = Color(0x1A000000)
                )
                .background(Color.White)
                .zIndex(10f)  // 바텀시트보다 위에 표시
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (isCompactScreen) Spacing.sm else Spacing.md,
                        vertical = if (isCompactScreen) Spacing.sm else (Spacing.sm + Spacing.xs)
                    ),
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm + Spacing.xs)
            ) {
                Button(
                    onClick = {
                        viewModel.onEvent(ChartUiEvent.SellClicked)
                        onNavigateToStockPurchase(uiState.currentStock.code, "sell")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isCompactScreen) 44.dp else 48.dp)
                        .semantics {
                            contentDescription = "${uiState.currentStock.name} 판매하기 버튼"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "판매하기",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = TitleB16
                    )
                }

                Button(
                    onClick = {
                        viewModel.onEvent(ChartUiEvent.BuyClicked)
                        onNavigateToStockPurchase(uiState.currentStock.code, "buy")
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(if (isCompactScreen) 44.dp else 48.dp)
                        .semantics {
                            contentDescription = "${uiState.currentStock.name} 구매하기 버튼"
                        },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        "구매하기",
                        color = MaterialTheme.colorScheme.onSecondary,
                        style = TitleB16
                    )
                }
            }
        }

        // 지표 설정 다이얼로그
        if (uiState.showIndicatorSettings) {
            IndicatorSettingsDialog(
                config = uiState.config,
                showUserTradingSignals = uiState.showUserTradingSignals,
                onIndicatorToggle = { indicatorType, enabled ->
                    viewModel.onEvent(ChartUiEvent.ToggleIndicator(indicatorType, enabled))
                },
                onTradingSignalsToggle = { enabled ->
                    viewModel.onEvent(ChartUiEvent.ToggleUserTradingSignals(enabled))
                },
                onDismiss = {
                    viewModel.onEvent(ChartUiEvent.HideIndicatorSettings)
                }
            )
        }

        // 로딩 인디케이터 (텍스트 없이)
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFFFF69B4))
            }
        }
    }
}

// AI 버튼이 제거된 TopAppBar
@Composable
private fun TopAppBar(
    onBackClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    stockInfo: ChartStockInfo,
    onSettingsClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(start = Spacing.xs + 3.dp)
                .semantics {
                    contentDescription = "이전 화면으로 돌아가기"
                }
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier.semantics {
                contentDescription = if (isFavorite) "관심종목에서 제거" else "관심종목에 추가"
            }
        ) {
            Icon(
                painter = if (isFavorite) painterResource(R.drawable.pink_heart) else painterResource(R.drawable.blank_heart),
                contentDescription = null,
                tint = if (isFavorite) Color.Unspecified else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        // AI 버튼 제거됨 - 역사 챌린지에서는 AI 기능 없음

        IconButton(
            onClick = onSettingsClick,
            modifier = Modifier.semantics {
                contentDescription = "지표 설정"
            }
        ) {
            Icon(
                painter = painterResource(R.drawable.chart_setting),
                contentDescription = null,
                tint = Gray900,
                modifier = Modifier.size(24.dp)
            )
        }

    }
}

@Composable
fun TimeFrameSelection(
    selectedTimeFrame: String,
    onTimeFrameChange: (String) -> Unit,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    var showMinuteDropdown by remember { mutableStateOf(false) }

    val timeFrames = listOf(
        "D" to "일",
        "W" to "주",
        "M" to "월",
        "Y" to "년"
    )

    val minuteFrames = listOf(
        "1" to "1분",
        "3" to "3분",
        "5" to "5분",
        "10" to "10분",
        "15" to "15분",
        "30" to "30분",
        "60" to "60분"
    )

    val selectedMinute = minuteFrames.find { it.first == selectedTimeFrame }
    val isMinuteSelected = selectedMinute != null

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center
    ) {
        // 분봉 드롭다운 버튼
        Box {
            FilterChip(
                onClick = { showMinuteDropdown = !showMinuteDropdown },
                modifier = Modifier
                    .semantics {
                        contentDescription = "분봉 선택: 현재 ${selectedMinute?.second ?: "10분"}"
                    }
                    .padding(horizontal = 6.dp)
                    .height(if (isCompact) 32.dp else 36.dp),
                shape = RoundedCornerShape(8.dp),
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            selectedMinute?.second ?: "10분",
                            style = if (isCompact) BodyR12 else SubtitleSb14,
                            color = if (isMinuteSelected) Color.White else Gray900
                        )
                        Icon(
                            imageVector = if (showMinuteDropdown) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = "분봉 선택",
                            tint = if (isMinuteSelected) Color.White else Gray900,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                selected = isMinuteSelected,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MainPink,
                    selectedLabelColor = Color.White,
                    containerColor = Color.Transparent,
                    labelColor = Gray900
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isMinuteSelected,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent
                ),
            )

            DropdownMenu(
                expanded = showMinuteDropdown,
                onDismissRequest = { showMinuteDropdown = false },
                modifier = Modifier
                    .background(Color.White)
                    .width(120.dp)
            ) {
                minuteFrames.forEach { (code, name) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                name,
                                style = BodyR14,
                                color = Gray900
                            )
                        },
                        onClick = {
                            onTimeFrameChange(code)
                            showMinuteDropdown = false
                        },
                        modifier = Modifier.height(40.dp)
                    )
                }
            }
        }

        // 나머지 시간대 버튼들
        timeFrames.forEach { (code, name) ->
            FilterChip(
                onClick = { onTimeFrameChange(code) },
                modifier = Modifier
                    .semantics {
                        contentDescription = "시간대 선택: ${name}${if (selectedTimeFrame == code) ", 선택됨" else ""}"
                    }
                    .padding(horizontal = 6.dp)
                    .height(if (isCompact) 32.dp else 36.dp),
                shape = RoundedCornerShape(8.dp),
                label = {
                    Text(
                        name,
                        style = if (isCompact) BodyR12 else SubtitleSb14,
                        color = if (selectedTimeFrame == code) Color.White else Gray900
                    )
                },
                selected = selectedTimeFrame == code,
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MainPink,
                    selectedLabelColor = Color.White,
                    containerColor = Color.Transparent,
                    labelColor = Gray900
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selectedTimeFrame == code,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent
                ),

                )
        }
    }
}

@Composable
private fun BottomSheetContent(
    viewModel: HistoryChallengeChartViewModel,
    nestedScrollConnection: NestedScrollConnection,
    holdingsListState: LazyListState,
    tradingHistoryListState: LazyListState,
    bottomSheetState: BottomSheetState,
    isCompact: Boolean = false,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedTabIndex = uiState.selectedBottomTab
    // 역사 챌린지에서도 패턴분석 탭 포함 (모의투자와 횟수 공유)
    val tabTitles = listOf("보유현황", "매매내역", "패턴분석")

    Column(modifier = modifier) {
        // 탭
        TabRow(
            selectedTabIndex = selectedTabIndex.coerceAtMost(tabTitles.size - 1),
            containerColor = Color.White,
            contentColor = Color(0xFF08090E),
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex.coerceAtMost(tabTitles.size - 1)]),
                    color = Color.Black,
                    height = 2.dp
                )
            }
        ) {
            tabTitles.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = {
                        viewModel.onEvent(ChartUiEvent.ChangeBottomTab(index))
                    },
                    text = {
                        Text(
                            title,
                            style = if (isCompact) SubtitleSb14 else TitleB16,
                            color = if (selectedTabIndex == index) Gray900 else Gray600
                        )
                    },
                    modifier = Modifier.height(if (isCompact) 40.dp else 44.dp)
                )
            }
        }

        // 탭 내용
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(
                    horizontal = if (isCompact) Spacing.md else 27.dp,
                    vertical = if (isCompact) Spacing.sm else 16.dp
                )
                .padding(
                    bottom = when (bottomSheetState) {
                        BottomSheetState.EXPANDED -> 0.dp // 전체 화면일 때는 패딩 없음
                        else -> 40.dp // COLLAPSED, HALF_EXPANDED일 때만 패딩
                    }
                )
        ) {
            when (selectedTabIndex.coerceAtMost(tabTitles.size - 1)) {
                0 -> HoldingsContent(
                    holdings = uiState.holdingItems,
                    listState = holdingsListState,
                    nestedScrollConnection = nestedScrollConnection,
                    bottomSheetState = bottomSheetState
                )
                1 -> TradingHistoryContent(
                    history = uiState.tradingHistory,
                    currentStockCode = uiState.currentStock.code,
                    listState = tradingHistoryListState,
                    nestedScrollConnection = nestedScrollConnection,
                    bottomSheetState = bottomSheetState
                )
                2 -> PatternAnalysisContent(
                    patternAnalysisCount = uiState.patternAnalysisCount,
                    maxPatternAnalysisCount = uiState.maxPatternAnalysisCount,
                    lastPatternAnalysis = uiState.patternAnalysis,
                    isPatternAnalyzing = uiState.isPatternAnalyzing,
                    patternAnalysisError = uiState.patternAnalysisError,
                    onAnalyzeClick = { viewModel.onEvent(ChartUiEvent.AnalyzePattern) }
                )
            }
        }
    }
}

@Composable
private fun HoldingsContent(
    holdings: List<HoldingItem>,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    bottomSheetState: BottomSheetState
) {
    if (holdings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "보유 주식이 없어요!",
                fontSize = 14.sp,
                color = Gray600
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = bottomSheetState != BottomSheetState.COLLAPSED
        ) {
            items(holdings) { holding ->
                HoldingItemRow(holding)
            }
        }
    }
}

@Composable
private fun HoldingItemRow(item: HoldingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 종목별 컬러 아이콘
            val stockColor = when (item.name) {
                "삼성전자" -> Color(0xFF1428A0)
                "GS리테일" -> Color(0xFF00A651)
                "한화생명" -> Color(0xFFE8501A)
                "LG전자" -> Color(0xFFA50034)
                "하이트진로맥주" -> Color(0xFFED1C24)
                else -> Color(0xFF666666)
            }

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(stockColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (item.name) {
                        "삼성전자" -> "삼성"
                        "GS리테일" -> "GS"
                        "한화생명" -> "한화"
                        "LG전자" -> "LG"
                        "하이트진로맥주" -> "진로"
                        else -> "종목"
                    },
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = item.name,
                    style = TitleB20,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.quantity} · 주당 ${String.format("%,d", item.value / (item.quantity.replace("주", "").trim().toIntOrNull() ?: 1))}원",
                    style = BodyR12,
                    color = Gray500
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${String.format("%,d", item.value)}원",
                style = TitleB18,
                color = MaterialTheme.colorScheme.onSurface
            )

            val isPositive = item.change >= 0
            val changeAmount = (item.value * item.change / 100).toInt()
            val changeText = if (isPositive) {
                "+${String.format("%,d", changeAmount)}원 (${String.format("%.2f", item.change)}%)"
            } else {
                "${String.format("%,d", changeAmount)}원 (${String.format("%.2f", item.change)}%)"
            }

            Text(
                text = changeText,
                style = BodyR14,
                color = if (isPositive) MainPink else MainBlue
            )
        }
    }
}

@Composable
private fun TradingHistoryContent(
    history: List<TradingItem>,
    currentStockCode: String,
    listState: LazyListState,
    nestedScrollConnection: NestedScrollConnection,
    bottomSheetState: BottomSheetState
) {
    // 현재 주식 코드와 일치하는 매매내역만 필터링
    val filteredHistory = history.filter { tradingItem ->
        tradingItem.stockCode == currentStockCode
    }

    if (filteredHistory.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "매매 내역이 없어요",
                fontSize = 14.sp,
                color = Gray600
            )
        }
    } else {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = bottomSheetState != BottomSheetState.COLLAPSED
        ) {
            items(filteredHistory) { tradingItem ->
                TradingItemRow(tradingItem)
            }
        }
    }
}

@Composable
private fun TradingItemRow(item: TradingItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // 구매/판매 구분 아이콘
            val isBuy = item.type == "구매"
            val iconColor = if (isBuy) MainPink else MainBlue
            val iconText = if (isBuy) "구매" else "판매"

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = iconText,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                Text(
                    text = item.quantity,
                    style = TitleB20,
                    color = Gray900
                )
                Text(
                    text = item.date,
                    style = BodyR12,
                    color = Gray500
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${String.format("%,d", item.amount)}원",
                style = TitleB18,
                color = Gray900
            )
            val quantityNumber = item.quantity.replace("주", "").trim().toIntOrNull() ?: 1
            Text(
                text = "주당 ${String.format("%,d", item.amount / quantityNumber)}원",
                style = BodyR12,
                color = Gray500
            )
        }
    }
}

@Composable
private fun PatternAnalysisContent(
    patternAnalysisCount: Int,
    maxPatternAnalysisCount: Int,
    lastPatternAnalysis: PatternAnalysisResult?,
    isPatternAnalyzing: Boolean,
    patternAnalysisError: String?,
    onAnalyzeClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(24.dp)
    ) {
        when {
            isPatternAnalyzing -> {
                // 로딩 상태
                PatternAnalysisLoading()
            }
            patternAnalysisError != null -> {
                // 에러 상태
                PatternAnalysisError(
                    error = patternAnalysisError,
                    onRetryClick = onAnalyzeClick
                )
            }
            lastPatternAnalysis != null -> {
                // 분석 결과가 있을 때
                PatternAnalysisWithResults(
                    patternAnalysis = lastPatternAnalysis,
                    patternAnalysisCount = patternAnalysisCount,
                    maxPatternAnalysisCount = maxPatternAnalysisCount,
                    onAnalyzeClick = onAnalyzeClick
                )
            }
            else -> {
                // 분석 결과가 없을 때 (한 번도 분석 안함)
                PatternAnalysisEmpty(
                    patternAnalysisCount = patternAnalysisCount,
                    maxPatternAnalysisCount = maxPatternAnalysisCount,
                    onAnalyzeClick = onAnalyzeClick
                )
            }
        }
    }
}

@Composable
private fun PatternAnalysisWithResults(
    patternAnalysis: PatternAnalysisResult,
    patternAnalysisCount: Int,
    maxPatternAnalysisCount: Int,
    onAnalyzeClick: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            // 헤더 텍스트
            Text(
                text = "발견된 패턴",
                style = TitleB24,
                color = Gray900,
                modifier = Modifier.padding(bottom = 32.dp)
            )
        }

        // 패턴 목록
        itemsIndexed(patternAnalysis.patterns) { index, pattern ->
            PatternResultItem(
                pattern = pattern,
                isLastItem = index == patternAnalysis.patterns.size - 1
            )

            if (index < patternAnalysis.patterns.size - 1) {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(40.dp))
            // 다시 분석하기 버튼
            Button(
                onClick = onAnalyzeClick,
                enabled = patternAnalysisCount < maxPatternAnalysisCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueLightHover,
                    disabledContainerColor = Gray300
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "다시 분석하기 ($patternAnalysisCount/$maxPatternAnalysisCount)",
                    style = TitleB16,
                    color = BlueNormalHover
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PatternAnalysisEmpty(
    patternAnalysisCount: Int,
    maxPatternAnalysisCount: Int,
    onAnalyzeClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 빈 상태 텍스트
        Text(
            text = "아직 분석한 패턴이 없어요",
            style = TitleB20,
            color = Gray700,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "차트 패턴을 분석해보세요!",
            style = BodyR16,
            color = Gray600,
            modifier = Modifier.padding(bottom = 40.dp)
        )

        // 분석하기 버튼
        Button(
            onClick = onAnalyzeClick,
            enabled = patternAnalysisCount < maxPatternAnalysisCount,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BlueLightHover,
                disabledContainerColor = Gray300
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "패턴 분석하기 ($patternAnalysisCount/$maxPatternAnalysisCount)",
                style = TitleB16,
                color = BlueNormalHover
            )
        }
    }
}

@Composable
private fun PatternResultItem(
    pattern: PatternAnalysisResponse,
    isLastItem: Boolean = false
) {
    Column {
        // 패턴 타이틀 (아이콘 + 제목)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.glowing_blue),
                contentDescription = null,
                tint = Color.Unspecified, // 원본 색상 유지 (그라데이션)
                modifier = Modifier
                    .size(20.dp) // 24sp 타이틀의 절반 정도로 크게 조정
                    .padding(end = 12.dp)
            )

            Text(
                text = pattern.name,
                style = TitleB24,
                color = Gray900
            )
        }

        // 패턴 설명 (아이콘 + 간격만큼 들여쓰기)
        Text(
            text = pattern.reason,
            style = BodyR20,
            color = Gray700,
            lineHeight = 28.sp,
            modifier = Modifier
                .padding(start = 32.dp, bottom = 24.dp) // 20dp(아이콘) + 12dp(간격) = 32dp 들여쓰기
        )

        // 구분선 (마지막 아이템이 아닐 때만 표시)
        if (!isLastItem) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Gray200)
            )
        }
    }
}

@Composable
private fun PatternAnalysisLoading() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            color = MainBlue,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "패턴을 분석하고 있습니다...",
            style = TitleB18,
            color = Gray700
        )
        Text(
            text = "잠시만 기다려주세요",
            style = BodyR14,
            color = Gray600,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun PatternAnalysisError(
    error: String,
    onRetryClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "분석 중 오류가 발생했습니다",
            style = TitleB20,
            color = Gray800,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = error,
            style = BodyR14,
            color = Gray600,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Button(
            onClick = onRetryClick,
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = BlueLightHover
            ),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "다시 시도",
                style = TitleB16,
                color = BlueNormalHover
            )
        }
    }
}

@Composable
private fun IndicatorSettingsDialog(
    config: ChartConfig,
    showUserTradingSignals: Boolean,
    onIndicatorToggle: (String, Boolean) -> Unit,
    onTradingSignalsToggle: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "지표 설정",
                style = TitleB18,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(Spacing.md)
            ) {
                // Volume 지표
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "거래량 (Volume)",
                        style = BodyR16,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = config.indicators.volume,
                        onCheckedChange = { enabled ->
                            onIndicatorToggle("volume", enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MainPink,
                            checkedTrackColor = MainPink.copy(alpha = 0.5f)
                        )
                    )
                }

                // RSI 지표
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RSI",
                        style = BodyR16,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = config.indicators.rsi,
                        onCheckedChange = { enabled ->
                            onIndicatorToggle("rsi", enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MainPink,
                            checkedTrackColor = MainPink.copy(alpha = 0.5f)
                        )
                    )
                }

                // MACD 지표
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MACD",
                        style = BodyR16,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = config.indicators.macd,
                        onCheckedChange = { enabled ->
                            onIndicatorToggle("macd", enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MainPink,
                            checkedTrackColor = MainPink.copy(alpha = 0.5f)
                        )
                    )
                }

                // SMA5 지표
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMA5",
                        style = BodyR16,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = config.indicators.sma5,
                        onCheckedChange = { enabled ->
                            onIndicatorToggle("sma5", enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MainPink,
                            checkedTrackColor = MainPink.copy(alpha = 0.5f)
                        )
                    )
                }

                // SMA20 지표
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SMA20",
                        style = BodyR16,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = config.indicators.sma20,
                        onCheckedChange = { enabled ->
                            onIndicatorToggle("sma20", enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MainPink,
                            checkedTrackColor = MainPink.copy(alpha = 0.5f)
                        )
                    )
                }

                // Bollinger Bands 지표
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "볼린저 밴드",
                        style = BodyR16,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = config.indicators.bollingerBands,
                        onCheckedChange = { enabled ->
                            onIndicatorToggle("bollingerBands", enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MainPink,
                            checkedTrackColor = MainPink.copy(alpha = 0.5f)
                        )
                    )
                }

                // 사용자 매매내역 표시
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "매매내역 표시",
                        style = BodyR16,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = showUserTradingSignals,
                        onCheckedChange = { enabled ->
                            onTradingSignalsToggle(enabled)
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MainPink,
                            checkedTrackColor = MainPink.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        },
        tonalElevation = 0.dp,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Radius.lg),
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "확인",
                    style = SubtitleSb14,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

/**
 * 완전 정적 차트 컴포넌트 - 절대 recomposition 되지 않음
 * 외부 데이터는 ViewModel을 통해 JavaScript로만 전달
 */
@Composable
private fun StableChartComponent(
    viewModel: HistoryChallengeChartViewModel,
    onLoadingProgress: (Int) -> Unit
) {
    // 완전 정적 차트 - 빈 데이터로 한 번만 생성
    MultiPanelChart(
        data = com.lago.app.presentation.ui.chart.v5.MultiPanelData(priceData = emptyList()),
        timeFrame = "1", // 1분봉으로 설정 (역사챌린지는 분단위 데이터)
        tradingSignals = emptyList(), // 완전 고정
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.md),
        onChartReady = {
            // ✅ 여기서 큐 flush - JavaScript 차트 초기화 완료 시점
            viewModel.jsBridge?.markReady()
            viewModel.onChartReady()
        },
        onWebViewReady = { webViewInstance ->
            // 초기 timeFrame 설정 적용 (1분봉)
            webViewInstance.evaluateJavascript("window.updateTimeFrame('1');", null)
            val bridge = com.lago.app.presentation.ui.chart.v5.JsBridge(
                webView = webViewInstance,
                historicalDataListener = viewModel
            )
            viewModel.jsBridge = bridge // ViewModel에 bridge 저장
            viewModel.setChartBridge(bridge)
        },
        onChartLoading = { isLoading ->
            viewModel.onChartLoadingChanged(isLoading)
        },
        onLoadingProgress = onLoadingProgress
    )
}

