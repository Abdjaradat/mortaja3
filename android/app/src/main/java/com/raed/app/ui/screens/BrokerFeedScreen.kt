package com.raed.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raed.app.data.mock.*
import kotlinx.coroutines.delay

private val Gold = Color(0xFFC9A961)

@Composable
fun BrokerFeedContent(
    onRequestClick: (id: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val requests = MockRequestsSource.getAll()
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            tick = System.currentTimeMillis()
        }
    }

    if (requests.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("🔨", fontSize = 48.sp)
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
    } else {
        LazyColumn(
            modifier = modifier,
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
                        Text("🔨", fontSize = 20.sp)
                        Text(
                            "قدّم أعلى عرض بالتوكنز لتحصل على بيانات المشتري",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            items(requests, key = { it.id }) { request ->
                RequestCard(
                    request = request,
                    tick = tick,
                    onBidClick = { onRequestClick(request.id) },
                )
            }
        }
    }
}

@Composable
private fun RequestCard(
    request: BuyerRequest,
    tick: Long,
    onBidClick: () -> Unit,
) {
    val remaining = (request.endsAt - tick).coerceAtLeast(0L)
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
                            request.vehicleType.label,
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

            // Budget
            Text(
                "الميزانية: ${request.budgetMin.toJod()} — ${request.budgetMax.toJod()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (request.notes.isNotBlank()) {
                Text(
                    request.notes,
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

                val myBid = request.myBid
                if (myBid != null) {
                    Surface(
                        color = if (request.amIWinning)
                            Color(0xFF1B5E20).copy(alpha = 0.15f)
                        else
                            Color(0xFFE65100).copy(alpha = 0.12f),
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Text(
                            if (request.amIWinning) "أنت الأعلى 🏆" else "هناك عرض أعلى ⚡",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (request.amIWinning) Color(0xFF1B5E20) else Color(0xFFE65100),
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

            // Already bid — show current bid + upgrade link
            val myBid = request.myBid
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
