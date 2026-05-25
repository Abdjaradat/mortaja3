package com.raed.app.ui.screens.listing

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.raed.app.ui.components.UnityBannerCard
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.PostRequestBody
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private val Gold = Color(0xFFC9A961)

private val VEHICLE_TYPES = listOf(
    "SUV" to "SUV",
    "SEDAN" to "سيدان",
    "HYBRID" to "هايبرد",
    "EV" to "كهربائي",
    "OTHER" to "بيكأب",
)

private val GOVERNORATES = listOf(
    "عمّان", "إربد", "الزرقاء", "العقبة", "المفرق",
    "جرش", "عجلون", "الكرك", "الطفيلة", "معان", "السلط", "مادبا",
)

@HiltViewModel
class PostRequestViewModel @Inject constructor(
    private val api: RaedApi,
) : ViewModel() {
    var vehicleType by mutableStateOf("SUV")
    var budgetMin by mutableStateOf("")
    var budgetMax by mutableStateOf("")
    var governorate by mutableStateOf("عمّان")
    var notes by mutableStateOf("")
    var isPosted by mutableStateOf(false)
    var postedAt by mutableLongStateOf(0L)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun isValid(): Boolean {
        val min = budgetMin.toIntOrNull() ?: return false
        val max = budgetMax.toIntOrNull() ?: return false
        return min > 0 && max > min
    }

    fun post() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val response = api.postRequest(
                    PostRequestBody(
                        vehicleType = vehicleType,
                        budgetMin = budgetMin.toInt(),
                        budgetMax = budgetMax.toInt(),
                        governorate = governorate,
                        notes = notes.trim().ifBlank { null },
                    )
                )
                if (response.isSuccessful) {
                    postedAt = System.currentTimeMillis()
                    isPosted = true
                } else {
                    error = "حدث خطأ أثناء نشر الطلب (${response.code()})"
                }
            } catch (e: Exception) {
                error = "تعذّر الاتصال بالخادم"
            } finally {
                isLoading = false
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostRequestScreen(
    onBack: () -> Unit,
    viewModel: PostRequestViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.error) {
        val msg = viewModel.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.error = null
    }

    if (viewModel.isPosted) {
        PostedSuccessContent(postedAt = viewModel.postedAt, onBack = onBack)
        return
    }

    var govExpanded by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("انشر طلبك") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = { UnityBannerCard() },
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

            // Info banner
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text("🎁", fontSize = 18.sp)
                    Text(
                        "نشر الطلبات مجاناً للمشترين. سيتواصل معك الوسطاء خلال 24 ساعة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Vehicle type chips
            Text(
                "نوع السيارة المطلوبة",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VEHICLE_TYPES.forEach { (apiValue, label) ->
                    FilterChip(
                        selected = viewModel.vehicleType == apiValue,
                        onClick = { viewModel.vehicleType = apiValue },
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                    )
                }
            }

            // Budget range
            Text(
                "الميزانية",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = viewModel.budgetMin,
                    onValueChange = { viewModel.budgetMin = it.filter { c -> c.isDigit() } },
                    label = { Text("من") },
                    suffix = { Text("د.أ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                Text("—", style = MaterialTheme.typography.bodyLarge)
                OutlinedTextField(
                    value = viewModel.budgetMax,
                    onValueChange = { viewModel.budgetMax = it.filter { c -> c.isDigit() } },
                    label = { Text("إلى") },
                    suffix = { Text("د.أ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }

            // Governorate
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

            // Notes
            OutlinedTextField(
                value = viewModel.notes,
                onValueChange = { viewModel.notes = it },
                label = { Text("ملاحظات (اختياري)") },
                placeholder = { Text("أي تفاصيل إضافية كاللون أو الموديل...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                maxLines = 4,
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { viewModel.post() },
                enabled = viewModel.isValid() && !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("انشر طلبك — مجاناً 🎁", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PostedSuccessContent(postedAt: Long, onBack: () -> Unit) {
    val durationMs = 24L * 60 * 60 * 1000
    var remaining by remember {
        mutableLongStateOf((postedAt + durationMs - System.currentTimeMillis()).coerceAtLeast(0L))
    }

    LaunchedEffect(Unit) {
        while (remaining > 0L) {
            delay(1000L)
            remaining = (postedAt + durationMs - System.currentTimeMillis()).coerceAtLeast(0L)
        }
    }

    val hours = remaining / 3_600_000L
    val minutes = (remaining % 3_600_000L) / 60_000L
    val seconds = (remaining % 60_000L) / 1_000L

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("✅", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "تم نشر طلبك!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "سيُعرض طلبك على الوسطاء لمدة 24 ساعة",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium,
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    "الوقت المتبقي",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    "%02d:%02d:%02d".format(hours, minutes, seconds),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        OutlinedButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("العودة للرئيسية")
        }
    }
}
