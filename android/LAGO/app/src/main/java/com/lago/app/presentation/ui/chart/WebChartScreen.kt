package com.lago.app.presentation.ui.chart

import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebChromeClient
import android.webkit.JavascriptInterface
import android.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature

/**
 * WebView dark mode 최적화 차트 화면
 * 
 * 해결하는 문제들:
 * 1. WebView ForceDark/Algorithmic Darkening으로 인한 첫 프레임 검은색 깜빡임
 * 2. WebView 초기 배경색과 차트 HTML 배경색 불일치
 * 3. onPageFinished vs 실제 시각적 렌더링 타이밍 차이
 * 4. Compose recomposition으로 인한 WebView 재생성 깜빡임
 */
@Composable
fun WebChartScreen(
    htmlContent: String,
    onWebViewReady: ((WebView) -> Unit)? = null,
    modifier: Modifier = Modifier,
    onChartReady: (() -> Unit)? = null,
    onChartLoading: ((Boolean) -> Unit)? = null,
    onLoadingProgress: ((Int) -> Unit)? = null,
    additionalJavaScriptInterface: Any? = null,
    interfaceName: String = "ChartInterface"
) {
    val context = LocalContext.current
    
    // remember를 사용하여 WebView 재생성 방지 (Compose recomposition 대응)
    val webView = remember {
        WebView(context).apply {
            // 1. WebView ForceDark/Algorithmic Darkening 비활성화
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(settings, WebSettingsCompat.FORCE_DARK_OFF)
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, false)
            }
            
            // 2. WebView 초기 배경색을 차트 HTML 배경색과 일치시킴 (#FFFFFF 또는 #0B0F1A)
            setBackgroundColor(Color.parseColor("#FFFFFF")) // 차트 HTML의 배경색과 동일하게 설정
            
            // 기본 WebView 설정
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                allowFileAccess = true
                allowContentAccess = true
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                    allowFileAccessFromFileURLs = true
                    allowUniversalAccessFromFileURLs = true
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                    mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                }
                
                cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
            }
            
            // WebView 디버깅 활성화 (개발 중에만)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT) {
                WebView.setWebContentsDebuggingEnabled(true)
            }
            
            // WebChromeClient 설정 (진행도 표시)
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    super.onProgressChanged(view, newProgress)
                    onLoadingProgress?.invoke(newProgress)
                }
            }
            
            // 3. onPageCommitVisible을 사용한 첫 픽셀 타이밍 개선
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                    super.onPageStarted(view, url, favicon)
                    onChartLoading?.invoke(true)
                }
                
                // onPageCommitVisible: 실제 첫 픽셀이 화면에 그려지는 시점
                override fun onPageCommitVisible(view: WebView?, url: String?) {
                    super.onPageCommitVisible(view, url)
                    
                    // 첫 픽셀 렌더링 완료 시점 - 빠른 로딩 완료 신호
                    onChartLoading?.invoke(false)
                    
                    // JavaScript 차트 초기화가 완료되면 onChartReady가 별도로 호출됨
                }
                
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    // onPageCommitVisible에서 처리하므로 여기서는 추가 작업 없음
                }
            }
            
            // JavaScript interface 추가
            addJavascriptInterface(
                WebChartJavaScriptInterface(
                    onChartReady = onChartReady,
                    onChartLoading = onChartLoading
                ),
                "Android"
            )
            
            // 추가 JavaScript interface 등록 (차트 이벤트용)
            additionalJavaScriptInterface?.let { jsInterface ->
                addJavascriptInterface(jsInterface, interfaceName)
            }
        }
    }
    
    // WebView ready 콜백 호출 및 HTML 로드 (한 번만)
    LaunchedEffect(webView) {
        onWebViewReady?.invoke(webView)
        // HTML 로드는 딱 한 번만 실행
        webView.loadDataWithBaseURL(
            "https://unpkg.com/",
            htmlContent,
            "text/html",
            "UTF-8",
            null
        )
    }
    
    // 4. AndroidView update에서 WebView 재생성 방지 - loadURL 절대 안함
    AndroidView(
        factory = { webView },
        update = { view ->
            // 절대 loadDataWithBaseURL() 호출하지 않음!
            // 모든 업데이트는 JavaScript 호출로만 처리
        },
        modifier = modifier.fillMaxSize()
    )
}

/**
 * JavaScript Interface for WebView communication
 */
class WebChartJavaScriptInterface(
    private val onChartReady: (() -> Unit)?,
    private val onChartLoading: ((Boolean) -> Unit)?
) {
    @JavascriptInterface
    fun onChartReady() {
        // JavaScript에서 차트 렌더링 완료 신호
        onChartLoading?.invoke(false)
        onChartReady?.invoke()
    }
    
    @JavascriptInterface
    fun onChartError(error: String) {
        // 차트 로딩 실패 시 로딩 상태 해제
        onChartLoading?.invoke(false)
    }
}