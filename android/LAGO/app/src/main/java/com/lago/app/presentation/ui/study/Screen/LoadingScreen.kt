package com.lago.app.presentation.ui.study.Screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.lago.app.presentation.theme.AppBackground
import com.lago.app.presentation.theme.LagoTheme
import com.lago.app.presentation.ui.components.CommonTopAppBar

@Composable
fun LoadingScreen(
    title: String,
    onBackClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBackground)
    ) {
        CommonTopAppBar(
            title = title,
            onBackClick = onBackClick
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    LagoTheme {
        LoadingScreen(
            title = "데일리 퀴즈"
        )
    }
}