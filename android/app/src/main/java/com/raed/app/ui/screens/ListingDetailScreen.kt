package com.raed.app.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DriveEta
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.ListingDto
import com.raed.app.data.api.models.StartConversationRequest
import com.raed.app.data.mock.toJod
import com.raed.app.ui.components.TokenGateBottomSheet
import com.raed.app.ui.components.AdEarnCard
import com.raed.app.ui.screens.token.loadAndShowRewardedAd
import com.raed.app.utils.toTimeAgo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private val Gold = Color(0xFFC9A961)
private val ExemptionBg = Color(0xFFFFF8E7)

@HiltViewModel
class ListingDetailViewModel @Inject constructor(
    private val api: RaedApi,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val listingId: String = checkNotNull(savedStateHandle["listingId"])

    var listing by mutableStateOf<ListingDto?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set

    var tokenBalance by mutableIntStateOf(0)
        private set
    var isSpending by mutableStateOf(false)
        private set
    var revealedPhone by mutableStateOf<String?>(null)
        private set
    var showTokenGate by mutableStateOf(false)
        private set
    var tokenGateRequired by mutableIntStateOf(0)
        private set
    var snackbarMessage by mutableStateOf<String?>(null)
        private set
    var startConversationResult by mutableStateOf<Pair<String, String>?>(null) // id, otherUserName
        private set

    init {
        load()
        refreshBalance()
    }

    fun load() {
        viewModelScope.launch {
            isLoading = true
            loadError = null
            runCatching { api.getListingById(listingId) }
                .onSuccess { r ->
                    if (r.isSuccessful) listing = r.body()
                    else loadError = "لم يُعثر على الإعلان"
                }
                .onFailure { loadError = "تحقق من اتصالك بالإنترنت" }
            isLoading = false
        }
    }

    fun refreshBalance() {
        viewModelScope.launch {
            runCatching { api.getTokenBalance() }
                .onSuccess { r -> if (r.isSuccessful) tokenBalance = r.body()!!.tokenBalance }
        }
    }

    fun revealContact() {
        if (tokenBalance < 20) {
            tokenGateRequired = 20
            showTokenGate = true
            return
        }
        viewModelScope.launch {
            isSpending = true
            runCatching { api.revealContact(listingId) }
                .onSuccess { r ->
                    if (r.isSuccessful) {
                        val body = r.body()!!
                        revealedPhone = body.phoneNumber
                        if (body.charged) tokenBalance -= 20
                    } else if (r.code() == 402) {
                        tokenGateRequired = 20
                        showTokenGate = true
                    } else if (r.code() == 404) {
                        snackbarMessage = "لا يوجد رقم هاتف لهذا الإعلان"
                    } else {
                        snackbarMessage = "فشل الكشف عن رقم الهاتف"
                    }
                }
                .onFailure { snackbarMessage = "خطأ في الاتصال" }
            isSpending = false
        }
    }

    fun onMessageClick() {
        val officerId = listing?.officer?.id ?: return
        if (tokenBalance < 10) {
            tokenGateRequired = 10
            showTokenGate = true
            return
        }
        viewModelScope.launch {
            isSpending = true
            runCatching {
                api.startConversation(StartConversationRequest(otherUserId = officerId, listingId = listingId))
            }
                .onSuccess { r ->
                    if (r.isSuccessful) {
                        tokenBalance -= 10
                        val conv = r.body()!!
                        val name = listing?.officer?.fullName ?: "البائع"
                        startConversationResult = conv.id to name
                    } else {
                        snackbarMessage = "فشل بدء المحادثة"
                    }
                }
                .onFailure { snackbarMessage = "خطأ في الاتصال" }
            isSpending = false
        }
    }

    fun watchAd() {
        viewModelScope.launch {
            runCatching { api.watchAd() }
                .onSuccess { r -> if (r.isSuccessful) tokenBalance += r.body()?.tokensEarned ?: 10 }
        }
    }

    fun dismissTokenGate() { showTokenGate = false }
    fun dismissPhone() { revealedPhone = null }
    fun clearSnackbar() { snackbarMessage = null }
    fun clearStartConversation() { startConversationResult = null }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListingDetailScreen(
    onBack: () -> Unit,
    onNavigateToCalculator: (price: Int) -> Unit,
    onNavigateToWallet: () -> Unit,
    onNavigateToConversation: (conversationId: String, name: String) -> Unit,
    viewModel: ListingDetailViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current as Activity

    LaunchedEffect(viewModel.snackbarMessage) {
        viewModel.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    LaunchedEffect(viewModel.startConversationResult) {
        viewModel.startConversationResult?.let { (convId, name) ->
            onNavigateToConversation(convId, name)
            viewModel.clearStartConversation()
        }
    }

    val listing = viewModel.listing
    val title = listing?.makeModel ?: if (viewModel.isLoading) "جار التحميل…" else "الإعلان"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (listing != null) {
                if (listing.isExemptionRight) {
                    ExemptionBottomBar(onMessage = { viewModel.onMessageClick() })
                } else {
                    CarBottomBar(
                        isSpending = viewModel.isSpending,
                        onMessage = { viewModel.onMessageClick() },
                        onReveal = { viewModel.revealContact() },
                    )
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
            viewModel.loadError != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("⚠️", fontSize = 40.sp)
                        Text(viewModel.loadError!!, color = MaterialTheme.colorScheme.error)
                        OutlinedButton(onClick = { viewModel.load() }) { Text("إعادة المحاولة") }
                    }
                }
            }
            listing != null -> {
                if (listing.isExemptionRight) {
                    ExemptionDetailContent(
                        listing = listing,
                        onCalcClick = { onNavigateToCalculator(listing.expectedPrice ?: 0) },
                        onWatchAd = { viewModel.watchAd() },
                        modifier = Modifier.padding(padding),
                    )
                } else {
                    CarDetailContent(
                        listing = listing,
                        onWatchAd = { viewModel.watchAd() },
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }

    if (viewModel.showTokenGate) {
        TokenGateBottomSheet(
            required = viewModel.tokenGateRequired,
            current = viewModel.tokenBalance,
            onWatchAd = {
                loadAndShowRewardedAd(
                    activity = activity,
                    onRewarded = { viewModel.refreshBalance() },
                    onFailed = {},
                )
            },
            onBuyTokens = { onNavigateToWallet() },
            onDismiss = { viewModel.dismissTokenGate() },
        )
    }

    viewModel.revealedPhone?.let { phone ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissPhone() },
            title = { Text("رقم الهاتف") },
            text = {
                Text(text = phone, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissPhone() }) { Text("حسناً") }
            },
        )
    }

}

@Composable
private fun CarDetailContent(listing: ListingDto, onWatchAd: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier.fillMaxWidth().height(220.dp).background(Color(0xFFE3F2FD)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.DriveEta, contentDescription = null, modifier = Modifier.size(80.dp), tint = Color(0xFF90A4AE))
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                val title = buildString {
                    append(listing.makeModel)
                    listing.displayYear.takeIf { it.isNotEmpty() }?.let { append(" $it") }
                }
                Text(text = title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                listing.expectedPrice?.let {
                    Text(text = it.toJod(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Gold)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                listing.mileageKm?.let { DetailChip("%,d كم".format(it)) }
                listing.fuelTypeLabel.takeIf { it.isNotEmpty() }?.let { DetailChip(it) }
                listing.transmissionLabel.takeIf { it.isNotEmpty() }?.let { DetailChip(it) }
                listing.color?.let { DetailChip(it) }
            }

            if (!listing.notes.isNullOrBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("الوصف", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        HorizontalDivider()
                        Text(listing.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("👤", fontSize = 20.sp)
                    }
                    Column {
                        Text(
                            listing.officer?.fullName ?: "البائع",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "📍 ${listing.governorate} • ${listing.createdAt.toTimeAgo()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            AdEarnCard(onAdWatched = onWatchAd)
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun ExemptionDetailContent(
    listing: ListingDto,
    onCalcClick: () -> Unit,
    onWatchAd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(
            modifier = Modifier.fillMaxWidth().background(Gold).padding(20.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("🎖", fontSize = 36.sp)
                Text("إعفاء ضابط", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                if (listing.isVerified) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Outlined.Verified, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("موثّق", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = ExemptionBg)) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ExemptionDetailRow("يبحث عن", listing.makeModel)
                    listing.expectedPrice?.let { ExemptionDetailRow("السعر", it.toJod()) }
                    ExemptionDetailRow("المحافظة", "📍 ${listing.governorate}")
                    ExemptionDetailRow("نُشر", listing.createdAt.toTimeAgo())
                }
            }

            if (!listing.notes.isNullOrBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("ملاحظات", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        HorizontalDivider()
                        Text(listing.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            OutlinedButton(
                onClick = onCalcClick,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Gold),
                border = androidx.compose.foundation.BorderStroke(1.dp, Gold),
            ) {
                Text("احسب الربح من هذا الإعلان 🧮", fontWeight = FontWeight.SemiBold)
            }

            AdEarnCard(onAdWatched = onWatchAd)
            Spacer(Modifier.height(80.dp))
        }
    }
}

@Composable
private fun CarBottomBar(isSpending: Boolean, onMessage: () -> Unit, onReveal: () -> Unit) {
    Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(onClick = onMessage, modifier = Modifier.weight(1f)) {
                Text("💬 راسل — 10🪙", style = MaterialTheme.typography.labelLarge)
            }
            Button(
                onClick = onReveal,
                enabled = !isSpending,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
            ) {
                if (isSpending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("📞 اتصل — 20🪙", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ExemptionBottomBar(onMessage: () -> Unit) {
    Surface(tonalElevation = 8.dp, shadowElevation = 8.dp) {
        Box(modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            Button(onClick = onMessage, modifier = Modifier.fillMaxWidth()) {
                Text("💬 تواصل — 10🪙", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun DetailChip(text: String) {
    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(text = text, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ExemptionDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
