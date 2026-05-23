package com.raed.app.ui.screens.clearance

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
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.*
import com.raed.app.data.local.SessionDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private val Gold = Color(0xFFC9A961)
private fun Int.toJod() = "%,d د.أ".format(this)

@HiltViewModel
class ClearanceRequestDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val api: RaedApi,
    private val sessionDataStore: SessionDataStore,
) : ViewModel() {
    val requestId: String = checkNotNull(savedStateHandle["requestId"])
    var request by mutableStateOf<ClearanceRequestDto?>(null)
    var currentUserId by mutableStateOf<String?>(null)
    var myAgentProfile by mutableStateOf<ClearanceAgentDto?>(null)
    var tokenBalance by mutableIntStateOf(0)
    var isLoading by mutableStateOf(true)
    var isSubmitting by mutableStateOf(false)
    var isSelecting by mutableStateOf(false)
    var isRating by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var message by mutableStateOf<String?>(null)
    var winnerAgent by mutableStateOf<ClearanceAgentDto?>(null)

    // Offer form state
    var priceInput by mutableStateOf("")
    var notesInput by mutableStateOf("")

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                if (currentUserId == null) currentUserId = sessionDataStore.userId.firstOrNull()
                val reqDef   = async { api.getClearanceRequest(requestId) }
                val agentDef = async { api.getMyClearanceAgent() }
                val balDef   = async { api.getTokenBalance() }
                reqDef.await().body()?.let { request = it }
                agentDef.await().let { if (it.isSuccessful) myAgentProfile = it.body() }
                balDef.await().let { if (it.isSuccessful) tokenBalance = it.body()?.tokenBalance ?: 0 }
            } catch (e: Exception) {
                error = "تعذّر تحميل البيانات"
            } finally {
                isLoading = false
            }
        }
    }

    fun isOfferInputValid(): Boolean {
        val price = priceInput.toIntOrNull() ?: return false
        return price > 0 && price <= tokenBalance
    }

    fun submitOffer() {
        val price = priceInput.toIntOrNull() ?: return
        viewModelScope.launch {
            isSubmitting = true
            error = null
            try {
                val resp = api.submitClearanceOffer(requestId, SubmitOfferBody(price, notesInput.trim().ifBlank { null }))
                when {
                    resp.isSuccessful -> {
                        message = "تم تقديم عرضك بنجاح ✅"
                        priceInput = ""
                        notesInput = ""
                        // Reload request + balance
                        api.getClearanceRequest(requestId).body()?.let { request = it }
                        api.getTokenBalance().body()?.let { tokenBalance = it.tokenBalance }
                    }
                    resp.code() == 402 -> error = "رصيد توكنز غير كافٍ"
                    else -> error = "حدث خطأ أثناء تقديم العرض"
                }
            } catch (e: Exception) {
                error = "تعذّر الاتصال بالخادم"
            } finally {
                isSubmitting = false
            }
        }
    }

    fun selectAgent(agentId: String) {
        viewModelScope.launch {
            isSelecting = true
            error = null
            try {
                val resp = api.selectClearanceAgent(requestId, SelectAgentBody(agentId))
                if (resp.isSuccessful) {
                    winnerAgent = resp.body()?.winner
                    message = "تم اختيار المخلص! 🎉"
                    api.getClearanceRequest(requestId).body()?.let { request = it }
                } else if (resp.code() == 402) {
                    error = "رصيد المخلص غير كافٍ، جرّب مخلصاً آخر"
                } else {
                    error = "حدث خطأ"
                }
            } catch (e: Exception) {
                error = "تعذّر الاتصال بالخادم"
            } finally {
                isSelecting = false
            }
        }
    }

    fun rateAgent(score: Int) {
        viewModelScope.launch {
            isRating = true
            error = null
            try {
                val resp = api.rateClearanceAgent(requestId, RateAgentBody(score))
                if (resp.isSuccessful) {
                    message = "شكراً على تقييمك! حصلت على 20 🪙"
                    api.getClearanceRequest(requestId).body()?.let { request = it }
                } else {
                    error = "حدث خطأ أثناء التقييم"
                }
            } catch (e: Exception) {
                error = "تعذّر الاتصال بالخادم"
            } finally {
                isRating = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClearanceRequestDetailScreen(
    onBack: () -> Unit,
    onAgentClick: (agentId: String) -> Unit = {},
    viewModel: ClearanceRequestDetailViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    var tick by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) { while (true) { delay(1000L); tick = System.currentTimeMillis() } }

    LaunchedEffect(viewModel.error) {
        val msg = viewModel.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg); viewModel.error = null
    }
    LaunchedEffect(viewModel.message) {
        val msg = viewModel.message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg); viewModel.message = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("تفاصيل الطلب") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        when {
            viewModel.isLoading -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                CircularProgressIndicator()
            }
            viewModel.request == null -> Box(Modifier.fillMaxSize().padding(padding), Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("الطلب غير موجود")
                    OutlinedButton(onClick = { viewModel.loadAll() }) { Text("إعادة المحاولة") }
                }
            }
            else -> {
                val request = viewModel.request!!
                val currentUserId = viewModel.currentUserId ?: ""
                val isMyRequest = request.isMyRequest(currentUserId)
                val myAgentUserId = viewModel.myAgentProfile?.userId ?: ""
                val myOffer = if (myAgentUserId.isNotEmpty()) request.myOffer(myAgentUserId) else null
                val remaining = (request.expiresAtMillis - tick).coerceAtLeast(0L)
                val isExpired = remaining == 0L || request.status == "CLOSED"
                val hours = remaining / 3_600_000L
                val minutes = (remaining % 3_600_000L) / 60_000L
                val seconds = (remaining % 60_000L) / 1_000L

                Column(
                    modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
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
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                                Text("تفاصيل الطلب", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                    Text(request.serviceTypeLabel, Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                            }
                            HorizontalDivider()
                            SummaryRow("الموقع", "📍 ${request.locationLabel}")
                            SummaryRow("الوصف", request.description)
                            request.budgetMax?.let { SummaryRow("الميزانية", "حتى ${it.toJod()}") }
                            if (request.status == "CLOSED") {
                                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()) {
                                    Text("✅ الطلب مغلق", Modifier.padding(10.dp),
                                        style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                        }
                    }

                    // Countdown (only if open)
                    if (!isExpired || remaining > 0L) {
                        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.padding(12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                                Text("⏱ الوقت المتبقي", style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    if (isExpired) "انتهى" else "%02d:%02d:%02d".format(hours, minutes, seconds),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    // ─── Customer view: offers section ─────────────────────────
                    if (isMyRequest) {
                        CustomerOffersSection(
                            request = request,
                            tick = tick,
                            isSelecting = viewModel.isSelecting,
                            isRating = viewModel.isRating,
                            winnerAgent = viewModel.winnerAgent,
                            onSelectAgent = { agentId -> viewModel.selectAgent(agentId) },
                            onRateAgent = { score -> viewModel.rateAgent(score) },
                            onAgentClick = onAgentClick,
                        )
                    }

                    // ─── Agent view: offer submission ──────────────────────────
                    if (!isMyRequest && !isExpired) {
                        AgentOfferSection(
                            myOffer = myOffer,
                            tokenBalance = viewModel.tokenBalance,
                            priceInput = viewModel.priceInput,
                            notesInput = viewModel.notesInput,
                            onPriceChange = { viewModel.priceInput = it },
                            onNotesChange = { viewModel.notesInput = it },
                            isValid = viewModel.isOfferInputValid(),
                            isSubmitting = viewModel.isSubmitting,
                            onSubmit = { viewModel.submitOffer() },
                        )
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CustomerOffersSection(
    request: ClearanceRequestDto,
    tick: Long,
    isSelecting: Boolean,
    isRating: Boolean,
    winnerAgent: ClearanceAgentDto?,
    onSelectAgent: (agentId: String) -> Unit,
    onRateAgent: (score: Int) -> Unit,
    onAgentClick: (agentId: String) -> Unit,
) {
    val lockRemaining = (request.offersLockedUntilMillis - tick).coerceAtLeast(0L)
    val isLocked = request.offersHidden && lockRemaining > 0L
    val lockHours = lockRemaining / 3_600_000L
    val lockMin = (lockRemaining % 3_600_000L) / 60_000L

    Text("العروض المقدّمة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

    if (isLocked) {
        Surface(
            color = MaterialTheme.colorScheme.secondaryContainer,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🔒", fontSize = 36.sp)
                Text("العروض مخفية حتى الآن", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "تظهر بعد %02dس %02dد".format(lockHours, lockMin),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                Text(
                    "هذا يمنح المخلصين وقتاً كافياً لتقديم عروضهم دون منافسة مبكرة",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    if (request.offers.isEmpty()) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                "لا توجد عروض حتى الآن",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        return
    }

    // Winner already selected
    val selectedOffer = request.offers.firstOrNull { it.isSelected }
    if (selectedOffer != null || request.status == "CLOSED") {
        val displayAgent = winnerAgent ?: selectedOffer?.agent
        if (displayAgent != null) {
            Surface(
                color = Color(0xFF1B5E20).copy(alpha = 0.12f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("🏆 المخلص الفائز", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20))
                    Text(displayAgent.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    Text("📍 ${displayAgent.locationLabel} | ⭐ ${displayAgent.ratingStars}", style = MaterialTheme.typography.bodySmall)
                    selectedOffer?.price?.let { Text("السعر: ${it.toJod()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold) }
                }
            }

            // Rating section (if not rated yet)
            if (!request.isRated) {
                RatingSection(isRating = isRating, onRate = onRateAgent)
            } else {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Text("شكراً على تقييمك! 🌟", Modifier.padding(12.dp), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        return
    }

    // Show all offers for selection
    request.offers.forEach { offer ->
        OfferCard(
            offer = offer,
            canSelect = request.status == "OPEN",
            isSelecting = isSelecting,
            onSelect = { onSelectAgent(offer.agentId) },
            onAgentClick = { offer.agent?.id?.let { onAgentClick(it) } },
        )
    }
}

@Composable
private fun OfferCard(
    offer: ClearanceOfferDto,
    canSelect: Boolean,
    isSelecting: Boolean,
    onSelect: () -> Unit,
    onAgentClick: () -> Unit,
) {
    val agent = offer.agent
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (agent != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(agent.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            if (agent.isVerifiedBadge) {
                                Text("✓", color = Color(0xFF1B5E20), fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        Text("⭐ ${agent.ratingStars} | ${agent.totalDeals} صفقة",
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text(
                    offer.price.toJod(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Gold,
                )
            }
            offer.notes?.let { notes ->
                if (notes.isNotBlank()) {
                    Text(notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (canSelect) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    agent?.let {
                        TextButton(onClick = onAgentClick, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)) {
                            Text("عرض الملف", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Button(
                        onClick = onSelect,
                        enabled = !isSelecting,
                        colors = ButtonDefaults.buttonColors(containerColor = Gold),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        if (isSelecting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                        else Text("اختر هذا المخلص", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun RatingSection(isRating: Boolean, onRate: (Int) -> Unit) {
    var selectedScore by remember { mutableIntStateOf(0) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("قيّم المخلص واحصل على 20 🪙", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { star ->
                    IconButton(onClick = { selectedScore = star }) {
                        Text(if (star <= selectedScore) "⭐" else "☆", fontSize = 24.sp)
                    }
                }
            }
            if (selectedScore > 0) {
                Button(
                    onClick = { onRate(selectedScore) },
                    enabled = !isRating,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                ) {
                    if (isRating) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    else Text("أرسل التقييم", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AgentOfferSection(
    myOffer: ClearanceOfferDto?,
    tokenBalance: Int,
    priceInput: String,
    notesInput: String,
    onPriceChange: (String) -> Unit,
    onNotesChange: (String) -> Unit,
    isValid: Boolean,
    isSubmitting: Boolean,
    onSubmit: () -> Unit,
) {
    HorizontalDivider()
    Text(
        if (myOffer != null) "عرضي الحالي" else "قدّم عرضك",
        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold,
    )

    if (myOffer != null) {
        Surface(color = Gold.copy(alpha = 0.12f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("سعرك: ${myOffer.price.toJod()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                myOffer.notes?.let { if (it.isNotBlank()) Text(it, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant) }
                Text("✅ تم تقديم عرضك", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1B5E20))
            }
        }
        return
    }

    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(10.dp), Arrangement.spacedBy(6.dp), Alignment.Top) {
            Text("ℹ️", fontSize = 14.sp)
            Text("سيُخصم 10 🪙 عند تقديم العرض وتُعاد إذا لم تُختر. رصيدك: $tokenBalance 🪙",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
        }
    }

    OutlinedTextField(
        value = priceInput,
        onValueChange = { onPriceChange(it.filter { c -> c.isDigit() }) },
        label = { Text("سعرك بالدينار") },
        suffix = { Text("د.أ") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = priceInput.isNotBlank() && !isValid,
        supportingText = {
            val v = priceInput.toIntOrNull()
            when {
                priceInput.isBlank() -> {}
                v == null -> Text("أدخل رقماً صحيحاً")
                v > tokenBalance -> Text("رصيدك غير كافٍ")
            }
        },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
    )

    OutlinedTextField(
        value = notesInput,
        onValueChange = onNotesChange,
        label = { Text("ملاحظات (اختياري)") },
        placeholder = { Text("أذكر خبرتك أو أي تفاصيل إضافية...") },
        modifier = Modifier.fillMaxWidth().height(90.dp),
        maxLines = 3,
    )

    Button(
        onClick = onSubmit,
        enabled = isValid && !isSubmitting,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = Gold),
    ) {
        if (isSubmitting) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.onPrimary)
        else Text("قدّم عرضك — ${priceInput.ifBlank { "?" }} د.أ", fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}
