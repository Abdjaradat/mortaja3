package com.raed.app.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@Serializable
data class PostRequestBody(
    @SerialName("vehicleType") val vehicleType: String,
    @SerialName("budgetMin") val budgetMin: Int,
    @SerialName("budgetMax") val budgetMax: Int,
    @SerialName("governorate") val governorate: String,
    @SerialName("notes") val notes: String? = null,
)

@Serializable
data class BrokerSummaryDto(
    @SerialName("id") val id: String,
    @SerialName("fullName") val fullName: String? = null,
)

@Serializable
data class BidDto(
    @SerialName("id") val id: String,
    @SerialName("brokerId") val brokerId: String,
    @SerialName("tokens") val tokens: Int,
    @SerialName("placedAt") val placedAt: String,
    @SerialName("isWinner") val isWinner: Boolean = false,
    @SerialName("broker") val broker: BrokerSummaryDto,
)

@Serializable
data class BuyerSummaryDto(
    @SerialName("id") val id: String,
    @SerialName("fullName") val fullName: String? = null,
)

@Serializable
data class RequestDto(
    @SerialName("id") val id: String,
    @SerialName("buyerId") val buyerId: String,
    @SerialName("vehicleType") val vehicleType: String,
    @SerialName("budgetMin") val budgetMin: Int,
    @SerialName("budgetMax") val budgetMax: Int,
    @SerialName("governorate") val governorate: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("status") val status: String,
    @SerialName("expiresAt") val expiresAt: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("buyer") val buyer: BuyerSummaryDto? = null,
    @SerialName("bids") val bids: List<BidDto> = emptyList(),
) {
    val bidCount: Int get() = bids.size
    val highestBid: Int get() = bids.maxOfOrNull { it.tokens } ?: 0
    val expiresAtMillis: Long get() = parseIsoMillis(expiresAt)
    val vehicleTypeLabel: String get() = when (vehicleType) {
        "SEDAN" -> "سيدان"
        "SUV" -> "SUV"
        "HYBRID" -> "هايبرد"
        "EV" -> "كهربائي"
        "OTHER" -> "بيكأب"
        else -> vehicleType
    }

    fun myBid(userId: String): BidDto? = bids.find { it.brokerId == userId }
    fun amIWinning(userId: String): Boolean {
        val mine = myBid(userId) ?: return false
        return mine.tokens == highestBid
    }
}

@Serializable
data class BidBody(
    @SerialName("tokens") val tokens: Int,
)

@Serializable
data class WinnerDto(
    @SerialName("id") val id: String,
    @SerialName("fullName") val fullName: String? = null,
)

@Serializable
data class CloseBody(
    @SerialName("winnerId") val winnerId: String,
)

@Serializable
data class CloseResultDto(
    @SerialName("closed") val closed: Boolean,
    @SerialName("winner") val winner: WinnerDto? = null,
)

internal fun parseIsoMillis(iso: String): Long = try {
    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        .also { it.timeZone = TimeZone.getTimeZone("UTC") }
        .parse(iso)?.time ?: 0L
} catch (_: Exception) {
    0L
}
