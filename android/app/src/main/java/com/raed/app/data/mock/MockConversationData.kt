package com.raed.app.data.mock

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

data class MockMessage(
    val id: String,
    val senderId: String,
    val content: String,
    val timeAgo: String,
)

data class MockConversation(
    val id: String,
    val otherUserName: String,
    val listingTitle: String,
    val messages: SnapshotStateList<MockMessage> = mutableStateListOf(),
    val timeAgo: String = "الآن",
)

object MockConversationsSource {

    private val _conversations = mutableStateListOf<MockConversation>()

    fun getAll(): List<MockConversation> = _conversations

    fun getById(id: String): MockConversation? = _conversations.find { it.id == id }

    fun startConversation(listingTitle: String, otherUserName: String): String {
        val existing = _conversations.find { it.listingTitle == listingTitle }
        if (existing != null) return existing.id
        val id = "conv-${System.currentTimeMillis()}"
        val greeting = mutableStateListOf(
            MockMessage(
                id = "msg-0",
                senderId = "other",
                content = "مرحباً، كيف أقدر أساعدك؟",
                timeAgo = "الآن",
            )
        )
        _conversations.add(
            0,
            MockConversation(
                id = id,
                otherUserName = otherUserName,
                listingTitle = listingTitle,
                messages = greeting,
            )
        )
        return id
    }

    fun sendMessage(conversationId: String, content: String) {
        _conversations.find { it.id == conversationId }?.messages?.add(
            MockMessage(
                id = "msg-${System.currentTimeMillis()}",
                senderId = "me",
                content = content,
                timeAgo = "الآن",
            )
        )
    }
}
