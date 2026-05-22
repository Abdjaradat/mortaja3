package com.raed.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.mock.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private val Gold = Color(0xFFC9A961)

@HiltViewModel
class BidViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val requestId: String = checkNotNull(savedStateHandle["requestId"])
    var bidInput by mutableStateOf("")
    var isSubmitting by mutableStateOf(false)
    var tokenBalance by mutableIntStateOf(500)
    var bidSubmitted by mutableStateOf(false)

    fun getRequest(): BuyerRequest? = MockRequestsSource.getById(requestId)

    fun minBid(): Int = (getRequest()?.highestBid ?: 0) + 1

    fun isInputValid(): Boolean {
        val v = bidInput.toIntOrNull() ?: return false
        return v >= minBid() && v <= tokenBalance
    }

    fun submit() {
        val tokens = bidInput.toIntOrNull() ?: return
        viewModelScope.launch {
            isSubmitting = true
            MockRequestsSource.placeBid(requestId, tokens)
            bidInput = ""
            bidSubmitted = true
            isSubmitting = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BidScreen(
    onBack: () -> Unit,
    viewModel: BidViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000L)
            tick = System.currentTimeMillis()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("قدّم عرضك") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        val request = viewModel.getRequest()

        if (request == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text("الطلب غير موجود")
            }
            return@Scaffold
        }

        val remaining = (request.endsAt - tick).coerceAtLeast(0L)
        val isExpired = remaining == 0L
        val hours = remaining / 3_600_000L
        val minutes = (remaining % 3_600_000L) / 60_000L
        val seconds = (remaining % 60_000L) / 1_000L

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // Request summary card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "تفاصيل الطلب",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    HorizontalDivider()
                    SummaryDetailRow("النوع", request.vehicleType.label)
                    SummaryDetailRow("الميزانية", "${request.budgetMin.toJod()} — ${request.budgetMax.toJod()}")
                    SummaryDetailRow("المحافظة", request.governorate)
                    if (request.notes.isNotBlank()) {
                        SummaryDetailRow("ملاحظات", request.notes)
                    }
                }
            }

            // Live stats row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Countdown tile
                Surface(
                    modifier = Modifier.weight(1f),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "⏱ الوقت المتبقي",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (isExpired) {
                            Text(
                                "انتهى",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(
                                "%02d:%02d:%02d".format(hours, minutes, seconds),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
                // Highest bid tile
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Gold.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            "🏆 أعلى عرض",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "${request.highestBid} 🪙",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gold,
                        )
                        Text(
                            "${request.bidCount} وسيط",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // My bid status (shown after submitting)
            if (viewModel.bidSubmitted) {
                val myBid = request.myBid
                if (myBid != null) {
                    val winning = request.amIWinning
                    Surface(
                        color = if (winning) Color(0xFF1B5E20).copy(alpha = 0.12f)
                        else Color(0xFFE65100).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(if (winning) "🏆" else "⚡", fontSize = 28.sp)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    if (winning) "أنت الأعلى عرضاً حالياً" else "هناك عرض أعلى منك",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (winning) Color(0xFF1B5E20) else Color(0xFFE65100),
                                )
                                Text(
                                    "عرضك: ${myBid.tokens} 🪙",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            if (!isExpired) {
                HorizontalDivider()

                // Escrow notice
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text("ℹ️", fontSize = 14.sp)
                        Text(
                            "سيتم خصم التوكنز فقط إذا فزت بالمزاد. رصيدك الحالي: ${viewModel.tokenBalance} 🪙",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }

                // Bid input
                val minBid = viewModel.minBid()
                OutlinedTextField(
                    value = viewModel.bidInput,
                    onValueChange = { viewModel.bidInput = it.filter { c -> c.isDigit() } },
                    label = { Text("عرضك بالتوكنز") },
                    placeholder = { Text("الحد الأدنى: $minBid 🪙") },
                    suffix = { Text("🪙") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = viewModel.bidInput.isNotBlank() && !viewModel.isInputValid(),
                    supportingText = {
                        val v = viewModel.bidInput.toIntOrNull()
                        when {
                            viewModel.bidInput.isBlank() -> {}
                            v == null -> Text("أدخل رقماً صحيحاً")
                            v < minBid -> Text("يجب أن يكون أعلى من ${minBid - 1} 🪙")
                            v > viewModel.tokenBalance -> Text("رصيدك غير كافٍ")
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                Button(
                    onClick = { viewModel.submit() },
                    enabled = viewModel.isInputValid() && !viewModel.isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                ) {
                    if (viewModel.isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        val label = if (request.myBid != null) "رفع العرض" else "قدّم العرض"
                        Text(
                            "$label — ${viewModel.bidInput.ifBlank { "?" }} 🪙",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            } else {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "انتهى وقت تقديم العروض على هذا الطلب",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
        )
    }
}
