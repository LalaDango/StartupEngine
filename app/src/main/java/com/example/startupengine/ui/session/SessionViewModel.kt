package com.example.startupengine.ui.session

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.startupengine.data.api.AIResponse
import com.example.startupengine.data.db.CompletedStep
import com.example.startupengine.data.db.Task
import com.example.startupengine.data.repository.AIRepository
import com.example.startupengine.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UiMessage(val text: String, val isAI: Boolean)
data class CurrentStep(val step: String, val time: String, val whyEasy: String)

data class SessionUiState(
    val task: Task? = null,
    val completedSteps: List<CompletedStep> = emptyList(),
    val messages: List<UiMessage> = emptyList(),
    val currentStep: CurrentStep? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class SessionViewModel(
    private val taskRepository: TaskRepository,
    private val aiRepository: AIRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SessionUiState())
    val uiState: StateFlow<SessionUiState> = _uiState.asStateFlow()

    fun startNewTask(taskName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            aiRepository.resetHistory()
            val task = taskRepository.createTask(taskName)
            _uiState.update { it.copy(task = task) }

            _uiState.update {
                it.copy(messages = it.messages + UiMessage(taskName, isAI = false))
            }
            taskRepository.saveSessionMessage(task.id, taskName, isAI = false)

            val response = aiRepository.sendMessage(taskName)
            handleAIResponse(response)
        }
    }

    fun resumeTask(taskId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            aiRepository.resetHistory()

            val task = taskRepository.getTaskById(taskId) ?: return@launch
            val steps = taskRepository.getStepsForTaskOnce(taskId)
            val pastMessages = taskRepository.getSessionMessages(taskId)
                .map { UiMessage(it.text, it.isAI) }

            _uiState.update {
                it.copy(task = task, completedSteps = steps, messages = pastMessages)
            }

            val resumeMessage = aiRepository.buildResumeMessage(task.name, steps)
            val response = aiRepository.sendMessage(resumeMessage)
            handleAIResponse(response)
        }
    }

    fun completeStep() {
        val currentStep = _uiState.value.currentStep ?: return
        val task = _uiState.value.task ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, currentStep = null) }

            val stepIndex = _uiState.value.completedSteps.size
            taskRepository.addCompletedStep(task.id, currentStep.step, stepIndex)

            val newStep = CompletedStep(
                taskId = task.id,
                stepText = currentStep.step,
                stepIndex = stepIndex,
                completedAt = System.currentTimeMillis()
            )
            _uiState.update {
                it.copy(completedSteps = it.completedSteps + newStep)
            }

            val response = aiRepository.sendMessage("できた！次のステップを教えて")
            handleAIResponse(response)
        }
    }

    fun sendMessage(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    messages = it.messages + UiMessage(text, isAI = false)
                )
            }
            _uiState.value.task?.let { task ->
                taskRepository.saveSessionMessage(task.id, text, isAI = false)
            }

            val currentStepText = _uiState.value.currentStep?.step
            val messageForAI = if (currentStepText != null) {
                "（現在の提案ステップ: 「$currentStepText」）\n$text"
            } else {
                text
            }
            val response = aiRepository.sendMessage(messageForAI)
            handleAIResponse(response)

            // pause判定
            if (response.pause == true) {
                _uiState.value.task?.let { task ->
                    taskRepository.pauseTask(task.id)
                    _uiState.update { it.copy(task = it.task?.copy(status = "paused")) }
                }
            }
        }
    }

    fun pauseAndGoBack() {
        viewModelScope.launch {
            _uiState.value.task?.let { task ->
                taskRepository.pauseTask(task.id)
            }
        }
    }

    private fun handleAIResponse(response: AIResponse) {
        _uiState.update { state ->
            val newMessages = state.messages + UiMessage(response.message, isAI = true)
            val newStep = if (response.nextStep != null) {
                CurrentStep(
                    step = response.nextStep,
                    time = response.stepTime ?: "",
                    whyEasy = response.whyEasy ?: ""
                )
            } else null

            state.copy(
                messages = newMessages,
                currentStep = newStep,
                isLoading = false,
                error = null
            )
        }

        // AI応答をDBに保存
        viewModelScope.launch {
            _uiState.value.task?.let { task ->
                taskRepository.saveSessionMessage(task.id, response.message, isAI = true)
            }
        }

        // タスク完了処理
        if (response.nextStep == null && !response.needsInput && response.pause != true) {
            viewModelScope.launch {
                _uiState.value.task?.let { task ->
                    taskRepository.completeTask(task.id)
                    _uiState.update { it.copy(task = it.task?.copy(status = "completed")) }
                }
            }
        }
    }

}
