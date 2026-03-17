package com.example.startupengine.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.startupengine.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val baseUrl: String = SettingsRepository.DEFAULT_BASE_URL,
    val modelName: String = SettingsRepository.DEFAULT_MODEL_NAME,
    val contextWindowSize: String = SettingsRepository.DEFAULT_CONTEXT_WINDOW.toString()
)

class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.baseUrl.collect { url ->
                _uiState.value = _uiState.value.copy(baseUrl = url)
            }
        }
        viewModelScope.launch {
            settingsRepository.modelName.collect { name ->
                _uiState.value = _uiState.value.copy(modelName = name)
            }
        }
        viewModelScope.launch {
            settingsRepository.contextWindowSize.collect { size ->
                _uiState.value = _uiState.value.copy(contextWindowSize = size.toString())
            }
        }
    }

    fun updateBaseUrl(url: String) {
        _uiState.value = _uiState.value.copy(baseUrl = url)
        viewModelScope.launch { settingsRepository.setBaseUrl(url) }
    }

    fun updateModelName(name: String) {
        _uiState.value = _uiState.value.copy(modelName = name)
        viewModelScope.launch { settingsRepository.setModelName(name) }
    }

    fun updateContextWindowSize(size: String) {
        _uiState.value = _uiState.value.copy(contextWindowSize = size)
        val sizeInt = size.toIntOrNull() ?: return
        viewModelScope.launch { settingsRepository.setContextWindowSize(sizeInt) }
    }
}
