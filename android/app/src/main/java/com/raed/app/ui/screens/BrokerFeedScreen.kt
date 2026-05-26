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
private fun Int.toJod() = "%,d د.أ".format(this)

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
                    error = "خطأ في تحميل الطلبات"
                }
            } catch (e: Exception) {
                error = "تعذّر الاتصال بالخادم"
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
                            "كيف يعمل المزاد؟ 💡",
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
                            text = "١. المشتري ينشر طلبه – نوع السيارة وميزانيته\n" +
                                    "٢. أنت تقدّم عرضك بعدد التوكنز – كلما زاد عرضك زادت أولويتك\n" +
                                    "٣. بعد 24 ساعة، يختار المشتري الوسيط المناسب\n" +
                                    "٤. الفائز يحصل على رقم المشتري – الخاسرون يستردون توكنزهم\n\n" +
                                    "⚠️ ملاحظة: التوكنز تُخصم عند تقديم العرض وتُعاد إذا لم تفز",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        )
                    }
                }
            }

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
                            Text("⚠️", fontSize = 40.sp)
                            Text(
                                viewModel.error ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                            OutlinedButton(onClick = { viewModel.refresh() }) {
                                Text("إعادة المحاولة")
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
                            Text("📨", fontSize = 48.sp)
                            Text(
                                "لا توجد طلبات حالياً",
                                style = MaterialTheme.typography.titleMedium,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                "سيظهر هنا طلبات المشترين بمجرد نشرها",
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
                                    Text("📨", fontSize = 20.sp)
                                    Text(
                                        "قدّم أعلى عرض بالتوكنز لتحصل على بيانات المشتري",
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
                        "📍 ${request.governorate}",
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
                            "انتهى",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                } else {
                    Text(
                        "⏱ %02d:%02d:%02d".format(hours, minutes, seconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = timeColor,
                        fontFamily = FontFamily.Monospace,
                    )
                }
            }

            Text(
                "الميزانية: ${request.budgetMin.toJod()} – ${request.budgetMax.toJod()}",
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "أعلى عرض: ${request.highestBid} 🪙",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Gold,
                    )
                    Text(
                        "${request.bidCount} وسيط قدّم عرضاً",
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
                            if (amIWinning) "أنت الأعلى 🏆" else "هناك عرض أعلى ⚡",
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
                            "قدّم عرضك",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (myBid != null && !isExpired) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "عرضك الحالي: ${myBid.tokens} 🪙",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(
                        onClick = onBidClick,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    ) {
                        Text(
                            "رفع العرض",
                            style = MaterialTheme.typography.labelSmall,
                            color = Gold,
                        )
                    }
                }
            }
        }
    }
}
