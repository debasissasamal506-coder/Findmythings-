package com.example.data.repository

import android.os.Build
import android.util.Log
import com.example.data.local.AppDatabase
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resumeWithException

class CloudSyncRepository(
    private val database: AppDatabase,
    private val backupRepository: BackupRepository,
    private val preferencesRepository: PreferencesRepository
) {
    private val tag = "CloudSyncRepository"

    sealed class SyncState {
        object Idle : SyncState()
        object Syncing : SyncState()
        data class Success(val message: String, val timestamp: Long) : SyncState()
        data class Error(val message: String) : SyncState()
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private fun normalizePhoneNumber(phone: String): String {
        return phone.replace("[^0-9+]".toRegex(), "")
    }

    /**
     * Uploads the current local database (all items and boxes) to the user's Cloud Account.
     */
    suspend fun syncToCloud(phoneNumber: String): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing
        try {
            val cleanPhone = normalizePhoneNumber(phoneNumber)
            if (cleanPhone.isBlank()) {
                val err = "Invalid phone number for sync"
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            val items = database.itemDao().getAllItemsList()
            val boxes = database.boxDao().getAllBoxesList()
            val backupJson = backupRepository.exportBackupJson()

            val firestore = FirebaseFirestore.getInstance()
            val userDocRef = firestore.collection("users")
                .document(cleanPhone)
                .collection("backup")
                .document("latest")

            val payload = hashMapOf<String, Any>(
                "phoneNumber" to cleanPhone,
                "itemCount" to items.size,
                "boxCount" to boxes.size,
                "backupJson" to backupJson,
                "deviceInfo" to "${Build.MANUFACTURER} ${Build.MODEL}",
                "updatedAt" to System.currentTimeMillis()
            )

            userDocRef.set(payload, SetOptions.merge()).awaitCustom()

            val now = System.currentTimeMillis()
            preferencesRepository.setLastSyncTimestamp(now)
            _syncState.value = SyncState.Success("Cloud backup updated successfully", now)
            Log.d(tag, "Successfully synced ${items.size} items and ${boxes.size} boxes to cloud for $cleanPhone")

            Result.success(Pair(items.size, boxes.size))
        } catch (e: Exception) {
            Log.e(tag, "Cloud sync failed", e)
            val msg = e.localizedMessage ?: "Failed to sync to cloud"
            _syncState.value = SyncState.Error(msg)
            Result.failure(e)
        }
    }

    /**
     * Restores items and boxes from the user's Cloud Account into the local Room database.
     * When user deletes the app and reinstalls, logging in with the same number retrieves this cloud data.
     */
    suspend fun restoreFromCloud(phoneNumber: String): Result<BackupRepository.RestoreResult> = withContext(Dispatchers.IO) {
        _syncState.value = SyncState.Syncing
        try {
            val cleanPhone = normalizePhoneNumber(phoneNumber)
            if (cleanPhone.isBlank()) {
                val err = "Invalid phone number"
                _syncState.value = SyncState.Error(err)
                return@withContext Result.failure(IllegalArgumentException(err))
            }

            val firestore = FirebaseFirestore.getInstance()
            val userDocRef = firestore.collection("users")
                .document(cleanPhone)
                .collection("backup")
                .document("latest")

            val snapshot = userDocRef.get().awaitCustom()
            if (!snapshot.exists()) {
                val notFoundMsg = "No existing cloud backup found for $cleanPhone"
                _syncState.value = SyncState.Success("New account initialized", System.currentTimeMillis())
                return@withContext Result.success(BackupRepository.RestoreResult.Success(0, 0))
            }

            val backupJson = snapshot.getString("backupJson")
            if (backupJson.isNullOrBlank()) {
                _syncState.value = SyncState.Success("Empty backup found", System.currentTimeMillis())
                return@withContext Result.success(BackupRepository.RestoreResult.Success(0, 0))
            }

            val restoreResult = backupRepository.restoreFromJson(backupJson)
            when (restoreResult) {
                is BackupRepository.RestoreResult.Success -> {
                    val now = System.currentTimeMillis()
                    preferencesRepository.setLastSyncTimestamp(now)
                    _syncState.value = SyncState.Success("Restored ${restoreResult.itemsRestored} items & ${restoreResult.boxesRestored} boxes", now)
                    Log.d(tag, "Successfully restored ${restoreResult.itemsRestored} items from cloud for $cleanPhone")
                    Result.success(restoreResult)
                }
                is BackupRepository.RestoreResult.Error -> {
                    _syncState.value = SyncState.Error(restoreResult.message)
                    Result.failure(Exception(restoreResult.message))
                }
            }
        } catch (e: Exception) {
            Log.e(tag, "Cloud restore failed", e)
            val msg = e.localizedMessage ?: "Failed to restore from cloud"
            _syncState.value = SyncState.Error(msg)
            Result.failure(e)
        }
    }

    /**
     * Checks if a cloud backup exists for the given phone number.
     */
    suspend fun hasCloudBackup(phoneNumber: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val cleanPhone = normalizePhoneNumber(phoneNumber)
            val firestore = FirebaseFirestore.getInstance()
            val snapshot = firestore.collection("users")
                .document(cleanPhone)
                .collection("backup")
                .document("latest")
                .get()
                .awaitCustom()

            snapshot.exists() && !snapshot.getString("backupJson").isNullOrBlank()
        } catch (e: Exception) {
            Log.w(tag, "Error checking cloud backup existence: ${e.message}")
            false
        }
    }

    /**
     * Triggers background sync if user is currently logged in.
     */
    suspend fun autoSyncIfLoggedIn() {
        val phone = preferencesRepository.userPhoneNumber.value
        val loggedIn = preferencesRepository.isLoggedIn.value
        if (loggedIn && !phone.isNullOrBlank()) {
            syncToCloud(phone)
        }
    }
}

/**
 * Await helper for Google Play Services Task without external dependency.
 */
suspend fun <T> Task<T>.awaitCustom(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        cont.resume(result) {
            // Cancellation cleanup if needed
        }
    }
    addOnFailureListener { exception ->
        cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}
