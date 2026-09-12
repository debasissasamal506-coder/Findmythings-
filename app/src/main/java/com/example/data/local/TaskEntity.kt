package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tasks",
    indices = [
        Index(value = ["isCompleted"]),
        Index(value = ["dueDate"]),
        Index(value = ["relatedThingId"]),
        Index(value = ["relatedBoxId"])
    ]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val category: String = "Home", // Personal, Home, Study, Work, Shopping, Other, Custom
    val priority: String = "Medium", // Low, Medium, High, Urgent
    val dueDate: Long? = null,
    val dueTimeFormatted: String = "",
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val reminderSet: Boolean = false,
    val reminderTime: Long? = null,
    val repeatType: String = "None", // None, Daily, Weekly, Monthly, Custom
    val relatedThingId: Long? = null,
    val relatedThingName: String? = null,
    val relatedThingLocation: String? = null,
    val relatedBoxId: Long? = null,
    val relatedBoxName: String? = null,
    val relatedBoxLocation: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
