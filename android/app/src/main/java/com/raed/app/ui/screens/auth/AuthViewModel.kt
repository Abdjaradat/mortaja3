package com.raed.app.ui.screens.auth

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.PhoneAuthCredential
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.UpdateProfileRequest
import com.raed.app.data.auth.AuthRepository
import com.raed.app.data.auth.AuthResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object OtpSent : AuthUiState()
    object NeedsUserType : AuthUiState()
    data class NeedsProfile(val userType: String) : AuthUiState()
    object Authenticated : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val api: RaedApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private var verificationId: String? = null

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _uiState.value = AuthUiState.Loading
        authRepository.sendPhoneOtp(
            phoneNumber = phoneNumber,
            activity = activity,
            onCodeSent = { vId ->
                verificationId = vId
                _uiState.value = AuthUiState.OtpSent
            },
            onAutoVerified = { credential: PhoneAuthCredential ->
                viewModelScope.launch { handleResult(authRepository.signInWithPhoneCredential(credential)) }
            },
            onError = { msg ->
                _uiState.value = AuthUiState.Error(msg)
            },
        )
    }

    fun verifyCode(code: String) {
        val vId = verificationId ?: run {
            _uiState.value = AuthUiState.Error("انتهت صلاحية الجلسة — أعد إرسال الرمز")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            handleResult(authRepository.verifyPhoneCode(vId, code))
        }
    }

    private var pendingOfficerStatus: String? = null

    fun confirmUserType(userType: String, officerStatus: String? = null) {
        pendingOfficerStatus = officerStatus
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            handleResult(authRepository.exchangeToken(userType = userType))
        }
    }

    fun submitProfile(fullName: String, governorate: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.updateMe(
                    UpdateProfileRequest(
                        fullName = fullName,
                        governorate = governorate,
                        officerStatus = pendingOfficerStatus,
                    )
                )
                _uiState.value = if (response.isSuccessful) AuthUiState.Authenticated
                else AuthUiState.Error("فشل حفظ الملف الشخصي")
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(e.localizedMessage ?: "خطأ غير متوقع")
            }
        }
    }

    fun clearError() {
        if (_uiState.value is AuthUiState.Error) _uiState.value = AuthUiState.Idle
    }

    fun resetToIdle() {
        verificationId = null
        _uiState.value = AuthUiState.Idle
    }

    private fun handleResult(result: AuthResult) {
        _uiState.value = when (result) {
            is AuthResult.Success -> when {
                result.isNewUser || !result.hasProfile -> AuthUiState.NeedsProfile(result.userType)
                else -> AuthUiState.Authenticated
            }
            is AuthResult.Error -> if (result.message == "NEW_USER_NEEDS_TYPE") {
                AuthUiState.NeedsUserType
            } else {
                AuthUiState.Error(result.message)
            }
        }
    }
}
