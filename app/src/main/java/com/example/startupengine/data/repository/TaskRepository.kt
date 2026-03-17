package com.example.startupengine.data.repository

import com.example.startupengine.data.db.CompletedStep
import com.example.startupengine.data.db.CompletedStepDao
import com.example.startupengine.data.db.GlobalStats
import com.example.startupengine.data.db.GlobalStatsDao
import com.example.startupengine.data.db.SessionMessage
import com.example.startupengine.data.db.SessionMessageDao
import com.example.startupengine.data.db.Task
import com.example.startupengine.data.db.TaskDao
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val completedStepDao: CompletedStepDao,
    private val globalStatsDao: GlobalStatsDao,
    private val sessionMessageDao: SessionMessageDao
) {
    fun getActiveTasks(): Flow<List<Task>> = taskDao.getActiveTasks()
    fun getCompletedTasks(): Flow<List<Task>> = taskDao.getCompletedTasks()
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()
    fun getStats(): Flow<GlobalStats?> = globalStatsDao.getStats()
    fun getStepsForTask(taskId: String): Flow<List<CompletedStep>> =
        completedStepDao.getStepsForTask(taskId)

    suspend fun getTaskById(id: String): Task? = taskDao.getTaskById(id)

    suspend fun createTask(name: String): Task {
        val now = System.currentTimeMillis()
        val task = Task(
            id = "t_$now",
            name = name,
            status = "active",
            createdAt = now,
            updatedAt = now
        )
        taskDao.insert(task)
        ensureStatsExist()
        globalStatsDao.incrementTotalTasks()
        return task
    }

    suspend fun pauseTask(taskId: String) {
        val task = taskDao.getTaskById(taskId) ?: return
        taskDao.update(task.copy(status = "paused", updatedAt = System.currentTimeMillis()))
    }

    suspend fun completeTask(taskId: String) {
        val task = taskDao.getTaskById(taskId) ?: return
        taskDao.update(task.copy(status = "completed", updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteTask(taskId: String) {
        taskDao.deleteById(taskId)
    }

    suspend fun createTaskFromExisting(task: Task) {
        taskDao.insert(task)
    }

    suspend fun addCompletedStep(taskId: String, stepText: String, stepIndex: Int) {
        completedStepDao.insert(
            CompletedStep(
                taskId = taskId,
                stepText = stepText,
                stepIndex = stepIndex,
                completedAt = System.currentTimeMillis()
            )
        )
        ensureStatsExist()
        globalStatsDao.incrementTotalSteps()
        val task = taskDao.getTaskById(taskId) ?: return
        taskDao.update(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun getStepsForTaskOnce(taskId: String): List<CompletedStep> =
        completedStepDao.getStepsForTaskOnce(taskId)

    suspend fun getStepCountForTask(taskId: String): Int =
        completedStepDao.getStepCountForTask(taskId)

    suspend fun saveSessionMessage(taskId: String, text: String, isAI: Boolean) {
        sessionMessageDao.insert(SessionMessage(taskId = taskId, text = text, isAI = isAI))
    }

    suspend fun getSessionMessages(taskId: String): List<SessionMessage> =
        sessionMessageDao.getMessagesForTask(taskId)

    private suspend fun ensureStatsExist() {
        if (globalStatsDao.getStatsOnce() == null) {
            globalStatsDao.upsert(GlobalStats())
        }
    }
}
