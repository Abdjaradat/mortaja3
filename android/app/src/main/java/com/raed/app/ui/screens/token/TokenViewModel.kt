package com.raed.app.ui.screens.token

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.TokenBalanceResponse
import com.raed.app.data.api.models.TokenTransactionDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TokenUiState(
    val balance: Int = 0,
    val totalEarned: Int = 0,
    val totalSpent: Int = 0,
    val referralCode: String = "",
    val transactions: List<TokenTransactionDto> = emptyList(),
    val isLoading: Boolean = false,
    val isWatchingAd: Boolean = false,
    val error: String? = null,
    val adDailyCount: Int = 0,
)

@HiltViewModel
class TokenViewModel @Inject constructor(
    private val api: RaedApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(TokenUiState())
    val uiState: StateFlow<TokenUiState> = _uiState.asStateFlow()

    init {
        loadBalance()
    }

    fun loadBalance() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val response = api.getTokenBalance()
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.value = _uiState.value.copy(
                        balance = body.tokenBalance,
                        totalEarned = body.totalTokensEarned,
                        totalSpent = body.totalTokensSpent,
                        referralCode = body.referralCode,
                        transactions = body.transactions,
                        isLoading = false,
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = "فشل تحميل الرصيد")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.localizedMessage)
            }
        }
    }

    fun watchAd() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWatchingAd = true, error = null)
            try {
                val response = api.watchAd()
                if (response.isSuccessful) {
                    val body = response.body()!!
                    _uiState.value = _uiState.value.copy(
                        balance = body.balanceAfter,
                        adDailyCount = body.todayCount,
                        isWatchingAd = false,
                    )
                    loadBalance()
                } else if (response.code() == 429) {
                    _uiState.value = _uiState.value.copy(
                        isWatchingAd = false,
                        error = "وصلت للحد اليومي (20 مشاهدة)",
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isWatchingAd = false, error = "فشلت المشاهدة")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isWatchingAd = false, error = e.localizedMessage)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}
