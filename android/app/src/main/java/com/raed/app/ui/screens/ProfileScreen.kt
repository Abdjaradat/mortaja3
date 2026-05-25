package com.raed.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import com.raed.app.ui.components.UnityBannerCard
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.raed.app.R
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.MeResponse
import com.raed.app.data.api.models.UpdateProfileRequest
import com.raed.app.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

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

    fun saveProfile(fullName: String, governorate: String, phoneNumber: String) {
        viewModelScope.launch {
            _isSaving.value = true
            try {
                val response = api.updateMe(
                    UpdateProfileRequest(
                        fullName = fullName.ifBlank { null },
                        governorate = governorate.ifBlank { null },
                        phoneNumber = phoneNumber.filter { it.isDigit() }.ifBlank { null },
                    )
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

    fun uploadAndSavePhoto(uri: android.net.Uri) {
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            try {
                val userId = Firebase.auth.currentUser?.uid ?: return@launch
                val timestamp = System.currentTimeMillis()
                val ref = Firebase.storage.reference.child("profiles/$userId/avatar_$timestamp.jpg")
                ref.putFile(uri).await()
                val url = ref.downloadUrl.await().toString()
                val response = api.updateMe(UpdateProfileRequest(photoUrl = url))
                if (response.isSuccessful) _profile.value = response.body()
            } catch (_: Exception) {
            } finally {
                _isUploadingPhoto.value = false
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
    onLoggedOut: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val profile by viewModel.profile.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsState()
    val loggedOut by viewModel.loggedOut.collectAsState()

    var fullName by remember(profile) { mutableStateOf(profile?.fullName ?: "") }
    var governorate by remember(profile) { mutableStateOf(profile?.governorate ?: "") }
    var phoneNumber by remember(profile) { mutableStateOf(profile?.phoneNumber ?: "") }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri -> uri?.let { viewModel.uploadAndSavePhoto(it) } }

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

                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .clickable(enabled = !isUploadingPhoto) {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        val photoUrl = profile?.photoUrl
                        if (photoUrl != null && !isUploadingPhoto) {
                            AsyncImage(
                                model = photoUrl,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else if (isUploadingPhoto) {
                            CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 3.dp)
                        } else {
                            Text("👤", fontSize = 36.sp)
                        }
                    }
                    if (!isUploadingPhoto) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(24.dp)
                                .align(Alignment.BottomEnd),
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Text("✎", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimary)
                            }
                        }
                    }
                }

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

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it.filter { c -> c.isDigit() } },
                    label = { Text("رقم التواصل") },
                    placeholder = { Text("07X XXX XXXX") },
                    prefix = { Text("+962 ") },
                    supportingText = { Text("يُستخدم عند الكشف عن رقمك في الإعلانات", style = MaterialTheme.typography.labelSmall) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isSaving,
                )

                Button(
                    onClick = { viewModel.saveProfile(fullName, governorate, phoneNumber) },
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

                Spacer(Modifier.height(8.dp))
                UnityBannerCard()
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
