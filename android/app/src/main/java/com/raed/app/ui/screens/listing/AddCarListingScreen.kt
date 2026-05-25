package com.raed.app.ui.screens.listing

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.outlined.Close
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.mutableStateListOf
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.CreateListingRequest
import com.raed.app.data.mock.*
import com.raed.app.ui.components.TokenGateBottomSheet
import com.raed.app.ui.screens.token.loadAndShowRewardedAd
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private val Gold = Color(0xFFC9A961)

@HiltViewModel
class AddCarViewModel @Inject constructor(
    private val api: RaedApi,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val listingCategory: String = savedStateHandle["category"] ?: "MORTAJA3"

    // Step 1
    var make by mutableStateOf("")
    var model by mutableStateOf("")
    var year by mutableIntStateOf(2023)
    var mileage by mutableStateOf("")
    var color by mutableStateOf("")
    var vehicleType by mutableStateOf("SEDAN")
    var fuelType by mutableStateOf(FuelType.GASOLINE)
    var transmission by mutableStateOf(Transmission.AUTOMATIC)

    fun selectVehicleType(vt: String) {
        vehicleType = vt
        when (vt) {
            "HYBRID" -> fuelType = FuelType.HYBRID
            "EV"     -> fuelType = FuelType.ELECTRIC
            else     -> {}
        }
    }

    // Step 2
    var price by mutableStateOf("")
    var negotiable by mutableStateOf(false)
    var description by mutableStateOf("")
    var phoneNumber by mutableStateOf("")

    // Step 3
    var governorate by mutableStateOf("عمّان")
    val selectedMediaUris = mutableStateListOf<android.net.Uri>()

    // UI state
    var currentStep by mutableIntStateOf(1)
        private set
    var isUploadingPhotos by mutableStateOf(false)
        private set
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

    fun nextStep() { if (currentStep < 3) currentStep++ }
    fun prevStep() { if (currentStep > 1) currentStep-- }

    fun step1Valid() = make.isNotBlank() && model.isNotBlank() && mileage.isNotBlank() && color.isNotBlank()
    fun step2Valid() = price.isNotBlank() && price.toIntOrNull() != null

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
                // Upfront balance check for UX (server enforces atomically)
                val balResp = api.getTokenBalance()
                if (balResp.isSuccessful) {
                    val bal = balResp.body()!!.tokenBalance
                    tokenBalance = bal
                    if (bal < 50) { showTokenGate = true; return@launch }
                }

                val photoUrls = if (selectedMediaUris.isNotEmpty()) {
                    isUploadingPhotos = true
                    uploadPhotos(selectedMediaUris.toList()).also { isUploadingPhotos = false }
                } else emptyList()

                val listingResp = api.createListing(
                    CreateListingRequest(
                        vehicleType = vehicleType,
                        makeModel = "${make.trim()} ${model.trim()}",
                        yearMin = year,
                        yearMax = year,
                        color = color.trim(),
                        mileageKm = mileage.toIntOrNull(),
                        fuelType = fuelType.name,
                        transmission = transmission.name,
                        expectedPrice = price.toIntOrNull(),
                        phoneNumber = phoneNumber.trim().takeIf { it.isNotBlank() },
                        listingType = "OWNED",
                        listingCategory = listingCategory,
                        governorate = governorate,
                        notes = description.trim().takeIf { it.isNotBlank() },
                        photos = photoUrls,
                    )
                )
                when {
                    listingResp.code() == 402 -> { refreshBalance(); showTokenGate = true }
                    listingResp.isSuccessful  -> publishSuccess = true
                    else                      -> errorMessage = "فشل نشر الإعلان على السيرفر"
                }
            } catch (e: Exception) {
                isUploadingPhotos = false
                errorMessage = e.localizedMessage ?: "خطأ غير متوقع"
            } finally {
                isPublishing = false
            }
        }
    }

    private suspend fun uploadPhotos(uris: List<android.net.Uri>): List<String> {
        val userId = Firebase.auth.currentUser?.uid ?: return emptyList()
        val timestamp = System.currentTimeMillis()
        return uris.mapIndexed { index, uri ->
            val ref = Firebase.storage.reference.child("listings/$userId/${timestamp}_$index")
            ref.putFile(uri).await()
            ref.downloadUrl.await().toString()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCarListingScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    onNavigateToWallet: () -> Unit,
    viewModel: AddCarViewModel = hiltViewModel(),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.listingCategory == "REGULAR") "سيارة عادية للبيع" else "إضافة سيارة") },
                navigationIcon = {
                    IconButton(onClick = { if (viewModel.currentStep > 1) viewModel.prevStep() else onBack() }) {
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
                .imePadding(),
        ) {
            // Step progress
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { viewModel.currentStep / 3f },
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "الخطوة ${viewModel.currentStep} من 3",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
            )

            when (viewModel.currentStep) {
                1 -> Step1Content(viewModel, Modifier.weight(1f))
                2 -> Step2Content(viewModel, Modifier.weight(1f))
                3 -> Step3Content(viewModel, Modifier.weight(1f))
            }

            // Bottom action button
            Spacer(Modifier.height(8.dp))
            when (viewModel.currentStep) {
                1 -> Button(
                    onClick = { viewModel.nextStep() },
                    enabled = viewModel.step1Valid(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("التالي") }

                2 -> Button(
                    onClick = { viewModel.nextStep() },
                    enabled = viewModel.step2Valid(),
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("التالي") }

                3 -> Button(
                    onClick = { viewModel.publish() },
                    enabled = !viewModel.isPublishing && !viewModel.isUploadingPhotos,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Gold),
                ) {
                    when {
                        viewModel.isUploadingPhotos -> {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("جاري رفع الصور...", fontWeight = FontWeight.Bold)
                        }
                        viewModel.isPublishing -> {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        }
                        else -> Text("انشر الإعلان — 50 🪙", fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (viewModel.showTokenGate) {
        TokenGateBottomSheet(
            required = 50,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step1Content(vm: AddCarViewModel, modifier: Modifier = Modifier) {
    val makes = listOf("تويوتا", "هيونداي", "كيا", "نيسان", "هوندا", "مرسيدس", "BMW", "فورد", "أخرى")
    var makeExpanded by remember { mutableStateOf(false) }
    val filteredMakes = makes.filter { vm.make.isBlank() || it.contains(vm.make) }

    var yearExpanded by remember { mutableStateOf(false) }
    val years = (2025 downTo 2000).toList()

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Make autocomplete
        ExposedDropdownMenuBox(
            expanded = makeExpanded && filteredMakes.isNotEmpty(),
            onExpandedChange = { makeExpanded = it },
        ) {
            OutlinedTextField(
                value = vm.make,
                onValueChange = { vm.make = it; makeExpanded = true },
                label = { Text("الماركة *") },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryEditable, true)
                    .fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = makeExpanded) },
                singleLine = true,
            )
            ExposedDropdownMenu(
                expanded = makeExpanded && filteredMakes.isNotEmpty(),
                onDismissRequest = { makeExpanded = false },
            ) {
                filteredMakes.forEach { suggestion ->
                    DropdownMenuItem(
                        text = { Text(suggestion) },
                        onClick = { vm.make = suggestion; makeExpanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        OutlinedTextField(
            value = vm.model,
            onValueChange = { vm.model = it },
            label = { Text("الموديل *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Year dropdown
        ExposedDropdownMenuBox(
            expanded = yearExpanded,
            onExpandedChange = { yearExpanded = it },
        ) {
            OutlinedTextField(
                value = vm.year.toString(),
                onValueChange = {},
                readOnly = true,
                label = { Text("سنة الصنع") },
                modifier = Modifier
                    .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                    .fillMaxWidth(),
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearExpanded) },
            )
            ExposedDropdownMenu(
                expanded = yearExpanded,
                onDismissRequest = { yearExpanded = false },
            ) {
                years.forEach { y ->
                    DropdownMenuItem(
                        text = { Text(y.toString()) },
                        onClick = { vm.year = y; yearExpanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        OutlinedTextField(
            value = vm.mileage,
            onValueChange = { vm.mileage = it.filter { c -> c.isDigit() } },
            label = { Text("الكيلومترات *") },
            suffix = { Text("كم") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        OutlinedTextField(
            value = vm.color,
            onValueChange = { vm.color = it },
            label = { Text("اللون *") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        // Vehicle body type
        val bodyTypes = listOf("سيدان" to "SEDAN", "SUV" to "SUV", "بيكأب" to "OTHER", "هايبرد" to "HYBRID", "كهربائي" to "EV")
        Text("نوع السيارة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            bodyTypes.forEach { (label, apiValue) ->
                FilterChip(
                    selected = vm.vehicleType == apiValue,
                    onClick = { vm.selectVehicleType(apiValue) },
                    label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                )
            }
        }

        // Fuel type
        Text("نوع الوقود", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FuelType.entries.forEach { fuel ->
                FilterChip(
                    selected = vm.fuelType == fuel,
                    onClick = { vm.fuelType = fuel },
                    label = { Text(fuel.label, style = MaterialTheme.typography.bodySmall) },
                )
            }
        }

        // Transmission
        Text("ناقل الحركة", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Transmission.entries.forEach { trans ->
                FilterChip(
                    selected = vm.transmission == trans,
                    onClick = { vm.transmission = trans },
                    label = { Text(trans.label, style = MaterialTheme.typography.bodySmall) },
                )
            }
        }
    }
}

@Composable
private fun Step2Content(vm: AddCarViewModel, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = vm.price,
            onValueChange = { vm.price = it.filter { c -> c.isDigit() } },
            label = { Text("السعر *") },
            suffix = { Text("د.أ") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("قابل للتفاوض", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = vm.negotiable, onCheckedChange = { vm.negotiable = it })
        }

        OutlinedTextField(
            value = vm.description,
            onValueChange = { vm.description = it },
            label = { Text("الوصف") },
            placeholder = { Text("أي معلومات إضافية...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            maxLines = 5,
        )

        OutlinedTextField(
            value = vm.phoneNumber,
            onValueChange = { vm.phoneNumber = it.filter { c -> c.isDigit() } },
            label = { Text("رقم للتواصل (اختياري)") },
            placeholder = { Text("07XXXXXXXX") },
            prefix = { Text("+962 ") },
            supportingText = { Text("اتركه فارغاً لاستخدام رقمك المسجل في الملف الشخصي") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Step3Content(vm: AddCarViewModel, modifier: Modifier = Modifier) {
    var govExpanded by remember { mutableStateOf(false) }

    val mediaLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 10)
    ) { uris ->
        uris.forEach { uri ->
            if (vm.selectedMediaUris.size < 10 && uri !in vm.selectedMediaUris) {
                vm.selectedMediaUris.add(uri)
            }
        }
    }

    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ExposedDropdownMenuBox(
            expanded = govExpanded,
            onExpandedChange = { govExpanded = it },
        ) {
            OutlinedTextField(
                value = vm.governorate,
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
                        onClick = { vm.governorate = gov; govExpanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                    )
                }
            }
        }

        // Media picker
        Text(
            "الصور والفيديوهات (اختياري)",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (vm.selectedMediaUris.isEmpty()) {
            OutlinedCard(
                onClick = {
                    mediaLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("📷", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "اضغط لإضافة صور أو فيديوهات",
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
        } else {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                vm.selectedMediaUris.forEach { uri ->
                    Box(modifier = Modifier.size(90.dp)) {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop,
                        )
                        IconButton(
                            onClick = { vm.selectedMediaUris.remove(uri) },
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
                if (vm.selectedMediaUris.size < 10) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
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
                "${vm.selectedMediaUris.size} / 10",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Summary card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("ملخص الإعلان", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                HorizontalDivider()
                SummaryRow("النوع", listOf("سيدان" to "SEDAN", "SUV" to "SUV", "بيكأب" to "OTHER", "هايبرد" to "HYBRID", "كهربائي" to "EV").firstOrNull { it.second == vm.vehicleType }?.first ?: vm.vehicleType)
                SummaryRow("السيارة", "${vm.make} ${vm.model} ${vm.year}")
                SummaryRow("الكيلومترات", "${vm.mileage} كم")
                SummaryRow("اللون", vm.color)
                SummaryRow("الوقود", vm.fuelType.label)
                SummaryRow("ناقل الحركة", vm.transmission.label)
                SummaryRow("السعر", "${vm.price} د.أ${if (vm.negotiable) " (قابل للتفاوض)" else ""}")
                SummaryRow("المحافظة", vm.governorate)
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}
