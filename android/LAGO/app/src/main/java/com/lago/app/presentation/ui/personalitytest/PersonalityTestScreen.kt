package com.lago.app.presentation.ui.personalitytest

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.lago.app.R
import com.lago.app.presentation.theme.*
import com.lago.app.presentation.ui.components.CommonTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalityTestScreen(
    nickname: String = "",
    onBackClick: () -> Unit = {},
    onPreviousClick: () -> Unit = {},
    onTestComplete: (Int) -> Unit = {}
) {
    var currentQuestionIndex by remember { mutableStateOf(0) }
    var selectedAnswers by remember { mutableStateOf(mutableMapOf<Int, Int>()) }
    var selectedOptionIndex by remember { mutableStateOf(-1) }
    
    val questions = PersonalityTestData.questions
    val currentQuestion = questions[currentQuestionIndex]
    val isLastQuestion = currentQuestionIndex == questions.size - 1
    
    val progress by animateFloatAsState(
        targetValue = (currentQuestionIndex + 1) / questions.size.toFloat(),
        animationSpec = tween(300),
        label = "progress"
    )

    LaunchedEffect(currentQuestionIndex) {
        selectedOptionIndex = selectedAnswers[currentQuestion.id] ?: -1
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier.weight(1f),
                color = MainBlue,
                trackColor = Color(0xFFE5E5E5)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Text(
                text = "${currentQuestionIndex + 1}/${questions.size}",
                style = TitleB14,
                color = MainBlue
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = currentQuestion.question,
                style = TitleB18,
                color = Color.Black,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // 선택지들
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                currentQuestion.options.forEachIndexed { index, option ->
                    AnswerOption(
                        text = "${index + 1}. ${option.text}",
                        isSelected = selectedOptionIndex == index,
                        onClick = {
                            selectedOptionIndex = index
                            selectedAnswers[currentQuestion.id] = option.score
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // 이전/다음 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // 이전 버튼 (첫 번째 질문이 아닐 때만 표시)
                if (currentQuestionIndex > 0) {
                    OutlinedButton(
                        onClick = {
                            currentQuestionIndex--
                            selectedOptionIndex = selectedAnswers[questions[currentQuestionIndex].id] ?: -1
                        },
                        modifier = Modifier
                            .width(120.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MainBlue
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MainBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "이전",
                            style = TitleB16,
                            color = MainBlue
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(120.dp))
                }
                
                // 다음/완료 버튼
                if (selectedOptionIndex != -1) {
                    Button(
                        onClick = {
                            if (isLastQuestion) {
                                val totalScore = selectedAnswers.values.sum()
                                onTestComplete(totalScore)
                            } else {
                                currentQuestionIndex++
                            }
                        },
                        modifier = Modifier
                            .width(120.dp)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MainBlue
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isLastQuestion) "완료하기" else "다음",
                            style = TitleB16,
                            color = Color.White
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .width(120.dp)
                            .height(56.dp)
                            .background(
                                color = Gray300,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLastQuestion) "완료하기" else "다음",
                            style = TitleB16,
                            color = Color.White
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun AnswerOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) MainBlue else Gray300,
        animationSpec = tween(200),
        label = "border_color"
    )
    
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) MainBlue.copy(alpha = 0.1f) else Color.White,
        animationSpec = tween(200),
        label = "background_color"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = backgroundColor
        ),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = borderColor
        )
    ) {
        Text(
            text = text,
            style = SubtitleSb16,
            color = Color.Black,
            modifier = Modifier.padding(20.dp)
        )
    }
}