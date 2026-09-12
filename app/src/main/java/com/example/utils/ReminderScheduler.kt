package com.example.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.local.ItemEntity
import com.example.data.local.TaskEntity

object ReminderScheduler {
    private const val TAG = "ReminderScheduler"

    fun scheduleReminder(context: Context, item: ItemEntity) {
        val reminderTime = item.reminderDate ?: return
        if (!item.reminderEnabled || reminderTime <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ITEM_ID, item.id)
            putExtra(ReminderReceiver.EXTRA_ITEM_NAME, item.name)
            putExtra(ReminderReceiver.EXTRA_ITEM_LOCATION, item.formatLocationHierarchy())
            putExtra(ReminderReceiver.EXTRA_IS_TASK, false)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            item.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder for item ${item.name} at $reminderTime")
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling reminder", e)
        }
    }

    fun scheduleTaskReminder(context: Context, task: TaskEntity) {
        val reminderTime = task.reminderTime ?: task.dueDate ?: return
        if (!task.reminderSet || task.isCompleted || reminderTime <= System.currentTimeMillis()) {
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_ITEM_ID, task.id)
            putExtra(ReminderReceiver.EXTRA_ITEM_NAME, task.title)
            val locationDetail = buildString {
                if (!task.relatedThingName.isNullOrBlank()) {
                    append("Thing: ${task.relatedThingName}")
                    if (!task.relatedThingLocation.isNullOrBlank()) append(" (${task.relatedThingLocation})")
                }
                if (!task.relatedBoxName.isNullOrBlank()) {
                    if (isNotEmpty()) append(" • ")
                    append("Box: ${task.relatedBoxName}")
                    if (!task.relatedBoxLocation.isNullOrBlank()) append(" (${task.relatedBoxLocation})")
                }
            }
            putExtra(ReminderReceiver.EXTRA_ITEM_LOCATION, locationDetail)
            putExtra(ReminderReceiver.EXTRA_IS_TASK, true)
        }

        val notificationId = (100000 + task.id).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled reminder for task ${task.title} at $reminderTime")
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                reminderTime,
                pendingIntent
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling task reminder", e)
        }
    }

    fun cancelReminder(context: Context, itemId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            itemId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }

    fun cancelTaskReminder(context: Context, taskId: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val notificationId = (100000 + taskId).toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
    }
}
