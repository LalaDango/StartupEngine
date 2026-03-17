package com.example.startupengine

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.example.startupengine.ui.navigation.NavGraph
import com.example.startupengine.ui.theme.StartupEngineTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as StartupEngineApp
        setContent {
            StartupEngineTheme {
                val navController = rememberNavController()
                NavGraph(
                    navController = navController,
                    taskRepository = app.taskRepository,
                    aiRepository = app.aiRepository,
                    settingsRepository = app.settingsRepository
                )
            }
        }
    }
}
