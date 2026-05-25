package com.raed.app.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListingDto(
    @SerialName("id") val id: String,
    @SerialName("vehicleType") val vehicleType: String,
    @SerialName("makeModel") val makeModel: String,
    @SerialName("yearMin") val yearMin: Int? = null,
    @SerialName("yearMax") val yearMax: Int? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("mileageKm") val mileageKm: Int? = null,
    @SerialName("fuelType") val fuelType: String? = null,
    @SerialName("transmission") val transmission: String? = null,
    @SerialName("phoneNumber") val phoneNumber: String? = null,
    @SerialName("marketPrice") val marketPrice: Int? = null,
    @SerialName("expectedPrice") val expectedPrice: Int? = null,
    @SerialName("listingType") val listingType: String,
    @SerialName("listingCategory") val listingCategory: String = "MORTAJA3",
    @SerialName("sellerType") val sellerType: String? = null,
    @SerialName("restrictionEndsAt") val restrictionEndsAt: String? = null,
    @SerialName("originalPrice") val originalPrice: Int? = null,
    @SerialName("tier") val tier: String = "FREE",
    @SerialName("tierExpiresAt") val tierExpiresAt: String? = null,
    @SerialName("governorate") val governorate: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("photos") val photos: List<String> = emptyList(),
    @SerialName("status") val status: String = "ACTIVE",
    @SerialName("viewCount") val viewCount: Int = 0,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("officer") val officer: OfficerSummaryDto? = null,
) {
    val isOwned get() = listingType == "OWNED"
    val isMortaja3 get() = listingCategory == "MORTAJA3"
    val isRegular get() = listingCategory == "REGULAR"
    val isExemptionRight get() = listingCategory == "EXEMPTION_RIGHT"
    val isBoosted get() = tier != "FREE"
    val isVerified get() = officer?.officerProfile?.verificationState == "VERIFIED"
    val displayYear get() = yearMin?.toString() ?: yearMax?.toString() ?: ""
    val fuelTypeLabel get() = when (fuelType) {
        "GASOLINE" -> "بنزين"
        "DIESEL" -> "ديزل"
        "HYBRID" -> "هايبرد"
        "ELECTRIC" -> "كهربائي"
        else -> fuelType ?: ""
    }
    val transmissionLabel get() = when (transmission) {
        "AUTOMATIC" -> "أوتوماتيك"
        "MANUAL" -> "يدوي"
        else -> transmission ?: ""
    }
}

@Serializable
data class OfficerSummaryDto(
    @SerialName("id") val id: String,
    @SerialName("fullName") val fullName: String? = null,
    @SerialName("governorate") val governorate: String? = null,
    @SerialName("photoUrl") val photoUrl: String? = null,
    @SerialName("officerProfile") val officerProfile: OfficerProfileSummaryDto? = null,
)

@Serializable
data class OfficerProfileSummaryDto(
    @SerialName("verificationState") val verificationState: String,
    @SerialName("rank") val rank: String? = null,
)

@Serializable
data class ListingsResponse(
    @SerialName("listings") val listings: List<ListingDto>,
    @SerialName("total") val total: Int,
    @SerialName("page") val page: Int,
    @SerialName("limit") val limit: Int,
)

@Serializable
data class CreateListingRequest(
    @SerialName("vehicleType") val vehicleType: String,
    @SerialName("makeModel") val makeModel: String,
    @SerialName("yearMin") val yearMin: Int? = null,
    @SerialName("yearMax") val yearMax: Int? = null,
    @SerialName("color") val color: String? = null,
    @SerialName("mileageKm") val mileageKm: Int? = null,
    @SerialName("fuelType") val fuelType: String? = null,
    @SerialName("transmission") val transmission: String? = null,
    @SerialName("phoneNumber") val phoneNumber: String? = null,
    @SerialName("marketPrice") val marketPrice: Int? = null,
    @SerialName("expectedPrice") val expectedPrice: Int? = null,
    @SerialName("listingType") val listingType: String,
    @SerialName("listingCategory") val listingCategory: String? = null,
    @SerialName("sellerType") val sellerType: String? = null,
    @SerialName("restrictionEndsAt") val restrictionEndsAt: String? = null,
    @SerialName("originalPrice") val originalPrice: Int? = null,
    @SerialName("governorate") val governorate: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("photos") val photos: List<String> = emptyList(),
)

@Serializable
data class SavedResponse(
    @SerialName("saved") val saved: Boolean,
)

@Serializable
data class OfficerProfileRequest(
    @SerialName("rank") val rank: String,
    @SerialName("status") val status: String,
    @SerialName("documentUrl") val documentUrl: String,
)

@Serializable
data class MedicalExemptProfileRequest(
    @SerialName("documentUrl") val documentUrl: String,
)

@Serializable
data class OfficerProfileStatusDto(
    @SerialName("verificationState") val verificationState: String,
    @SerialName("rejectionReason") val rejectionReason: String? = null,
    @SerialName("verifiedAt") val verifiedAt: String? = null,
)

@Serializable
data class RevealContactResponse(
    @SerialName("phoneNumber") val phoneNumber: String,
    @SerialName("charged") val charged: Boolean = true,
)
