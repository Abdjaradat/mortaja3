package com.raed.app.data.api.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ConversationDto(
    @SerialName("id") val id: String,
    @SerialName("user1Id") val user1Id: String,
    @SerialName("user2Id") val user2Id: String,
    @SerialName("user1") val user1: UserSummaryDto? = null,
    @SerialName("user2") val user2: UserSummaryDto? = null,
    @SerialName("listingId") val listingId: String? = null,
    @SerialName("updatedAt") val updatedAt: String,
    @SerialName("messages") val messages: List<MessageSummaryDto> = emptyList(),
)

@Serializable
data class UserSummaryDto(
    @SerialName("id") val id: String,
    @SerialName("fullName") val fullName: String? = null,
    @SerialName("photoUrl") val photoUrl: String? = null,
)

@Serializable
data class MessageDto(
    @SerialName("id") val id: String,
    @SerialName("content") val content: String,
    @SerialName("senderId") val senderId: String,
    @SerialName("isRead") val isRead: Boolean,
    @SerialName("createdAt") val createdAt: String,
)

@Serializable
data class MessageSummaryDto(
    @SerialName("content") val content: String,
    @SerialName("createdAt") val createdAt: String,
    @SerialName("isRead") val isRead: Boolean,
    @SerialName("senderId") val senderId: String,
)

@Serializable
data class SendMessageRequest(
    @SerialName("content") val content: String,
)

@Serializable
data class StartConversationRequest(
    @SerialName("otherUserId") val otherUserId: String,
    @SerialName("listingId") val listingId: String? = null,
)
