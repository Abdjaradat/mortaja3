package com.raed.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.MessageDto
import com.raed.app.data.api.models.SendMessageRequest
import com.raed.app.data.local.SessionDataStore
import com.raed.app.utils.toTimeAgo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@HiltViewModel
class ConversationViewModel @Inject constructor(
    private val api: RaedApi,
    private val session: SessionDataStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val conversationId: String = checkNotNull(savedStateHandle["conversationId"])

    var messages by mutableStateOf<List<MessageDto>>(emptyList())
        private set
    var currentUserId by mutableStateOf<String?>(null)
        private set
    var otherUserPhotoUrl by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var isSending by mutableStateOf(false)
        private set
    var messageText by mutableStateOf("")

    init { load() }

    private fun load() {
        viewModelScope.launch {
            isLoading = true
            currentUserId = session.userId.firstOrNull()
            runCatching { api.getMessages(conversationId) }
                .onSuccess { r -> if (r.isSuccessful) messages = r.body()!! }
            runCatching { api.getConversations() }
                .onSuccess { r ->
                    if (r.isSuccessful) {
                        val conv = r.body()?.firstOrNull { it.id == conversationId }
                        val other = if (conv?.user1Id == currentUserId) conv?.user2 else conv?.user1
                        otherUserPhotoUrl = other?.photoUrl
                    }
                }
            isLoading = false
        }
    }

    fun send() {
        val text = messageText.trim()
        if (text.isBlank()) return
        messageText = ""
        viewModelScope.launch {
            isSending = true
            runCatching { api.sendMessage(conversationId, SendMessageRequest(content = text)) }
                .onSuccess { r ->
                    if (r.isSuccessful) {
                        val newMsg = r.body()!!
                        messages = messages + newMsg
                    } else {
                        messageText = text
                    }
                }
                .onFailure { messageText = text }
            isSending = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    otherUserName: String,
    onBack: () -> Unit,
    viewModel: ConversationViewModel = hiltViewModel(),
) {
    val listState = rememberLazyListState()

    LaunchedEffect(viewModel.messages.size) {
        if (viewModel.messages.isNotEmpty()) listState.animateScrollToItem(viewModel.messages.size - 1)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(
                            modifier = Modifier.size(36.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            val photoUrl = viewModel.otherUserPhotoUrl
                            if (photoUrl != null) {
                                AsyncImage(
                                    model = photoUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop,
                                )
                            } else {
                                Text(
                                    otherUserName.firstOrNull()?.toString() ?: "؟",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                        Text(
                            otherUserName.ifBlank { "المحادثة" },
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = viewModel.messageText,
                        onValueChange = { viewModel.messageText = it },
                        placeholder = { Text("اكتب رسالة…") },
                        modifier = Modifier.weight(1f),
                        maxLines = 4,
                        shape = MaterialTheme.shapes.extraLarge,
                    )
                    FilledIconButton(
                        onClick = { viewModel.send() },
                        enabled = viewModel.messageText.isNotBlank() && !viewModel.isSending,
                    ) {
                        if (viewModel.isSending) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.AutoMirrored.Outlined.Send, contentDescription = "إرسال")
                        }
                    }
                }
            }
        },
    ) { padding ->
        when {
            viewModel.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            viewModel.messages.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("لا توجد رسائل بعد. ابدأ المحادثة!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(viewModel.messages, key = { it.id }) { message ->
                        MessageBubble(message = message, isMe = message.senderId == viewModel.currentUserId)
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageDto, isMe: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = if (isMe) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.widthIn(max = 280.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = message.createdAt.toTimeAgo(),
                    style = MaterialTheme.typography.labelSmall,
                    color = (if (isMe) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant).copy(alpha = 0.6f),
                )
            }
        }
    }
}
