package com.raed.app.ui.screens.clearance

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import com.raed.app.data.api.models.PostClearanceRequestBody
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay

private val Gold = Color(0xFFC9A961)

private val SERVICE_TYPES = listOf(
    "CARS" to "تخليص سيارة",
    "GOODS" to "بضاعة عامة",
    "CONTAINERS" to "حاوية",
    "OTHER" to "أخرى",
)

private val LOCATIONS = listOf(
    "BOHRET_AMMAN" to "بحرة عمان",
    "ZARQA" to "الزرقاء",
)

@HiltViewModel
class PostClearanceRequestViewModel @Inject constructor(
    private val api: RaedApi,
) : ViewModel() {
    var serviceType by mutableStateOf("CARS")
    var location by mutableStateOf("BOHRET_AMMAN")
    var description by mutableStateOf("")
    var budgetMaxInput by mutableStateOf("")
    var isPosted by mutableStateOf(false)
    var postedAt by mutableLongStateOf(0L)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun isValid() = description.trim().length >= 10

    fun post() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val response = api.postClearanceRequest(
                    PostClearanceRequestBody(
                        serviceType = serviceType,
                        location = location,
                        description = description.trim(),
                        budgetMax = budgetMaxInput.toIntOrNull(),
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
fun PostClearanceRequestScreen(
    onBack: () -> Unit,
    viewModel: PostClearanceRequestViewModel = hiltViewModel(),
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("انشر طلب تخليص") },
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
                        "النشر مجاني. سيتواصل المخلصون معك خلال 24 ساعة.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            // Service type
            Text("نوع الخدمة", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SERVICE_TYPES.forEach { (apiValue, label) ->
                    FilterChip(
                        selected = viewModel.serviceType == apiValue,
                        onClick = { viewModel.serviceType = apiValue },
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                    )
                }
            }

            // Location
            Text("الموقع", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LOCATIONS.forEach { (apiValue, label) ->
                    FilterChip(
                        selected = viewModel.location == apiValue,
                        onClick = { viewModel.location = apiValue },
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                    )
                }
            }

            // Description
            OutlinedTextField(
                value = viewModel.description,
                onValueChange = { viewModel.description = it },
                label = { Text("وصف الطلب") },
                placeholder = { Text("اذكر نوع البضاعة، المشكلة، أي تفاصيل مهمة...") },
                modifier = Modifier.fillMaxWidth().height(120.dp),
                maxLines = 5,
                isError = viewModel.description.isNotBlank() && viewModel.description.trim().length < 10,
                supportingText = {
                    if (viewModel.description.isNotBlank() && viewModel.description.trim().length < 10) {
                        Text("الوصف قصير جداً (10 أحرف على الأقل)")
                    }
                },
            )

            // Budget
            OutlinedTextField(
                value = viewModel.budgetMaxInput,
                onValueChange = { viewModel.budgetMaxInput = it.filter { c -> c.isDigit() } },
                label = { Text("ميزانيتك التقريبية (اختياري)") },
                placeholder = { Text("يساعد المخلصين على تقديم عروض مناسبة") },
                suffix = { Text("د.أ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { viewModel.post() },
                enabled = viewModel.isValid() && !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
            ) {
                if (viewModel.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("انشر الطلب مجاناً 🎁", fontWeight = FontWeight.Bold)
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
        while (remaining > 0L) { delay(1000L)
            remaining = (postedAt + durationMs - System.currentTimeMillis()).coerceAtLeast(0L) }
    }
    val hours = remaining / 3_600_000L
    val minutes = (remaining % 3_600_000L) / 60_000L
    val seconds = (remaining % 60_000L) / 1_000L

    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("✅", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("تم نشر طلبك!", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "سيتلقى المخلصون إشعاراً بطلبك\nستظهر العروض خلال 4 ساعات\nالطلب مفتوح لمدة 24 ساعة",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
            Column(
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("الوقت المتبقي", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
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
        OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("العودة للرئيسية")
        }
    }
}
