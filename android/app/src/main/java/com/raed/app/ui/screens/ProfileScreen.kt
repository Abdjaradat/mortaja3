package com.raed.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.R
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.MeResponse
import com.raed.app.data.api.models.UpdateProfileRequest
import com.raed.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: RaedApi,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _profile = MutableStateFlow<MeResponse?>(null)
    val profile: StateFlow<MeResponse?> = _profile.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut.asStateFlow()

    init {
        loadProfile()
    }

    fun loadProfile() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val response = api.getMe()
                if (response.isSuccessful) {
                    _profile.value = response.body()
                }
            } catch (_: Exception) {
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun saveProfile(fullName: String, governorate: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val response = api.updateMe(
                    UpdateProfileRequest(fullName = fullName.ifBlank { null }, governorate = governorate.ifBlank { null })
                )
                if (response.isSuccessful) {
                    _profile.value = response.body()
                }
            } catch (_: Exception) {
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.clearSession()
            _loggedOut.value = true
        }
    }
}

private val JORDANIAN_GOVERNORATES = listOf(
    "عمّان", "إربد", "الزرقاء", "البلقاء", "مأدبا", "الكرك",
    "الطفيلة", "معان", "العقبة", "جرش", "عجلون", "المفرق",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileContent(
    onNavigateToOfficerVerification: () -> Unit,
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()

    var fullName by remember(profile) { mutableStateOf(profile?.fullName ?: "") }
    var governorate by remember(profile) { mutableStateOf(profile?.governorate ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    LaunchedEffect(loggedOut) {
        if (loggedOut) onLoggedOut()
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(R.string.logout)) },
            text = { Text("هل تريد تسجيل الخروج؟") },
            confirmButton = {
                TextButton(onClick = { viewModel.logout() }) { Text("نعم") }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (isLoading && profile == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.profile),
                    style = MaterialTheme.typography.headlineSmall,
                )

                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("الاسم الكامل") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving,
                )

                ExposedDropdownMenuBox(
                    expanded = dropdownExpanded,
                    onExpandedChange = { dropdownExpanded = it },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    OutlinedTextField(
                        value = governorate,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("المحافظة") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        enabled = !isSaving,
                    )
                    ExposedDropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false },
                    ) {
                        JORDANIAN_GOVERNORATES.forEach { gov ->
                            DropdownMenuItem(
                                text = { Text(gov) },
                                onClick = { governorate = gov; dropdownExpanded = false },
                            )
                        }
                    }
                }

                Button(
                    onClick = { viewModel.saveProfile(fullName, governorate) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(stringResource(R.string.save))
                    }
                }

                // Officer-specific section
                if (profile?.userType == "OFFICER") {
                    HorizontalDivider()
                    val verState = profile?.officerProfile?.verificationState
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when (verState) {
                                "VERIFIED" -> MaterialTheme.colorScheme.primaryContainer
                                "REJECTED" -> MaterialTheme.colorScheme.errorContainer
                                else -> MaterialTheme.colorScheme.secondaryContainer
                            }
                        ),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "حالة التوثيق: " + when (verState) {
                                    "VERIFIED" -> stringResource(R.string.verified)
                                    "PENDING" -> stringResource(R.string.pending_verification)
                                    "REJECTED" -> "مرفوض"
                                    else -> "لم يُقدَّم بعد"
                                },
                                style = MaterialTheme.typography.titleSmall,
                            )

                            if (verState == null || verState == "REJECTED") {
                                OutlinedButton(
                                    onClick = onNavigateToOfficerVerification,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(Icons.Outlined.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("تقديم طلب التوثيق")
                                }
                            }
                        }
                    }
                }

                HorizontalDivider()

                OutlinedButton(
                    onClick = { showLogoutDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.logout))
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
