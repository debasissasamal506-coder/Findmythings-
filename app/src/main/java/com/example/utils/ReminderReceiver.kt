package com.example.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val isTaskReminder = intent.getBooleanExtra(EXTRA_IS_TASK, false)
        val itemId = intent.getLongExtra(EXTRA_ITEM_ID, -1L)
        val title = intent.getStringExtra(EXTRA_ITEM_NAME) ?: if (isTaskReminder) "Task Reminder" else "Item Reminder"
        val subtitle = intent.getStringExtra(EXTRA_ITEM_LOCATION) ?: ""

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Find My Things Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminds you about your physical items and scheduled tasks."
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (!isTaskReminder && itemId > 0L) {
                putExtra("OPEN_ITEM_ID", itemId)
            }
        }

        val notificationId = if (isTaskReminder) (100000 + itemId).toInt() else itemId.toInt()

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notifTitle = if (isTaskReminder) "To-Do: $title" else "Find My Things: $title"
        val notifBody = if (isTaskReminder) {
            if (subtitle.isNotBlank()) "Linked to: $subtitle" else "You have a task scheduled for today."
        } else {
            "📍 Saved at: $subtitle"
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notifTitle)
            .setContentText(notifBody)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText("$notifTitle\n$notifBody")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    companion object {
        const val CHANNEL_ID = "channel_find_my_things_reminders"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_ITEM_NAME = "extra_item_name"
        const val EXTRA_ITEM_LOCATION = "extra_item_location"
        const val EXTRA_IS_TASK = "extra_is_task"
    }
}
