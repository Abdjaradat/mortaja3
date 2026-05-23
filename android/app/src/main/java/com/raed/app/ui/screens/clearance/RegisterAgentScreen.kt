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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.RegisterAgentBody
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private val Gold = Color(0xFFC9A961)

private val LOCATION_OPTIONS = listOf("BOHRET_AMMAN" to "بحرة عمان", "ZARQA" to "الزرقاء")
private val SPEC_OPTIONS = listOf("CARS" to "سيارات", "GOODS" to "بضائع عامة", "CONTAINERS" to "حاويات", "ALL" to "الكل")

@HiltViewModel
class RegisterAgentViewModel @Inject constructor(private val api: RaedApi) : ViewModel() {
    var displayName by mutableStateOf("")
    var location by mutableStateOf("BOHRET_AMMAN")
    var specializations by mutableStateOf(setOf("CARS"))
    var yearsInput by mutableStateOf("")
    var bio by mutableStateOf("")
    var isRegistered by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    fun isValid() = displayName.trim().length >= 2 && specializations.isNotEmpty()

    fun toggleSpec(spec: String) {
        specializations = if (specializations.contains(spec)) specializations - spec else specializations + spec
    }

    fun register() {
        viewModelScope.launch {
            isLoading = true
            error = null
            try {
                val resp = api.registerClearanceAgent(
                    RegisterAgentBody(
                        displayName = displayName.trim(),
                        location = location,
                        specializations = specializations.toList(),
                        yearsExperience = yearsInput.toIntOrNull() ?: 0,
                        bio = bio.trim().ifBlank { null },
                    )
                )
                when {
                    resp.isSuccessful -> isRegistered = true
                    resp.code() == 402 -> error = "رصيد توكنز غير كافٍ (50 🪙 مطلوبة)"
                    resp.code() == 409 -> error = "أنت مسجّل بالفعل كمخلص"
                    else -> error = "حدث خطأ (${resp.code()})"
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
fun RegisterAgentScreen(
    onBack: () -> Unit,
    onRegistered: () -> Unit = {},
    viewModel: RegisterAgentViewModel = hiltViewModel(),
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.error) {
        val msg = viewModel.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg); viewModel.error = null
    }

    if (viewModel.isRegistered) {
        RegistrationSuccessContent(onDone = { onRegistered(); onBack() })
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("سجّل كمخلص جمركي") },
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
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()).imePadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Spacer(Modifier.height(4.dp))

            // Cost notice
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(12.dp), Arrangement.spacedBy(8.dp), Alignment.Top) {
                    Text("💡", fontSize = 18.sp)
                    Text("التسجيل يكلف 50 🪙 مرة واحدة فقط. بعدها تقدر تقدم عروضك على طلبات العملاء.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            // Display name
            OutlinedTextField(
                value = viewModel.displayName,
                onValueChange = { viewModel.displayName = it },
                label = { Text("الاسم المهني") },
                placeholder = { Text("الاسم الذي سيراه العملاء") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Location
            Text("موقع العمل", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LOCATION_OPTIONS.forEach { (apiValue, label) ->
                    FilterChip(
                        selected = viewModel.location == apiValue,
                        onClick = { viewModel.location = apiValue },
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                    )
                }
            }

            // Specializations (multi-select)
            Text("التخصص", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SPEC_OPTIONS.forEach { (apiValue, label) ->
                    FilterChip(
                        selected = viewModel.specializations.contains(apiValue),
                        onClick = { viewModel.toggleSpec(apiValue) },
                        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
                    )
                }
            }

            // Years of experience
            OutlinedTextField(
                value = viewModel.yearsInput,
                onValueChange = { viewModel.yearsInput = it.filter { c -> c.isDigit() } },
                label = { Text("سنوات الخبرة") },
                suffix = { Text("سنة") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            // Bio
            OutlinedTextField(
                value = viewModel.bio,
                onValueChange = { viewModel.bio = it },
                label = { Text("نبذة عنك (اختياري)") },
                placeholder = { Text("اذكر خبرتك وما يميزك عن غيرك...") },
                modifier = Modifier.fillMaxWidth().height(100.dp),
                maxLines = 4,
            )

            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { viewModel.register() },
                enabled = viewModel.isValid() && !viewModel.isLoading,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Gold),
            ) {
                if (viewModel.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary)
                else Text("سجّل كمخلص — 50 🪙", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun RegistrationSuccessContent(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("🏛️", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text("تم التسجيل بنجاح!", style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text("ملفك المهني أُنشئ. يمكنك الآن تقديم عروضك على طلبات العملاء.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Gold)) {
            Text("ابدأ بتصفح الطلبات", fontWeight = FontWeight.Bold)
        }
    }
}
