package com.raed.app.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterAgentBody(
    @SerialName("displayName") val displayName: String,
    @SerialName("location") val location: String,
    @SerialName("specializations") val specializations: List<String>,
    @SerialName("yearsExperience") val yearsExperience: Int,
    @SerialName("bio") val bio: String? = null,
)

@Serializable
data class ClearanceAgentDto(
    @SerialName("id") val id: String,
    @SerialName("userId") val userId: String,
    @SerialName("displayName") val displayName: String,
    @SerialName("photoUrl") val photoUrl: String? = null,
    @SerialName("location") val location: String,
    @SerialName("specializations") val specializations: List<String> = emptyList(),
    @SerialName("yearsExperience") val yearsExperience: Int = 0,
    @SerialName("bio") val bio: String? = null,
    @SerialName("isVerified") val isVerified: Boolean = false,
    @SerialName("totalDeals") val totalDeals: Int = 0,
    @SerialName("avgRating") val avgRating: Double = 0.0,
    @SerialName("ratingCount") val ratingCount: Int = 0,
    @SerialName("createdAt") val createdAt: String,
) {
    val locationLabel: String get() = when (location) {
        "BOHRET_AMMAN" -> "بحرة عمان"
        "ZARQA" -> "الزرقاء"
        else -> location
    }
    val specializationLabels: List<String> get() = specializations.map { spec ->
        when (spec) {
            "CARS" -> "سيارات"
            "GOODS" -> "بضائع عامة"
            "CONTAINERS" -> "حاويات"
            "ALL" -> "الكل"
            else -> spec
        }
    }
    val isVerifiedBadge: Boolean get() = isVerified || totalDeals >= 10
    val ratingStars: String get() = "%.1f".format(avgRating)
}

@Serializable
data class PostClearanceRequestBody(
    @SerialName("serviceType") val serviceType: String,
    @SerialName("location") val location: String,
    @SerialName("description") val description: String,
    @SerialName("budgetMax") val budgetMax: Int? = null,
)

@Serializable
data class ClearanceOfferDto(
    @SerialName("id") val id: String,
    @SerialName("agentId") val agentId: String,
    @SerialName("price") val price: Int,
    @SerialName("notes") val notes: String? = null,
    @SerialName("isSelected") val isSelected: Boolean = false,
    @SerialName("placedAt") val placedAt: String,
    @SerialName("agent") val agent: ClearanceAgentDto? = null,
)

@Serializable
data class ClearanceCustomerDto(
    @SerialName("id") val id: String,
    @SerialName("fullName") val fullName: String? = null,
)

@Serializable
data class ClearanceRequestDto(
    @SerialName("id") val id: String,
    @SerialName("customerId") val customerId: String,
    @SerialName("serviceType") val serviceType: String,
    @SerialName("location") val location: String,
    @SerialName("description") val description: String,
    @SerialName("budgetMax") val budgetMax: Int? = null,
    @SerialName("status") val status: String,
    @SerialName("isRated") val isRated: Boolean = false,
    @SerialName("expiresAt") val expiresAt: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("offers") val offers: List<ClearanceOfferDto> = emptyList(),
    @SerialName("offersHidden") val offersHidden: Boolean = false,
    @SerialName("customer") val customer: ClearanceCustomerDto? = null,
) {
    val expiresAtMillis: Long get() = parseIsoMillis(expiresAt)
    val createdAtMillis: Long get() = parseIsoMillis(createdAt)
    val offerCount: Int get() = offers.size
    val lowestOffer: Int? get() = offers.filter { !it.isSelected || offers.none { o -> o.isSelected } }
        .minOfOrNull { it.price }
    val offersLockedUntilMillis: Long get() = createdAtMillis + 4L * 60 * 60 * 1000
    val serviceTypeLabel: String get() = when (serviceType) {
        "CARS" -> "تخليص سيارة"
        "GOODS" -> "بضاعة عامة"
        "CONTAINERS" -> "حاوية"
        "OTHER" -> "أخرى"
        else -> serviceType
    }
    val locationLabel: String get() = when (location) {
        "BOHRET_AMMAN" -> "بحرة عمان"
        "ZARQA" -> "الزرقاء"
        else -> location
    }

    fun myOffer(agentUserId: String): ClearanceOfferDto? =
        offers.firstOrNull { it.agent?.userId == agentUserId }

    fun isMyRequest(userId: String) = customerId == userId
}

@Serializable
data class SubmitOfferBody(
    @SerialName("price") val price: Int,
    @SerialName("notes") val notes: String? = null,
)

@Serializable
data class SelectAgentBody(
    @SerialName("agentId") val agentId: String,
)

@Serializable
data class RateAgentBody(
    @SerialName("score") val score: Int,
)

@Serializable
data class SelectAgentResultDto(
    @SerialName("closed") val closed: Boolean,
    @SerialName("winner") val winner: ClearanceAgentDto? = null,
)

@Serializable
data class RateAgentResultDto(
    @SerialName("rated") val rated: Boolean,
    @SerialName("tokensEarned") val tokensEarned: Int,
)
