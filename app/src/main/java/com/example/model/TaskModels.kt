package com.example.model

import androidx.compose.ui.graphics.Color

enum class TaskSection(val title: String) {
    TODAY("Today"),
    TOMORROW("Tomorrow"),
    THIS_WEEK("This Week"),
    UPCOMING("Upcoming"),
    OVERDUE("Overdue"),
    COMPLETED("Completed")
}

enum class TaskPriority(val title: String, val level: Int, val color: Color) {
    URGENT("Urgent", 0, Color(0xFFEF4444)),
    HIGH("High", 1, Color(0xFFF97316)),
    MEDIUM("Medium", 2, Color(0xFFEAB308)),
    LOW("Low", 3, Color(0xFF10B981));

    companion object {
        fun fromString(value: String): TaskPriority {
            return when (value.lowercase().trim()) {
                "urgent" -> URGENT
                "high" -> HIGH
                "medium" -> MEDIUM
                "low" -> LOW
                else -> MEDIUM
            }
        }
    }
}

enum class ReminderOption(val label: String, val offsetMillis: Long?) {
    NONE("No reminder", null),
    MIN_10("10 minutes before", 10 * 60 * 1000L),
    MIN_30("30 minutes before", 30 * 60 * 1000L),
    HOUR_1("1 hour before", 60 * 60 * 1000L),
    DAY_1("1 day before", 24 * 60 * 60 * 1000L),
    CUSTOM("Custom date & time", -1L)
}

enum class RepeatOption(val label: String) {
    NONE("Does not repeat"),
    DAILY("Every day"),
    WEEKLY("Every week"),
    MONTHLY("Every month"),
    CUSTOM("Custom");

    companion object {
        fun fromDbString(value: String): RepeatOption {
            return when (value.lowercase().trim()) {
                "daily", "every day" -> DAILY
                "weekly", "every week" -> WEEKLY
                "monthly", "every month" -> MONTHLY
                "custom" -> CUSTOM
                else -> NONE
            }
        }
    }
}

enum class TaskSortBy(val label: String) {
    DUE_DATE("Due Date"),
    PRIORITY("Priority"),
    RECENTLY_CREATED("Recently Created"),
    ALPHABETICAL("Alphabetical (A-Z)"),
    RECENTLY_UPDATED("Recently Updated")
}

data class TaskFilterState(
    val priority: TaskPriority? = null,
    val category: String? = null,
    val onlyWithReminders: Boolean = false,
    val onlyWithThing: Boolean = false,
    val onlyWithBox: Boolean = false
) {
    val isActive: Boolean
        get() = priority != null || category != null || onlyWithReminders || onlyWithThing || onlyWithBox
}
