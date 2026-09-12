package com.example.data.repository

import com.example.data.local.SubTaskDao
import com.example.data.local.SubTaskEntity
import com.example.data.local.TaskDao
import com.example.data.local.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao,
    private val subTaskDao: SubTaskDao? = null
) {

    val allTasks: Flow<List<TaskEntity>> = taskDao.getAllTasks()
    val incompleteTasks: Flow<List<TaskEntity>> = taskDao.getIncompleteTasks()
    val completedTasks: Flow<List<TaskEntity>> = taskDao.getCompletedTasks()
    val totalTaskCount: Flow<Int> = taskDao.getTotalTaskCount()
    val incompleteTaskCount: Flow<Int> = taskDao.getIncompleteTaskCount()
    val allSubTasks: Flow<List<SubTaskEntity>> = subTaskDao?.getAllSubTasks() ?: emptyFlow()

    fun getTodayTasks(endOfDay: Long): Flow<List<TaskEntity>> {
        return taskDao.getTodayTasks(endOfDay)
    }

    fun getTasksForThing(thingId: Long): Flow<List<TaskEntity>> {
        return taskDao.getTasksForThing(thingId)
    }

    fun getTasksForBox(boxId: Long): Flow<List<TaskEntity>> {
        return taskDao.getTasksForBox(boxId)
    }

    fun searchTasks(query: String): Flow<List<TaskEntity>> {
        return taskDao.searchTasks(query)
    }

    suspend fun getTaskById(id: Long): TaskEntity? {
        return taskDao.getTaskById(id)
    }

    suspend fun insertTask(task: TaskEntity): Long {
        return taskDao.insertTask(task)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task.copy(updatedAt = System.currentTimeMillis()))
    }

    suspend fun toggleTaskCompleted(id: Long, completed: Boolean): TaskEntity? {
        val task = taskDao.getTaskById(id) ?: return null
        val now = System.currentTimeMillis()

        if (completed && isRecurring(task.repeatType)) {
            // Archive the current occurrence as completed
            val completedArchive = task.copy(
                id = 0L,
                isCompleted = true,
                completedAt = now,
                repeatType = "None",
                reminderSet = false,
                updatedAt = now
            )
            val archiveId = taskDao.insertTask(completedArchive)

            // Copy subtasks to the archive copy if any exist
            subTaskDao?.let { sDao ->
                val existingSubTasks = sDao.getSubTasksForTaskList(id)
                if (existingSubTasks.isNotEmpty()) {
                    val archiveSubtasks = existingSubTasks.map {
                        it.copy(id = 0L, taskId = archiveId, isCompleted = true)
                    }
                    sDao.insertAll(archiveSubtasks)
                }
            }

            // Advance the original task's due date to the next occurrence
            val nextDueDate = calculateNextDueDate(task.dueDate ?: now, task.repeatType)
            val updatedTask = task.copy(
                dueDate = nextDueDate,
                isCompleted = false,
                completedAt = null,
                updatedAt = now
            )
            taskDao.updateTask(updatedTask)

            // Reset subtasks on the active recurring task for the next cycle
            subTaskDao?.let { sDao ->
                val existingSubTasks = sDao.getSubTasksForTaskList(id)
                existingSubTasks.forEach { sub ->
                    sDao.setSubTaskCompleted(sub.id, false)
                }
            }

            return updatedTask
        } else {
            val completedAt = if (completed) now else null
            taskDao.setTaskCompleted(
                id = id,
                completed = completed,
                completedAt = completedAt,
                updatedAt = now
            )
            return task.copy(isCompleted = completed, completedAt = completedAt, updatedAt = now)
        }
    }

    suspend fun snoozeTask(id: Long, snoozeDurationMillis: Long): TaskEntity? {
        val task = taskDao.getTaskById(id) ?: return null
        val now = System.currentTimeMillis()
        val baseTime = if (task.dueDate != null && task.dueDate > now) task.dueDate else now
        val newDueDate = baseTime + snoozeDurationMillis
        val newReminderTime = if (task.reminderSet) newDueDate else task.reminderTime

        val updated = task.copy(
            dueDate = newDueDate,
            reminderTime = newReminderTime,
            updatedAt = now
        )
        taskDao.updateTask(updated)
        return updated
    }

    suspend fun deleteTask(task: TaskEntity) {
        taskDao.deleteTask(task)
    }

    // Subtask management
    fun getSubTasksForTask(taskId: Long): Flow<List<SubTaskEntity>> {
        return subTaskDao?.getSubTasksForTask(taskId) ?: emptyFlow()
    }

    suspend fun getSubTasksForTaskList(taskId: Long): List<SubTaskEntity> {
        return subTaskDao?.getSubTasksForTaskList(taskId) ?: emptyList()
    }

    suspend fun insertSubTask(taskId: Long, title: String): Long {
        return subTaskDao?.insertSubTask(
            SubTaskEntity(taskId = taskId, title = title.trim())
        ) ?: -1L
    }

    suspend fun insertSubTasks(subtasks: List<SubTaskEntity>) {
        subTaskDao?.insertAll(subtasks)
    }

    suspend fun updateSubTask(subTask: SubTaskEntity) {
        subTaskDao?.updateSubTask(subTask)
    }

    suspend fun setSubTaskCompleted(id: Long, isCompleted: Boolean) {
        subTaskDao?.setSubTaskCompleted(id, isCompleted)
    }

    suspend fun updateSubTaskTitle(id: Long, title: String) {
        subTaskDao?.updateSubTaskTitle(id, title.trim())
    }

    suspend fun deleteSubTask(id: Long) {
        subTaskDao?.deleteSubTaskById(id)
    }

    companion object {
        fun isRecurring(repeatType: String): Boolean {
            return when (repeatType.lowercase().trim()) {
                "every day", "daily" -> true
                "every week", "weekly" -> true
                "every month", "monthly" -> true
                "custom" -> true
                else -> false
            }
        }

        fun calculateNextDueDate(currentDueDate: Long, repeatType: String): Long {
            val cal = Calendar.getInstance().apply { timeInMillis = currentDueDate }
            val now = System.currentTimeMillis()

            when (repeatType.lowercase().trim()) {
                "every day", "daily" -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    while (cal.timeInMillis < now) {
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                "every week", "weekly" -> {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                    while (cal.timeInMillis < now) {
                        cal.add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }
                "every month", "monthly" -> {
                    cal.add(Calendar.MONTH, 1)
                    while (cal.timeInMillis < now) {
                        cal.add(Calendar.MONTH, 1)
                    }
                }
                "custom" -> {
                    // Default custom interval: 3 days forward
                    cal.add(Calendar.DAY_OF_YEAR, 3)
                    while (cal.timeInMillis < now) {
                        cal.add(Calendar.DAY_OF_YEAR, 3)
                    }
                }
                else -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return cal.timeInMillis
        }
    }
}
