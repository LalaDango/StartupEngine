package com.example.startupengine.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.startupengine.data.repository.AIRepository
import com.example.startupengine.data.repository.SettingsRepository
import com.example.startupengine.data.repository.TaskRepository
import com.example.startupengine.ui.home.HomeScreen
import com.example.startupengine.ui.session.SessionScreen
import com.example.startupengine.ui.settings.SettingsScreen

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Session : Screen("session/{taskId}?newTaskName={newTaskName}") {
        fun createRoute(taskId: String) = "session/$taskId"
        fun createNewRoute(taskName: String) = "session/new?newTaskName=$taskName"
    }
    data object Settings : Screen("settings")
}

@Composable
fun NavGraph(
    navController: NavHostController,
    taskRepository: TaskRepository,
    aiRepository: AIRepository,
    settingsRepository: SettingsRepository
) {
    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                taskRepository = taskRepository,
                onNavigateToSession = { taskId ->
                    navController.navigate(Screen.Session.createRoute(taskId))
                },
                onNavigateToNewSession = { taskName ->
                    navController.navigate(Screen.Session.createNewRoute(taskName))
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                }
            )
        }

        composable(
            route = Screen.Session.route,
            arguments = listOf(
                navArgument("taskId") { type = NavType.StringType },
                navArgument("newTaskName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val taskId = backStackEntry.arguments?.getString("taskId") ?: ""
            val newTaskName = backStackEntry.arguments?.getString("newTaskName") ?: ""
            SessionScreen(
                taskId = taskId,
                newTaskName = newTaskName,
                taskRepository = taskRepository,
                aiRepository = aiRepository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsRepository = settingsRepository,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
