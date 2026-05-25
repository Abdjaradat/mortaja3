package com.raed.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.ConversationDto
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
class ConversationsViewModel @Inject constructor(
    private val api: RaedApi,
    private val session: SessionDataStore,
) : ViewModel() {

    var conversations by mutableStateOf<List<ConversationDto>>(emptyList())
        private set
    var currentUserId by mutableStateOf<String?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set

    init { load() }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            currentUserId = session.userId.firstOrNull()
            runCatching { api.getConversations() }
                .onSuccess { r -> if (r.isSuccessful) conversations = r.body()!! }
            isLoading = false
        }
    }

    fun otherUserName(conv: ConversationDto): String {
        val other = if (conv.user1Id == currentUserId) conv.user2 else conv.user1
        return other?.fullName ?: "مستخدم"
    }

    fun otherUserInitial(conv: ConversationDto): String {
        val other = if (conv.user1Id == currentUserId) conv.user2 else conv.user1
        return other?.fullName?.firstOrNull()?.toString() ?: "؟"
    }

    fun otherUserPhotoUrl(conv: ConversationDto): String? {
        val other = if (conv.user1Id == currentUserId) conv.user2 else conv.user1
        return other?.photoUrl
    }
}

@Composable
fun ConversationsContent(
    onConversationClick: (id: String, name: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ConversationsViewModel = hiltViewModel(),
) {
    Box(modifier = modifier.fillMaxSize()) {
        when {
            viewModel.isLoading -> {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
            viewModel.conversations.isEmpty() -> {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("💬", fontSize = 48.sp)
                    Text("لا توجد محادثات بعد", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "ابدأ محادثة من صفحة أي إعلان",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            else -> {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(viewModel.conversations, key = { it.id }) { conv ->
                        val name = viewModel.otherUserName(conv)
                        val initial = viewModel.otherUserInitial(conv)
                        val photoUrl = viewModel.otherUserPhotoUrl(conv)
                        val lastMsg = conv.messages.firstOrNull()
                        ListItem(
                            modifier = Modifier.clickable { onConversationClick(conv.id, name) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (photoUrl != null) {
                                        AsyncImage(
                                            model = photoUrl,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Text(
                                            initial,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        )
                                    }
                                }
                            },
                            headlineContent = {
                                Text(name, fontWeight = FontWeight.SemiBold)
                            },
                            supportingContent = {
                                if (lastMsg != null) {
                                    Text(
                                        lastMsg.content,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            },
                            trailingContent = {
                                Text(
                                    conv.updatedAt.toTimeAgo(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            },
                        )
                        HorizontalDivider(modifier = Modifier.padding(start = 72.dp))
                    }
                }
            }
        }
    }
}
