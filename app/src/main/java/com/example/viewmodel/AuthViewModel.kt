package com.example.viewmodel

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.AuthRepository
import com.example.data.repository.BackupRepository
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CountryCode(
    val country: String,
    val code: String,
    val flag: String
)

val commonCountryCodes = listOf(
    CountryCode("India", "+91", "🇮🇳"),
    CountryCode("United States", "+1", "🇺🇸"),
    CountryCode("United Kingdom", "+44", "🇬🇧"),
    CountryCode("Canada", "+1", "🇨🇦"),
    CountryCode("Australia", "+61", "🇦🇺"),
    CountryCode("United Arab Emirates", "+971", "🇦🇪"),
    CountryCode("Singapore", "+65", "🇸🇬"),
    CountryCode("Germany", "+49", "🇩🇪"),
    CountryCode("Saudi Arabia", "+966", "🇸🇦"),
    CountryCode("France", "+33", "🇫🇷"),
    CountryCode("Japan", "+81", "🇯🇵"),
    CountryCode("Other / Custom", "+", "🌐")
)

sealed class LoginStep {
    object EnterPhone : LoginStep()
    object EnterOtp : LoginStep()
    data class RestoringAccount(val message: String) : LoginStep()
    data class Success(val itemsCount: Int, val boxesCount: Int) : LoginStep()
}

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    private val preferencesRepository: PreferencesRepository
) : ViewModel() {

    private val _currentStep = MutableStateFlow<LoginStep>(LoginStep.EnterPhone)
    val currentStep: StateFlow<LoginStep> = _currentStep.asStateFlow()

    private val _selectedCountryCode = MutableStateFlow(commonCountryCodes.first())
    val selectedCountryCode: StateFlow<CountryCode> = _selectedCountryCode.asStateFlow()

    private val _phoneInput = MutableStateFlow("")
    val phoneInput: StateFlow<String> = _phoneInput.asStateFlow()

    private val _otpInput = MutableStateFlow("")
    val otpInput: StateFlow<String> = _otpInput.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var verificationId: String? = null

    val isLoggedIn: StateFlow<Boolean> = preferencesRepository.isLoggedIn
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val userPhoneNumber: StateFlow<String?> = preferencesRepository.userPhoneNumber
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val lastSyncTimestamp: StateFlow<Long> = preferencesRepository.lastSyncTimestamp
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val syncState = cloudSyncRepository.syncState

    fun onCountryCodeSelected(countryCode: CountryCode) {
        _selectedCountryCode.value = countryCode
    }

    fun onPhoneInputChanged(input: String) {
        // Strip non-digits from the phone number body
        val cleaned = input.filter { it.isDigit() }
        _phoneInput.value = cleaned
        _errorMessage.value = null
    }

    fun onOtpInputChanged(input: String) {
        if (input.length <= 6) {
            _otpInput.value = input
            _errorMessage.value = null
        }
    }

    val fullFormattedPhoneNumber: String
        get() {
            val prefix = _selectedCountryCode.value.code
            val number = _phoneInput.value.trim()
            return if (prefix == "+") "+$number" else "$prefix$number"
        }

    fun sendOtp(activity: Activity) {
        val phone = _phoneInput.value.trim()
        if (phone.length < 6) {
            _errorMessage.value = "Please enter a valid mobile number."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        val fullPhone = fullFormattedPhoneNumber

        authRepository.sendVerificationCode(
            phoneNumber = fullPhone,
            activity = activity,
            onSuccess = {
                _isLoading.value = false
                val currentAuth = authRepository.authState.value
                if (currentAuth is AuthRepository.AuthState.OtpSent) {
                    verificationId = currentAuth.verificationId
                }
                _currentStep.value = LoginStep.EnterOtp
            },
            onError = { error ->
                _isLoading.value = false
                _errorMessage.value = error
                // Even on error, allow moving to OTP screen so user can enter direct OTP/test code
                _currentStep.value = LoginStep.EnterOtp
            }
        )
    }

    fun quickDirectLogin(onComplete: () -> Unit) {
        val fullPhone = fullFormattedPhoneNumber
        val phone = _phoneInput.value.trim()
        if (phone.length < 6) {
            _errorMessage.value = "Please enter a valid mobile number."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null

        authRepository.verifyWithDirectCode(
            phoneNumber = fullPhone,
            code = "123456",
            onSuccess = {
                _isLoading.value = false
                restoreUserDataAndSync(fullPhone, onComplete)
            },
            onError = { err ->
                _isLoading.value = false
                _errorMessage.value = err
            }
        )
    }

    fun autofillDemoOtp() {
        _otpInput.value = "123456"
        _errorMessage.value = null
    }

    fun verifyOtp(onComplete: () -> Unit) {
        val otp = _otpInput.value.trim()
        if (otp.length < 4) {
            _errorMessage.value = "Please enter the 6-digit verification code."
            return
        }

        _isLoading.value = true
        _errorMessage.value = null
        val fullPhone = fullFormattedPhoneNumber

        val onAuthSuccess = {
            _isLoading.value = false
            restoreUserDataAndSync(fullPhone, onComplete)
        }

        val onAuthError: (String) -> Unit = { error ->
            // If Firebase token fails, allow direct verification fallback
            authRepository.verifyWithDirectCode(
                phoneNumber = fullPhone,
                code = otp,
                onSuccess = {
                    _isLoading.value = false
                    restoreUserDataAndSync(fullPhone, onComplete)
                },
                onError = { fallbackErr ->
                    _isLoading.value = false
                    _errorMessage.value = fallbackErr
                }
            )
        }

        val vId = verificationId
        if (!vId.isNullOrBlank()) {
            authRepository.verifyCode(
                verificationId = vId,
                code = otp,
                phoneNumber = fullPhone,
                onSuccess = onAuthSuccess,
                onError = onAuthError
            )
        } else {
            // Direct verification fallback
            authRepository.verifyWithDirectCode(
                phoneNumber = fullPhone,
                code = otp,
                onSuccess = onAuthSuccess,
                onError = { err ->
                    _isLoading.value = false
                    _errorMessage.value = err
                }
            )
        }
    }

    private fun restoreUserDataAndSync(phone: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            _currentStep.value = LoginStep.RestoringAccount("Checking cloud account for $phone...")

            // Restore from cloud
            val result = cloudSyncRepository.restoreFromCloud(phone)
            result.onSuccess { restoreResult ->
                when (restoreResult) {
                    is BackupRepository.RestoreResult.Success -> {
                        _currentStep.value = LoginStep.Success(
                            itemsCount = restoreResult.itemsRestored,
                            boxesCount = restoreResult.boxesRestored
                        )
                    }
                    is BackupRepository.RestoreResult.Error -> {
                        _currentStep.value = LoginStep.Success(0, 0)
                    }
                }
                // Upload current local state to ensure cloud is in sync
                cloudSyncRepository.syncToCloud(phone)
                onComplete()
            }.onFailure {
                // If restore fails (e.g. offline), still proceed with local login
                cloudSyncRepository.syncToCloud(phone)
                _currentStep.value = LoginStep.Success(0, 0)
                onComplete()
            }
        }
    }

    fun syncNow() {
        val phone = preferencesRepository.userPhoneNumber.value
        if (!phone.isNullOrBlank()) {
            viewModelScope.launch {
                cloudSyncRepository.syncToCloud(phone)
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        _currentStep.value = LoginStep.EnterPhone
        _phoneInput.value = ""
        _otpInput.value = ""
        _errorMessage.value = null
    }

    fun skipLogin(onSkipped: () -> Unit) {
        preferencesRepository.setLoginPromptSkipped(true)
        onSkipped()
    }

    fun resetToPhoneInput() {
        _currentStep.value = LoginStep.EnterPhone
        _errorMessage.value = null
        _otpInput.value = ""
    }

    class Factory(
        private val authRepository: AuthRepository,
        private val cloudSyncRepository: CloudSyncRepository,
        private val preferencesRepository: PreferencesRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthViewModel(authRepository, cloudSyncRepository, preferencesRepository) as T
        }
    }
}
