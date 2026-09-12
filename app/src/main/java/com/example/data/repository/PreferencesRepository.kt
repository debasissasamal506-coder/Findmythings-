package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.MessageDigest

class PreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("find_my_things_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(prefs.getString(KEY_THEME_MODE, "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _onboardingCompleted = MutableStateFlow(prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false))
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean(KEY_NOTIFICATIONS_ENABLED, true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _appLockEnabled = MutableStateFlow(prefs.getBoolean(KEY_APP_LOCK_ENABLED, false))
    val appLockEnabled: StateFlow<Boolean> = _appLockEnabled.asStateFlow()

    private val _defaultCategory = MutableStateFlow(prefs.getString(KEY_DEFAULT_CATEGORY, "Documents") ?: "Documents")
    val defaultCategory: StateFlow<String> = _defaultCategory.asStateFlow()

    private val _userPhoneNumber = MutableStateFlow(prefs.getString(KEY_USER_PHONE_NUMBER, null))
    val userPhoneNumber: StateFlow<String?> = _userPhoneNumber.asStateFlow()

    private val _userUid = MutableStateFlow(prefs.getString(KEY_USER_UID, null))
    val userUid: StateFlow<String?> = _userUid.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(prefs.getBoolean(KEY_IS_LOGGED_IN, false))
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(prefs.getLong(KEY_LAST_SYNC_TIMESTAMP, 0L))
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _loginPromptSkipped = MutableStateFlow(prefs.getBoolean(KEY_LOGIN_SKIPPED, false))
    val loginPromptSkipped: StateFlow<Boolean> = _loginPromptSkipped.asStateFlow()

    // Transient in-memory unlocked state for current app session
    private val _isSessionUnlocked = MutableStateFlow(false)
    val isSessionUnlocked: StateFlow<Boolean> = _isSessionUnlocked.asStateFlow()

    private val _customTaskCategories = MutableStateFlow(
        prefs.getStringSet(KEY_CUSTOM_TASK_CATEGORIES, emptySet()) ?: emptySet()
    )
    val customTaskCategories: StateFlow<Set<String>> = _customTaskCategories.asStateFlow()

    fun addCustomTaskCategory(category: String) {
        val trimmed = category.trim()
        if (trimmed.isBlank()) return
        val current = _customTaskCategories.value.toMutableSet()
        current.add(trimmed)
        prefs.edit().putStringSet(KEY_CUSTOM_TASK_CATEGORIES, current).apply()
        _customTaskCategories.value = current
    }

    fun removeCustomTaskCategory(category: String) {
        val current = _customTaskCategories.value.toMutableSet()
        current.remove(category)
        prefs.edit().putStringSet(KEY_CUSTOM_TASK_CATEGORIES, current).apply()
        _customTaskCategories.value = current
    }

    fun setOnboardingCompleted(completed: Boolean) {
        prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
        _onboardingCompleted.value = completed
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString(KEY_THEME_MODE, mode).apply()
        _themeMode.value = mode
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_NOTIFICATIONS_ENABLED, enabled).apply()
        _notificationsEnabled.value = enabled
    }

    fun setDefaultCategory(category: String) {
        prefs.edit().putString(KEY_DEFAULT_CATEGORY, category).apply()
        _defaultCategory.value = category
    }

    fun setAppLock(pin: String) {
        val salt = "FMT_SALT_2026"
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, true)
            .putString(KEY_PIN_HASH, hash)
            .apply()
        _appLockEnabled.value = true
        _isSessionUnlocked.value = true
    }

    fun disableAppLock() {
        prefs.edit()
            .putBoolean(KEY_APP_LOCK_ENABLED, false)
            .remove(KEY_PIN_HASH)
            .apply()
        _appLockEnabled.value = false
        _isSessionUnlocked.value = true
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val salt = "FMT_SALT_2026"
        val inputHash = hashPin(pin, salt)
        val valid = storedHash == inputHash
        if (valid) {
            _isSessionUnlocked.value = true
        }
        return valid
    }

    fun lockSession() {
        _isSessionUnlocked.value = false
    }

    fun unlockSession() {
        _isSessionUnlocked.value = true
    }

    fun setUserSession(phoneNumber: String, uid: String?) {
        prefs.edit()
            .putString(KEY_USER_PHONE_NUMBER, phoneNumber)
            .putString(KEY_USER_UID, uid)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putBoolean(KEY_LOGIN_SKIPPED, false)
            .apply()
        _userPhoneNumber.value = phoneNumber
        _userUid.value = uid
        _isLoggedIn.value = true
        _loginPromptSkipped.value = false
    }

    fun clearUserSession() {
        prefs.edit()
            .remove(KEY_USER_PHONE_NUMBER)
            .remove(KEY_USER_UID)
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_LAST_SYNC_TIMESTAMP)
            .apply()
        _userPhoneNumber.value = null
        _userUid.value = null
        _isLoggedIn.value = false
        _lastSyncTimestamp.value = 0L
    }

    fun setLastSyncTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_SYNC_TIMESTAMP, timestamp).apply()
        _lastSyncTimestamp.value = timestamp
    }

    fun setLoginPromptSkipped(skipped: Boolean) {
        prefs.edit().putBoolean(KEY_LOGIN_SKIPPED, skipped).apply()
        _loginPromptSkipped.value = skipped
    }

    private fun hashPin(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest((salt + pin).toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_ONBOARDING_COMPLETED = "key_onboarding_completed"
        private const val KEY_NOTIFICATIONS_ENABLED = "key_notifications_enabled"
        private const val KEY_APP_LOCK_ENABLED = "key_app_lock_enabled"
        private const val KEY_PIN_HASH = "key_pin_hash"
        private const val KEY_DEFAULT_CATEGORY = "key_default_category"
        private const val KEY_USER_PHONE_NUMBER = "key_user_phone_number"
        private const val KEY_USER_UID = "key_user_uid"
        private const val KEY_IS_LOGGED_IN = "key_is_logged_in"
        private const val KEY_LAST_SYNC_TIMESTAMP = "key_last_sync_timestamp"
        private const val KEY_LOGIN_SKIPPED = "key_login_skipped"
        private const val KEY_CUSTOM_TASK_CATEGORIES = "key_custom_task_categories"
    }
}
