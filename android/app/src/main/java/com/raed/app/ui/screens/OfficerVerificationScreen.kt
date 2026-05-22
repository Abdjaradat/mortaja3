package com.raed.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.google.firebase.storage.FirebaseStorage
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.OfficerProfileRequest
import com.raed.app.data.local.SessionDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

sealed class VerificationState {
    object Idle : VerificationState()
    object Uploading : VerificationState()
    object Submitting : VerificationState()
    object Success : VerificationState()
    data class Error(val message: String) : VerificationState()
}

@HiltViewModel
class OfficerVerificationViewModel @Inject constructor(
    private val api: RaedApi,
    private val sessionDataStore: SessionDataStore,
) : ViewModel() {

    private val _state = MutableStateFlow<VerificationState>(VerificationState.Idle)
    val state: StateFlow<VerificationState> = _state.asStateFlow()

    fun submit(rank: String, status: String, documentUri: Uri) {
        viewModelScope.launch {
            val userId = sessionDataStore.userId.firstOrNull() ?: run {
                _state.value = VerificationState.Error("يرجى تسجيل الدخول أولاً")
                return@launch
            }

            _state.value = VerificationState.Uploading

            try {
                val storageRef = FirebaseStorage.getInstance()
                    .reference
                    .child("officers/$userId/id_document")

                storageRef.putFile(documentUri).await()
                val downloadUrl = storageRef.downloadUrl.await().toString()

                _state.value = VerificationState.Submitting

                val response = api.submitOfficerProfile(
                    OfficerProfileRequest(rank = rank, status = status, documentUrl = downloadUrl)
                )

                if (response.isSuccessful) {
                    _state.value = VerificationState.Success
                } else {
                    val code = response.code()
                    _state.value = VerificationState.Error(
                        if (code == 409) "تم تقديم طلب توثيق مسبقاً" else "فشل تقديم الطلب"
                    )
                }
            } catch (e: Exception) {
                _state.value = VerificationState.Error(e.localizedMessage ?: "حدث خطأ غير متوقع")
            }
        }
    }

    fun clearError() {
        if (_state.value is VerificationState.Error) _state.value = VerificationState.Idle
    }
}

private val RANKS = listOf(
    "رائد", "مقدم", "عقيد", "عميد", "لواء", "فريق", "فريق أول",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfficerVerificationScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: OfficerVerificationViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    var rank by remember { mutableStateOf("") }
    var rankExpanded by remember { mutableStateOf(false) }
    var isRetired by remember { mutableStateOf(false) }
    var documentUri by remember { mutableStateOf<Uri?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { documentUri = it }
    }

    val isLoading = state is VerificationState.Uploading || state is VerificationState.Submitting

    LaunchedEffect(state) {
        if (state is VerificationState.Success) onSuccess()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("توثيق هوية الضابط") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Text(
                    text = "يرجى رفع صورة واضحة من بطاقة هويتك العسكرية. ستُراجَع من قِبَل الفريق خلال 48 ساعة.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }

            // Rank dropdown
            ExposedDropdownMenuBox(
                expanded = rankExpanded,
                onExpandedChange = { rankExpanded = it },
                modifier = Modifier.fillMaxWidth(),
            ) {
                OutlinedTextField(
                    value = rank,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("الرتبة") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = rankExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                    enabled = !isLoading,
                )
                ExposedDropdownMenu(
                    expanded = rankExpanded,
                    onDismissRequest = { rankExpanded = false },
                ) {
                    RANKS.forEach { r ->
                        DropdownMenuItem(
                            text = { Text(r) },
                            onClick = { rank = r; rankExpanded = false },
                        )
                    }
                }
            }

            // Status toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("الحالة الوظيفية", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isRetired) "متقاعد" else "خدمة فعلية",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                    Spacer(Modifier.width(8.dp))
                    Switch(checked = isRetired, onCheckedChange = { isRetired = it }, enabled = !isLoading)
                }
            }

            // Document upload
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outline,
                        shape = MaterialTheme.shapes.medium,
                    )
                    .clickable(enabled = !isLoading) { imagePicker.launch("image/*") },
                contentAlignment = Alignment.Center,
            ) {
                if (documentUri != null) {
                    AsyncImage(
                        model = documentUri,
                        contentDescription = "وثيقة الهوية",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Upload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "اضغط لرفع صورة الهوية العسكرية",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            if (state is VerificationState.Error) {
                Text(
                    text = (state as VerificationState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            val statusLabel = when (state) {
                is VerificationState.Uploading -> "جارٍ رفع المستند…"
                is VerificationState.Submitting -> "جارٍ إرسال الطلب…"
                else -> "تقديم طلب التوثيق"
            }

            Button(
                onClick = {
                    val uri = documentUri ?: return@Button
                    viewModel.submit(
                        rank = rank,
                        status = if (isRetired) "RETIRED" else "ACTIVE",
                        documentUri = uri,
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = rank.isNotEmpty() && documentUri != null && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(statusLabel)
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
