package com.example.startupengine.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.startupengine.data.db.Task
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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    taskRepository: TaskRepository,
    onNavigateToSession: (String) -> Unit,
    onNavigateToNewSession: (String) -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val viewModel = remember { HomeViewModel(taskRepository) }
    val uiState by viewModel.uiState.collectAsState()
    var inputText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val sampleChips = listOf("年金の免除申請", "副業の方向性", "マイナンバー更新")

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgDark
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .navigationBarsPadding()
                .imePadding()
        ) {
            // Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Settings icon
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "設定",
                            tint = TextSub
                        )
                    }
                }

                Text(
                    "\u26A1",
                    fontSize = 28.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row {
                    Text(
                        "\u25B8",
                        color = Accent,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "着手エンジン",
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "先送りを、今すぐの1歩に",
                    color = TextSub,
                    fontSize = 13.sp
                )

                // Stats
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceDark, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${uiState.stats.totalSteps}",
                            color = Success,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "TOTAL STEPS",
                            color = TextSub,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(Border)
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${uiState.stats.totalTasks}",
                            color = Accent,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "TASKS",
                            color = TextSub,
                            fontSize = 10.sp,
                            letterSpacing = 0.8.sp
                        )
                    }
                }
            }

            // Task list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Paused tasks
                if (uiState.pausedTasks.isNotEmpty()) {
                    item {
                        SectionHeader("\u23F8 中断中のタスク")
                    }
                    items(uiState.pausedTasks, key = { it.id }) { task ->
                        TaskListItem(
                            task = task,
                            onTap = { onNavigateToSession(task.id) },
                            onDelete = {
                                viewModel.deleteTask(task)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "「${task.name}」を削除しました",
                                        actionLabel = "元に戻す",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    }
                                }
                            }
                        )
                    }
                    item { Spacer(modifier = Modifier.height(12.dp)) }
                }

                // Completed tasks
                if (uiState.completedTasks.isNotEmpty()) {
                    item {
                        SectionHeader("\uD83D\uDCCB 履歴")
                    }
                    items(uiState.completedTasks.take(10), key = { it.id }) { task ->
                        TaskListItem(
                            task = task,
                            onTap = { onNavigateToSession(task.id) },
                            onDelete = {
                                viewModel.deleteTask(task)
                                scope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "「${task.name}」を削除しました",
                                        actionLabel = "元に戻す",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.undoDelete()
                                    }
                                }
                            }
                        )
                    }
                }

                // Empty state
                if (uiState.pausedTasks.isEmpty() && uiState.completedTasks.isEmpty() && !uiState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "「年金免除」「副業探し」「確定申告」...\n曖昧でOK。まず1行投げ込んでみて。",
                                color = TextDim,
                                fontSize = 14.sp,
                                lineHeight = 25.2.sp
                            )
                        }
                    }
                }
            }

            // Bottom input area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SurfaceDark)
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Sample chips
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(bottom = 10.dp)
                ) {
                    sampleChips.forEach { chip ->
                        AssistChip(
                            onClick = { inputText = chip },
                            label = { Text(chip, fontSize = 12.sp) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = CardDark,
                                labelColor = TextSub
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                borderColor = Border,
                                enabled = true
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }

                // Input field
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text("先送りしてるタスクを1行で...", color = TextDim)
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
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = {
                            if (inputText.isNotBlank()) {
                                val name = inputText.trim()
                                inputText = ""
                                onNavigateToNewSession(name)
                            }
                        })
                    )
                    Button(
                        onClick = {
                            if (inputText.isNotBlank()) {
                                val name = inputText.trim()
                                inputText = ""
                                onNavigateToNewSession(name)
                            }
                        },
                        enabled = inputText.isNotBlank(),
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
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        color = TextSub,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun TaskListItem(
    task: Task,
    onTap: () -> Unit,
    onDelete: () -> Unit
) {
    val isPaused = task.status == "paused"
    val dateFormat = remember { SimpleDateFormat("M/d", Locale.JAPAN) }
    val dateStr = dateFormat.format(Date(task.updatedAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardDark, RoundedCornerShape(14.dp))
            .border(1.dp, Border, RoundedCornerShape(14.dp))
            .clickable(onClick = onTap)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(
                    if (isPaused) AccentGlow else SuccessDim,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (isPaused) "\u23F8" else "\u2713",
                fontSize = 18.sp
            )
        }

        // Text
        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.name,
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                dateStr,
                color = TextSub,
                fontSize = 12.sp
            )
        }

        // Resume badge
        if (isPaused) {
            Text(
                "続きから \u25B8",
                color = Accent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .background(AccentDim, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        // Delete button
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                Icons.Default.Close,
                contentDescription = "削除",
                tint = TextDim,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
