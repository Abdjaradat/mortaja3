package com.raed.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import com.raed.app.ui.components.UnityBannerAd
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.BidDto
import com.raed.app.data.api.models.RequestDto
import com.raed.app.data.local.SessionDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private val Gold = Color(0xFFC9A961)
private fun Int.toJod() = "%,d Ø¯.Ø£".format(this)

@HiltViewModel
class BrokerFeedViewModel @Inject constructor(
    private val api: RaedApi,
    private val sessionDataStore: SessionDataStore,
) : ViewModel() {
    var requests by mutableStateOf<List<RequestDto>>(emptyList())
    var isLoading by mutableStateOf(false)
    var isRefreshing by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var currentUserId by mutableStateOf<String?>(null)

    init {
        refresh()
    }

    fun refresh(isManual: Boolean = false) {
        viewModelScope.launch {
            if (isManual) isRefreshing = true else if (requests.isEmpty()) isLoading = true
            error = null
            try {
                if (currentUserId == null) {
                    currentUserId = sessionDataStore.userId.firstOrNull()
                }
                val response = api.getRequests(mine = false)
                if (response.isSuccessful) {
                    requests = response.body() ?: emptyList()
                } else {
                    error = "Ø®Ø·Ø£ ÙÙŠ ØªØ­Ù…ÙŠÙ„ Ø§Ù„Ø·Ù„Ø¨Ø§Øª"
                }
            } catch (e: Exception) {
                error = "ØªØ¹Ø°Ù‘Ø± Ø§Ù„Ø§ØªØµØ§Ù„ Ø¨Ø§Ù„Ø®Ø§Ø¯Ù…"
            } finally {
                isLoading = false
                isRefreshing = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrokerFeedContent(
    onRequestClick: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: BrokerFeedViewModel = hiltViewModel(),
) {
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var infoExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            tick = System.currentTimeMillis()
        }
    }

    PullToRefreshBox(
        isRefreshing = viewModel.isRefreshing,
        onRefresh = { viewModel.refresh(isManual = true) },
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // â”€â”€ Collapsible info card â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { infoExpanded = !infoExpanded }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "ÙƒÙŠÙ ÙŠØ¹Ù…Ù„ Ø§Ù„Ù…Ø²Ø§Ø¯ØŸ ðŸ’¡",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Icon(
                            imageVector = if (infoExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    AnimatedVisibility(visible = infoExpanded) {
                        Text(
                            text = "Ù¡. Ø§Ù„Ù…Ø´ØªØ±ÙŠ ÙŠÙ†Ø´Ø± Ø·Ù„Ø¨Ù‡ â€” Ù†ÙˆØ¹ Ø§Ù„Ø³ÙŠØ§Ø±Ø© ÙˆÙ…ÙŠØ²Ø§Ù†ÙŠØªÙ‡\n" +
                                    "Ù¢. Ø£Ù†Øª ØªÙ‚Ø¯Ù‘Ù… Ø¹Ø±Ø¶Ùƒ Ø¨Ø¹Ø¯Ø¯ Ø§Ù„ØªÙˆÙƒÙ†Ø² â€” ÙƒÙ„Ù…Ø§ Ø²Ø§Ø¯ Ø¹Ø±Ø¶Ùƒ Ø²Ø§Ø¯Øª Ø£ÙˆÙ„ÙˆÙŠØªÙƒ\n" +
                                    "Ù£. Ø¨Ø¹Ø¯ 24 Ø³Ø§Ø¹Ø©ØŒ ÙŠØ®ØªØ§Ø± Ø§Ù„Ù…Ø´ØªØ±ÙŠ Ø§Ù„ÙˆØ³ÙŠØ· Ø§Ù„Ù…Ù†Ø§Ø³Ø¨\n" +
                                    "Ù¤. Ø§Ù„ÙØ§Ø¦Ø² ÙŠØ­ØµÙ„ Ø¹Ù„Ù‰ Ø±Ù‚Ù… Ø§Ù„Ù…Ø´ØªØ±ÙŠ â€” Ø§Ù„Ø®Ø§Ø³Ø±ÙˆÙ† ÙŠØ³ØªØ±Ø¯ÙˆÙ† ØªÙˆÙƒÙ†Ø²Ù‡Ù…\n\n" +
                                    "âš ï¸ Ù…Ù„Ø§Ø­Ø¸Ø©: Ø§Ù„ØªÙˆÙƒÙ†Ø² ØªÙØ®ØµÙ… Ø¹Ù†Ø¯ ØªÙ‚Ø¯ÙŠÙ… Ø§Ù„Ø¹Ø±Ø¶ ÙˆØªÙØ¹Ø§Ø¯ Ø¥Ø°Ø§ Ù„Ù… ØªÙØ²",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        )
                    }
                }
            }

            // â”€â”€ Content states â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
            when {
                viewModel.isLoading -> {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                viewModel.error != null -> {
                    Box(
                        modifier = Modifier.weight(1f).padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("âš ï¸", fontSize = 40.sp)
                            Text(
                                viewModel.error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                            OutlinedButton(onClick = { viewModel.refresh() }) {
                                Text("Ø¥Ø¹Ø§Ø¯Ø© Ø§Ù„Ù…Ø­Ø§ÙˆÙ„Ø©")
                            }
                        }
                    }
                }

                viewModel.requests.isEmpty() -> {
                    Box(
                        modifier = Modifier.weight(1f).padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text("ðŸ”¨", fontSize = 48.sp)
                            Text(
                                "Ù„Ø§ ØªÙˆØ¬Ø¯ Ø·Ù„Ø¨Ø§Øª Ø­Ø§Ù„ÙŠØ§Ù‹",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "Ø³ÙŠØ¸Ù‡Ø± Ù‡Ù†Ø§ Ø·Ù„Ø¨Ø§Øª Ø§Ù„Ù…Ø´ØªØ±ÙŠÙ† Ø¨Ù…Ø¬Ø±Ø¯ Ù†Ø´Ø±Ù‡Ø§",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        item(key = "feed_header") {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text("ðŸ”¨", fontSize = 20.sp)
                                    Text(
                                        "Ù‚Ø¯Ù‘Ù… Ø£Ø¹Ù„Ù‰ Ø¹Ø±Ø¶ Ø¨Ø§Ù„ØªÙˆÙƒÙ†Ø² Ù„ØªØ­ØµÙ„ Ø¹Ù„Ù‰ Ø¨ÙŠØ§Ù†Ø§Øª Ø§Ù„Ù…Ø´ØªØ±ÙŠ",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                        items(viewModel.requests, key = { it.id }) { request ->
                            RequestCard(
                                request = request,
                                currentUserId = viewModel.currentUserId ?: "",
                                tick = tick,
                                onBidClick = { onRequestClick(request.id) },
                            )
                        }
                    }
                }
            }
            UnityBannerAd()
        }
    }
}

@Composable
private fun RequestCard(
    request: RequestDto,
    currentUserId: String,
    tick: Long,
    onBidClick: () -> Unit,
) {
    val remaining = (request.expiresAtMillis - tick).coerceAtLeast(0L)
    val isExpired = remaining == 0L
    val hours = remaining / 3_600_000L
    val minutes = (remaining % 3_600_000L) / 60_000L
    val seconds = (remaining % 60_000L) / 1_000L

    val timeColor = when {
        isExpired -> MaterialTheme.colorScheme.error
        hours < 3 -> Color(0xFFBF360C)
        hours < 12 -> Color(0xFFE65100)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val myBid: BidDto? = if (currentUserId.isNotEmpty()) request.myBid(currentUserId) else null
    val amIWinning = currentUserId.isNotEmpty() && request.amIWinning(currentUserId)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header row: type chip + governorate + countdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            request.vehicleTypeLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Text(
                        "ðŸ“ ${request.governorate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (isExpired) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            "Ø§Ù†ØªÙ‡Ù‰",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                } else {
                    Text(
                        "â± %02d:%02d:%02d".format(hours, minutes, seconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = timeColor,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            // Budget
            Text(
                "Ø§Ù„Ù…ÙŠØ²Ø§Ù†ÙŠØ©: ${request.budgetMin.toJod()} â€” ${request.budgetMax.toJod()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            val notes = request.notes
            if (!notes.isNullOrBlank()) {
                Text(
                    notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()

            // Bid stats + action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "Ø£Ø¹Ù„Ù‰ Ø¹Ø±Ø¶: ${request.highestBid} ðŸª™",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Gold,
                    )
                    Text(
                        "${request.bidCount} ÙˆØ³ÙŠØ· Ù‚Ø¯Ù‘Ù… Ø¹Ø±Ø¶Ø§Ù‹",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (myBid != null) {
                    Surface(
                        color = if (amIWinning)
                            Color(0xFF1B5E20).copy(alpha = 0.15f)
                        else
                            Color(0xFFE65100).copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            if (amIWinning) "Ø£Ù†Øª Ø§Ù„Ø£Ø¹Ù„Ù‰ ðŸ†" else "Ù‡Ù†Ø§Ùƒ Ø¹Ø±Ø¶ Ø£Ø¹Ù„Ù‰ âš¡",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (amIWinning) Color(0xFF1B5E20) else Color(0xFFE65100),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                } else {
                    Button(
                        onClick = onBidClick,
                        enabled = !isExpired,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                    ) {
                        Text(
                            "Ù‚Ø¯Ù‘Ù… Ø¹Ø±Ø¶Ùƒ",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Already bid â€” show current bid + upgrade link
            if (myBid != null && !isExpired) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Ø¹Ø±Ø¶Ùƒ Ø§Ù„Ø­Ø§Ù„ÙŠ: ${myBid.tokens} ðŸª™",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onBidClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            "Ø±ÙØ¹ Ø§Ù„Ø¹Ø±Ø¶",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold,
                        )
                    }
                }
            }
        }
    }
}

