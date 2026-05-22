package com.raed.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    fun signInWithEmail(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            handleResult(authRepository.signInWithEmail(email, password))
        }
    }

    fun createAccount(email: String, password: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            handleResult(authRepository.createAccountWithEmail(email, password))
        }
    }

    fun signInWithGoogle(googleIdToken: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            handleResult(authRepository.signInWithGoogle(googleIdToken))
        }
    }

    fun confirmUserType(userType: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            handleResult(authRepository.exchangeToken(userType = userType))
        }
    }

    fun submitProfile(fullName: String, governorate: String) {
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            try {
                val response = api.updateMe(UpdateProfileRequest(fullName = fullName, governorate = governorate))
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
