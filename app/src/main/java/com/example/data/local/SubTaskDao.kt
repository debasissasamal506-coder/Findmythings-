package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubTaskDao {
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY id ASC")
    fun getSubTasksForTask(taskId: Long): Flow<List<SubTaskEntity>>

    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY id ASC")
    suspend fun getSubTasksForTaskList(taskId: Long): List<SubTaskEntity>

    @Query("SELECT * FROM subtasks ORDER BY id ASC")
    fun getAllSubTasks(): Flow<List<SubTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubTask(subTask: SubTaskEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(subTasks: List<SubTaskEntity>)

    @Update
    suspend fun updateSubTask(subTask: SubTaskEntity)

    @Query("UPDATE subtasks SET isCompleted = :isCompleted WHERE id = :id")
    suspend fun setSubTaskCompleted(id: Long, isCompleted: Boolean)

    @Query("UPDATE subtasks SET title = :title WHERE id = :id")
    suspend fun updateSubTaskTitle(id: Long, title: String)

    @Delete
    suspend fun deleteSubTask(subTask: SubTaskEntity)

    @Query("DELETE FROM subtasks WHERE id = :id")
    suspend fun deleteSubTaskById(id: Long)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubTasksForTask(taskId: Long)
}
