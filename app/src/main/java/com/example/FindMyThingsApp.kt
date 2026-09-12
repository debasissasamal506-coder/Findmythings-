package com.example

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.data.local.AppDatabase
import com.example.data.repository.AuthRepository
import com.example.data.repository.BackupRepository
import com.example.data.repository.BoxRepository
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.ItemRepository
import com.example.data.repository.PreferencesRepository
import com.example.utils.ReminderReceiver

class FindMyThingsApp : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val itemRepository by lazy { ItemRepository(database.itemDao()) }
    val boxRepository by lazy { BoxRepository(database.boxDao(), database.itemDao()) }
    val preferencesRepository by lazy { PreferencesRepository(this) }
    val taskRepository by lazy { com.example.data.repository.TaskRepository(database.taskDao(), database.subTaskDao()) }
    val backupRepository by lazy { BackupRepository(database) }
    val cloudSyncRepository by lazy {
        CloudSyncRepository(database, backupRepository, preferencesRepository)
    }
    val authRepository by lazy {
        AuthRepository(preferencesRepository)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ReminderReceiver.CHANNEL_ID,
                "Item Location Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you where your saved items are located."
                enableVibration(true)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }
}
