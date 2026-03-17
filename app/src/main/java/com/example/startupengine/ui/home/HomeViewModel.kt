package com.example.startupengine.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.startupengine.data.db.GlobalStats
import com.example.startupengine.data.db.Task
import com.example.startupengine.data.repository.TaskRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val pausedTasks: List<Task> = emptyList(),
    val completedTasks: List<Task> = emptyList(),
    val stats: GlobalStats = GlobalStats(),
    val isLoading: Boolean = true
)

class HomeViewModel(private val taskRepository: TaskRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    // 削除取り消し用
    private var lastDeletedTask: Task? = null

    init {
        viewModelScope.launch {
            combine(
                taskRepository.getActiveTasks(),
                taskRepository.getCompletedTasks(),
                taskRepository.getStats()
            ) { active, completed, stats ->
                HomeUiState(
                    pausedTasks = active.filter { it.status == "paused" },
                    completedTasks = completed,
                    stats = stats ?: GlobalStats(),
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun deleteTask(task: Task) {
        lastDeletedTask = task
        viewModelScope.launch {
            taskRepository.deleteTask(task.id)
        }
    }

    fun undoDelete() {
        val task = lastDeletedTask ?: return
        lastDeletedTask = null
        viewModelScope.launch {
            taskRepository.createTaskFromExisting(task)
        }
    }
}
