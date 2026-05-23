package com.raed.app.ui.screens.listing

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.CreateListingRequest
import com.raed.app.data.mock.*
import com.raed.app.ui.components.TokenGateBottomSheet
import com.raed.app.ui.screens.token.loadAndShowRewardedAd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@HiltViewModel
class AddExemptionViewModel @Inject constructor(
    private val api: RaedApi,
) : ViewModel() {

    var posterType by mutableStateOf(PosterType.OFFICER)
    var vehicleCategory by mutableStateOf("SUV")
    var fuelTypeForExemption by mutableStateOf(FuelType.HYBRID)
    var price by mutableStateOf("")
    var governorate by mutableStateOf("عمّان")
    var notes by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
    val selectedMediaUris = mutableStateListOf<android.net.Uri>()

    var isPublishing by mutableStateOf(false)
        private set
    var publishSuccess by mutableStateOf(false)
        private set
    var showTokenGate by mutableStateOf(false)
        private set
    var tokenBalance by mutableIntStateOf(0)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun isValid() = price.isNotBlank() && price.toIntOrNull() != null && phoneNumber.isNotBlank()

    fun dismissTokenGate() { showTokenGate = false }
    fun clearError() { errorMessage = null }

    fun refreshBalance() {
        viewModelScope.launch {
            runCatching { api.getTokenBalance() }
                .onSuccess { r -> if (r.isSuccessful) tokenBalance = r.body()!!.tokenBalance }
        }
    }

    fun publish() {
        viewModelScope.launch {
            isPublishing = true
            errorMessage = null
            try {
                val balResp = api.getTokenBalance()
                if (balResp.isSuccessful) {
                    val bal = balResp.body()!!.tokenBalance
                    tokenBalance = bal
                    if (bal < 30) { showTokenGate = true; return@launch }
                }
                val vehicleTypeApi = when (vehicleCategory) {
                    "SUV" -> "SUV"
                    "هايبرد", "HYBRID" -> "HYBRID"
                    "كهربائي", "EV" -> "EV"
                    else -> "SEDAN"
                }
                val listingResp = api.createListing(
                    CreateListingRequest(
                        vehicleType = vehicleTypeApi,
                        makeModel = "$vehicleCategory ${fuelTypeForExemption.label}",
                        fuelType = fuelTypeForExemption.name,
                        expectedPrice = price.toIntOrNull(),
                        phoneNumber = phoneNumber.trim().takeIf { it.isNotBlank() },
                        listingType = "SEEKING",
                        listingCategory = "EXEMPTION_RIGHT",
                        governorate = governorate,
                        notes = notes.trim().takeIf { it.isNotBlank() },
                    )
                )
                when {
                    listingResp.code() == 402 -> { refreshBalance(); showTokenGate = true }
                    listingResp.isSuccessful  -> publishSuccess = true
                    else -> errorMessage = "فشل نشر الإعلان على السيرفر"
                }
            } catch (e: Exception) {
                errorMessage = e.localizedMessage ?: "خطأ غير متوقع"
            } finally {
                isPublishing = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExemptionListingScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onNavigateToWallet: () -> Unit,
    viewModel: AddExemptionViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val activity = LocalContext.current as Activity

    LaunchedEffect(viewModel.publishSuccess) {
        if (viewModel.publishSuccess) {
            snackbarHostState.showSnackbar("تم نشر إعلانك ✓")
            onSuccess()
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearError()
        }
    }

    val vehicleCategories = listOf("سيدان", "SUV", "بيكأب", "هاتشباك", "فان")

    var govExpanded by remember { mutableStateOf(false) }

    val mediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        uris.forEach { uri ->
            if (viewModel.selectedMediaUris.size < 10 && uri !in viewModel.selectedMediaUris) {
                viewModel.selectedMediaUris.add(uri)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعلان إعفاء") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // Poster type
            Text("أنا", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PosterType.entries.forEach { type ->
                    FilterChip(
                        selected = viewModel.posterType == type,
                        onClick = { viewModel.posterType = type },
                        label = { Text(type.label) },
                    )
                }
            }

            HorizontalDivider()

            if (viewModel.posterType == PosterType.OFFICER) {
                Text(
                    "يعرض إعفاءه",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    "يبحث عن إعفاء",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            // Vehicle category
            Text("نوع السيارة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                vehicleCategories.forEach { cat ->
                    FilterChip(
                        selected = viewModel.vehicleCategory == cat,
                        onClick = { viewModel.vehicleCategory = cat },
                        label = { Text(cat, style = MaterialTheme.typography.bodySmall) },
                    )
                }
            }

            // Fuel type
            Text("نوع الوقود", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(FuelType.GASOLINE, FuelType.HYBRID, FuelType.ELECTRIC).forEach { fuel ->
                    FilterChip(
                        selected = viewModel.fuelTypeForExemption == fuel,
                        onClick = { viewModel.fuelTypeForExemption = fuel },
                        label = { Text(fuel.label, style = MaterialTheme.typography.bodySmall) },
                    )
                }
            }

            val priceLabel = if (viewModel.posterType == PosterType.OFFICER)
                "السعر المطلوب مقابل الإعفاء *"
            else
                "ميزانيتك لشراء الإعفاء *"

            OutlinedTextField(
                value = viewModel.price,
                onValueChange = { viewModel.price = it.filter { c -> c.isDigit() } },
                label = { Text(priceLabel) },
                placeholder = { Text("السعر الحالي بالسوق ~12,000 د.أ") },
                suffix = { Text("د.أ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            ExposedDropdownMenuBox(
                expanded = govExpanded,
                onExpandedChange = { govExpanded = it },
            ) {
                OutlinedTextField(
                    value = viewModel.governorate,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("المحافظة") },
                    modifier = Modifier
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        .fillMaxWidth(),
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = govExpanded) },
                )
                ExposedDropdownMenu(
                    expanded = govExpanded,
                    onDismissRequest = { govExpanded = false },
                ) {
                    GOVERNORATES.forEach { gov ->
                        DropdownMenuItem(
                            text = { Text(gov) },
                            onClick = { viewModel.governorate = gov; govExpanded = false },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = viewModel.notes,
                onValueChange = { viewModel.notes = it },
                label = { Text("ملاحظات (اختياري)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4,
            )

            // Media picker
            Text(
                "صور توثيقية (اختياري)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (viewModel.selectedMediaUris.isEmpty()) {
                OutlinedCard(
                    onClick = {
                        mediaLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("📷", style = MaterialTheme.typography.titleLarge)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    "أضف صور أو فيديوهات",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    "حتى 10 ملفات",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                )
                            }
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    viewModel.selectedMediaUris.forEach { uri ->
                        Box(modifier = Modifier.size(80.dp)) {
                            AsyncImage(
                                model = uri,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop,
                            )
                            IconButton(
                                onClick = { viewModel.selectedMediaUris.remove(uri) },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(28.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .background(Color(0x99000000), CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        Icons.Outlined.Close,
                                        contentDescription = "حذف",
                                        tint = Color.White,
                                        modifier = Modifier.size(12.dp),
                                    )
                                }
                            }
                        }
                    }
                    if (viewModel.selectedMediaUris.size < 10) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable {
                                    mediaLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "+",
                                style = MaterialTheme.typography.headlineMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                Text(
                    "${viewModel.selectedMediaUris.size} / 10",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = viewModel.phoneNumber,
                onValueChange = { viewModel.phoneNumber = it.filter { c -> c.isDigit() } },
                label = { Text("رقم التواصل *") },
                placeholder = { Text("07XXXXXXXX") },
                prefix = { Text("+962 ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { viewModel.publish() },
                enabled = viewModel.isValid() && !viewModel.isPublishing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC9A961)),
            ) {
                if (viewModel.isPublishing) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("انشر — 30 🪙", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (viewModel.showTokenGate) {
        TokenGateBottomSheet(
            required = 30,
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
}
