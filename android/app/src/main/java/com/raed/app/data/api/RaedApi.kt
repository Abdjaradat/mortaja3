package com.raed.app.data.api

import com.raed.app.data.api.models.*
import retrofit2.Response
import retrofit2.http.*

interface RaedApi {

    // Auth
    @POST("auth/verify-firebase-token")
    suspend fun verifyFirebaseToken(@Body request: VerifyFirebaseTokenRequest): Response<AuthResponse>

    @POST("auth/refresh")
    suspend fun refreshToken(@Body request: RefreshTokenRequest): Response<AuthResponse>

    @POST("auth/logout")
    suspend fun logout(): Response<Unit>

    // Users
    @GET("users/me")
    suspend fun getMe(): Response<MeResponse>

    @PATCH("users/me")
    suspend fun updateMe(@Body request: UpdateProfileRequest): Response<MeResponse>

    @PATCH("users/me/fcm-token")
    suspend fun updateFcmToken(@Body request: FcmTokenRequest): Response<Unit>

    @POST("users/me/officer-profile")
    suspend fun submitOfficerProfile(@Body request: OfficerProfileRequest): Response<OfficerProfileStatusDto>

    @GET("users/me/officer-profile/status")
    suspend fun getOfficerProfileStatus(): Response<OfficerProfileStatusDto>

    @POST("users/me/medical-exempt-profile")
    suspend fun submitMedicalExemptProfile(@Body request: MedicalExemptProfileRequest): Response<MedicalExemptProfileStatusDto>

    @GET("users/me/medical-exempt-profile/status")
    suspend fun getMedicalExemptProfileStatus(): Response<MedicalExemptProfileStatusDto>

    // Listings
    @GET("listings")
    suspend fun getListings(
        @Query("governorate") governorate: String? = null,
        @Query("type") type: String? = null,
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 20,
    ): Response<ListingsResponse>

    @GET("listings/{id}")
    suspend fun getListingById(@Path("id") id: String): Response<ListingDto>

    @POST("listings")
    suspend fun createListing(@Body request: CreateListingRequest): Response<ListingDto>

    @POST("listings/{id}/save")
    suspend fun saveListing(@Path("id") id: String): Response<SavedResponse>

    @DELETE("listings/{id}/save")
    suspend fun unsaveListing(@Path("id") id: String): Response<Unit>

    @GET("listings/saved")
    suspend fun getSavedListings(): Response<List<ListingDto>>

    @POST("listings/{id}/reveal-contact")
    suspend fun revealContact(@Path("id") id: String): Response<RevealContactResponse>

    // Tokens
    @GET("tokens/balance")
    suspend fun getTokenBalance(): Response<TokenBalanceResponse>

    @POST("tokens/watch-ad")
    suspend fun watchAd(): Response<WatchAdResponse>

    @POST("tokens/spend")
    suspend fun spendTokens(@Body request: SpendTokenRequest): Response<SpendTokenResponse>

    // Conversations
    @GET("conversations")
    suspend fun getConversations(): Response<List<ConversationDto>>

    @POST("conversations")
    suspend fun startConversation(@Body request: StartConversationRequest): Response<ConversationDto>

    @GET("conversations/{id}/messages")
    suspend fun getMessages(@Path("id") id: String): Response<List<MessageDto>>

    @POST("conversations/{id}/messages")
    suspend fun sendMessage(@Path("id") id: String, @Body request: SendMessageRequest): Response<MessageDto>

    // Requests / Auction
    @POST("requests")
    suspend fun postRequest(@Body body: PostRequestBody): Response<RequestDto>

    @GET("requests")
    suspend fun getRequests(@Query("mine") mine: Boolean? = null): Response<List<RequestDto>>

    @GET("requests/{id}")
    suspend fun getRequest(@Path("id") id: String): Response<RequestDto>

    @POST("requests/{id}/bid")
    suspend fun placeBid(@Path("id") id: String, @Body body: BidBody): Response<BidDto>

    @POST("requests/{id}/close")
    suspend fun closeRequest(@Path("id") id: String, @Body body: CloseBody): Response<CloseResultDto>

    // Clearance Marketplace
    @POST("clearance/agents")
    suspend fun registerClearanceAgent(@Body body: RegisterAgentBody): Response<ClearanceAgentDto>

    @GET("clearance/agents")
    suspend fun getClearanceAgents(@Query("location") location: String? = null): Response<List<ClearanceAgentDto>>

    @GET("clearance/agents/me")
    suspend fun getMyClearanceAgent(): Response<ClearanceAgentDto>

    @GET("clearance/agents/{id}")
    suspend fun getClearanceAgent(@Path("id") id: String): Response<ClearanceAgentDto>

    @POST("clearance/requests")
    suspend fun postClearanceRequest(@Body body: PostClearanceRequestBody): Response<ClearanceRequestDto>

    @GET("clearance/requests")
    suspend fun getClearanceRequests(@Query("mine") mine: Boolean? = null): Response<List<ClearanceRequestDto>>

    @GET("clearance/requests/{id}")
    suspend fun getClearanceRequest(@Path("id") id: String): Response<ClearanceRequestDto>

    @POST("clearance/requests/{id}/offer")
    suspend fun submitClearanceOffer(@Path("id") id: String, @Body body: SubmitOfferBody): Response<ClearanceOfferDto>

    @POST("clearance/requests/{id}/select")
    suspend fun selectClearanceAgent(@Path("id") id: String, @Body body: SelectAgentBody): Response<SelectAgentResultDto>

    @POST("clearance/requests/{id}/rate")
    suspend fun rateClearanceAgent(@Path("id") id: String, @Body body: RateAgentBody): Response<RateAgentResultDto>
}

@kotlinx.serialization.Serializable
data class MeResponse(
    val id: String,
    val fullName: String? = null,
    val governorate: String? = null,
    val photoUrl: String? = null,
    val userType: String,
    val tokenBalance: Int = 0,
    val totalTokensEarned: Int = 0,
    val totalTokensSpent: Int = 0,
    val referralCode: String? = null,
    val officerProfile: OfficerProfileInfo? = null,
    val medicalExemptProfile: MedicalExemptProfileInfo? = null,
) {
    val isExemptVerified get() =
        officerProfile?.verificationState == "VERIFIED" ||
        medicalExemptProfile?.verificationState == "VERIFIED"
}

@kotlinx.serialization.Serializable
data class OfficerProfileInfo(
    val rank: String? = null,
    val status: String? = null,
    val verificationState: String,
    val exemptionUsed: Boolean = false,
)

@kotlinx.serialization.Serializable
data class MedicalExemptProfileInfo(
    val verificationState: String,
    val exemptionUsed: Boolean = false,
    val verifiedAt: String? = null,
)
