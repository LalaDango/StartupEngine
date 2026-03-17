package com.example.startupengine.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    companion object {
        private val KEY_BASE_URL = stringPreferencesKey("base_url")
        private val KEY_MODEL_NAME = stringPreferencesKey("model_name")
        private val KEY_CONTEXT_WINDOW = intPreferencesKey("context_window_size")

        const val DEFAULT_BASE_URL = "http://192.168.1.1:52625"
        const val DEFAULT_MODEL_NAME = "qwen3:8b"
        const val DEFAULT_CONTEXT_WINDOW = 8192
    }

    val baseUrl: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_BASE_URL] ?: DEFAULT_BASE_URL
    }

    val modelName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_MODEL_NAME] ?: DEFAULT_MODEL_NAME
    }

    val contextWindowSize: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_CONTEXT_WINDOW] ?: DEFAULT_CONTEXT_WINDOW
    }

    suspend fun getBaseUrl(): String =
        context.dataStore.data.first()[KEY_BASE_URL] ?: DEFAULT_BASE_URL

    suspend fun getModelName(): String =
        context.dataStore.data.first()[KEY_MODEL_NAME] ?: DEFAULT_MODEL_NAME

    suspend fun getContextWindowSize(): Int =
        context.dataStore.data.first()[KEY_CONTEXT_WINDOW] ?: DEFAULT_CONTEXT_WINDOW

    suspend fun setBaseUrl(url: String) {
        context.dataStore.edit { it[KEY_BASE_URL] = url }
    }

    suspend fun setModelName(name: String) {
        context.dataStore.edit { it[KEY_MODEL_NAME] = name }
    }

    suspend fun setContextWindowSize(size: Int) {
        context.dataStore.edit { it[KEY_CONTEXT_WINDOW] = size }
    }
}
