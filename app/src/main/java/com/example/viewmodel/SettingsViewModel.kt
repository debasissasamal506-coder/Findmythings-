package com.example.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.BackupRepository
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class BackupUiEvent {
    data class ExportReady(val jsonString: String) : BackupUiEvent()
    data class RestoreSuccess(val message: String) : BackupUiEvent()
    data class Error(val message: String) : BackupUiEvent()
}

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val backupRepository: BackupRepository
) : ViewModel() {

    val themeMode: StateFlow<String> = preferencesRepository.themeMode
    val notificationsEnabled: StateFlow<Boolean> = preferencesRepository.notificationsEnabled
    val appLockEnabled: StateFlow<Boolean> = preferencesRepository.appLockEnabled
    val defaultCategory: StateFlow<String> = preferencesRepository.defaultCategory

    private val _backupEvent = MutableSharedFlow<BackupUiEvent>()
    val backupEvent: SharedFlow<BackupUiEvent> = _backupEvent.asSharedFlow()

    fun setThemeMode(mode: String) {
        preferencesRepository.setThemeMode(mode)
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        preferencesRepository.setNotificationsEnabled(enabled)
    }

    fun setDefaultCategory(category: String) {
        preferencesRepository.setDefaultCategory(category)
    }

    fun setAppLockPin(pin: String) {
        preferencesRepository.setAppLock(pin)
    }

    fun disableAppLock() {
        preferencesRepository.disableAppLock()
    }

    fun lockAppSession() {
        preferencesRepository.lockSession()
    }

    fun exportBackup() {
        viewModelScope.launch {
            try {
                val json = backupRepository.exportBackupJson()
                _backupEvent.emit(BackupUiEvent.ExportReady(json))
            } catch (e: Exception) {
                _backupEvent.emit(BackupUiEvent.Error("Backup failed: ${e.localizedMessage}"))
            }
        }
    }

    fun restoreBackup(jsonString: String) {
        viewModelScope.launch {
            when (val result = backupRepository.restoreFromJson(jsonString)) {
                is BackupRepository.RestoreResult.Success -> {
                    _backupEvent.emit(
                        BackupUiEvent.RestoreSuccess(
                            "Restored ${result.itemsRestored} items, ${result.boxesRestored} boxes, and ${result.tasksRestored} tasks!"
                        )
                    )
                }
                is BackupRepository.RestoreResult.Error -> {
                    _backupEvent.emit(BackupUiEvent.Error(result.message))
                }
            }
        }
    }

    fun shareApp(context: Context) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(
                Intent.EXTRA_TEXT,
                "Find My Things - Remember where you kept it! The offline, privacy-first personal item organizer."
            )
        }
        val chooser = Intent.createChooser(intent, "Share Find My Things")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }

    class Factory(
        private val preferencesRepository: PreferencesRepository,
        private val backupRepository: BackupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SettingsViewModel(preferencesRepository, backupRepository) as T
        }
    }
}
