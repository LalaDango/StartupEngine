package com.example.startupengine.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.startupengine.data.db.CompletedStep
import com.example.startupengine.data.repository.AIRepository
import com.example.startupengine.data.repository.TaskRepository
import com.example.startupengine.ui.theme.Accent
import com.example.startupengine.ui.theme.AccentDim
import com.example.startupengine.ui.theme.AccentGlow
import com.example.startupengine.ui.theme.BgDark
import com.example.startupengine.ui.theme.Border
import com.example.startupengine.ui.theme.CardDark
import com.example.startupengine.ui.theme.Success
import com.example.startupengine.ui.theme.SuccessDim
import com.example.startupengine.ui.theme.SurfaceDark
import com.example.startupengine.ui.theme.TextDim
import com.example.startupengine.ui.theme.TextPrimary
import com.example.startupengine.ui.theme.TextSub
import com.example.startupengine.ui.theme.Warn

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SessionScreen(
    taskId: String,
    newTaskName: String,
    taskRepository: TaskRepository,
    aiRepository: AIRepository,
    onNavigateBack: () -> Unit
) {
    val viewModel = remember {
        SessionViewModel(taskRepository, aiRepository)
    }

    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    // 初期化
    LaunchedEffect(taskId, newTaskName) {
        if (taskId == "new" && newTaskName.isNotEmpty()) {
            viewModel.startNewTask(newTaskName)
        } else if (taskId != "new") {
            viewModel.resumeTask(taskId)
        }
    }

    // 自動スクロール
    LaunchedEffect(uiState.messages.size, uiState.currentStep, uiState.completedSteps.size) {
        val totalItems = uiState.completedSteps.size + uiState.messages.size +
            (if (uiState.isLoading) 1 else 0) + (if (uiState.currentStep != null) 1 else 0)
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top bar
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "\u25B8 ",
                        color = Accent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = uiState.task?.name ?: "新しいタスク",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 15.sp
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = {
                    viewModel.pauseAndGoBack()
                    onNavigateBack()
                }) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "戻る",
                        tint = TextSub
                    )
                }
            },
            actions = {
                // DONE counter
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${uiState.completedSteps.size}",
                        color = Success,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "DONE",
                        color = TextSub,
                        fontSize = 9.sp
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.pauseAndGoBack()
                        onNavigateBack()
                    },
                    border = androidx.compose.foundation.BorderStroke(1.dp, Border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Warn)
                ) {
                    Icon(
                        Icons.Default.Pause,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("中断", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = BgDark
            )
        )

        // Content
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 完了ステップ
            items(uiState.completedSteps) { step ->
                CompletedStepCard(step)
            }

            // チャットメッセージ
            items(uiState.messages) { message ->
                ChatBubble(message)
            }

            // ローディング
            if (uiState.isLoading) {
                item {
                    Text(
                        text = "考え中...",
                        color = TextSub,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // 現在のステップカード
            uiState.currentStep?.let { step ->
                item {
                    StepCard(
                        step = step,
                        onComplete = { viewModel.completeStep() }
                    )
                }
            }

            // 下部のスペース
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }

        // Input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SurfaceDark)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        if (uiState.currentStep != null) "困ったら何でも..." else "タスクを入力...",
                        color = TextDim
                    )
                },
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Accent,
                    unfocusedBorderColor = Border,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary,
                    cursorColor = Accent
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                enabled = !uiState.isLoading,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                })
            )
            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendMessage(inputText)
                        inputText = ""
                    }
                },
                enabled = inputText.isNotBlank() && !uiState.isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Accent,
                    disabledContainerColor = CardDark
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("\u25B8", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CompletedStepCard(step: CompletedStep) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SuccessDim, RoundedCornerShape(10.dp))
            .border(
                width = 3.dp,
                color = Success,
                shape = RoundedCornerShape(10.dp)
            )
            .padding(10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Success),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "\u2713",
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Column {
            Text(
                "Step ${step.stepIndex + 1}",
                color = TextSub,
                fontSize = 11.sp
            )
            Text(
                step.stepText,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 21.sp
            )
        }
    }
}

@Composable
private fun ChatBubble(message: UiMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isAI) Arrangement.Start else Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .background(
                    if (message.isAI) CardDark else AccentDim,
                    RoundedCornerShape(
                        topStart = if (message.isAI) 4.dp else 14.dp,
                        topEnd = if (message.isAI) 14.dp else 4.dp,
                        bottomStart = 14.dp,
                        bottomEnd = 14.dp
                    )
                )
                .border(
                    1.dp,
                    if (message.isAI) Border else Accent.copy(alpha = 0.27f),
                    RoundedCornerShape(
                        topStart = if (message.isAI) 4.dp else 14.dp,
                        topEnd = if (message.isAI) 14.dp else 4.dp,
                        bottomStart = 14.dp,
                        bottomEnd = 14.dp
                    )
                )
                .padding(horizontal = 15.dp, vertical = 11.dp)
        ) {
            Text(
                text = message.text,
                color = TextPrimary,
                fontSize = 14.sp,
                lineHeight = 22.4.sp
            )
        }
    }
}

@Composable
private fun StepCard(step: CurrentStep, onComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(16.dp))
            .background(CardDark, RoundedCornerShape(16.dp))
            .border(2.dp, Accent, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "\u25B8 NEXT STEP",
                color = Accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            if (step.time.isNotEmpty()) {
                Text(
                    "\u23F1 ${step.time}",
                    color = TextSub,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .background(AccentGlow, RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Step text
        Text(
            text = step.step,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 25.6.sp
        )

        // Why easy
        if (step.whyEasy.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "\uD83D\uDCA1 ${step.whyEasy}",
                color = TextSub,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Complete button
        Button(
            onClick = onComplete,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Accent
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                "\u2713 できた！次へ",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}
