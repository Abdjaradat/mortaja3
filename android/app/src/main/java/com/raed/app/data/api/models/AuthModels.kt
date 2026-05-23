package com.raed.app.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VerifyFirebaseTokenRequest(
    @SerialName("idToken") val idToken: String,
    @SerialName("userType") val userType: String? = null,
)

@Serializable
data class AuthResponse(
    @SerialName("accessToken") val accessToken: String,
    @SerialName("refreshToken") val refreshToken: String,
    @SerialName("user") val user: AuthUserDto,
)

@Serializable
data class AuthUserDto(
    @SerialName("id") val id: String,
    @SerialName("userType") val userType: String,
    @SerialName("fullName") val fullName: String? = null,
    @SerialName("isNewUser") val isNewUser: Boolean,
)

@Serializable
data class RefreshTokenRequest(
    @SerialName("refreshToken") val refreshToken: String,
)

@Serializable
data class UpdateProfileRequest(
    @SerialName("fullName") val fullName: String? = null,
    @SerialName("governorate") val governorate: String? = null,
    @SerialName("phoneNumber") val phoneNumber: String? = null,
)

@Serializable
data class FcmTokenRequest(
    @SerialName("token") val token: String,
)
