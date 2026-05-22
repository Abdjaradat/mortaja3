package com.raed.app.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TokenTransactionDto(
    @SerialName("id") val id: String,
    @SerialName("type") val type: String,
    @SerialName("amount") val amount: Int,
    @SerialName("reason") val reason: String,
    @SerialName("balanceAfter") val balanceAfter: Int,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
data class TokenBalanceResponse(
    @SerialName("tokenBalance") val tokenBalance: Int,
    @SerialName("totalTokensEarned") val totalTokensEarned: Int,
    @SerialName("totalTokensSpent") val totalTokensSpent: Int,
    @SerialName("referralCode") val referralCode: String,
    @SerialName("transactions") val transactions: List<TokenTransactionDto>,
)

@Serializable
data class WatchAdResponse(
    @SerialName("tokensEarned") val tokensEarned: Int,
    @SerialName("balanceAfter") val balanceAfter: Int,
    @SerialName("todayCount") val todayCount: Int,
)

@Serializable
data class SpendTokenRequest(
    @SerialName("reason") val reason: String,
    @SerialName("relatedEntityId") val relatedEntityId: String? = null,
)

@Serializable
data class SpendTokenResponse(
    @SerialName("balanceAfter") val balanceAfter: Int,
)
