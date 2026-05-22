package com.raed.app.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private val JORDANIAN_GOVERNORATES = listOf(
    "عمّان", "إربد", "الزرقاء", "البلقاء", "مأدبا", "الكرك",
    "الطفيلة", "معان", "العقبة", "جرش", "عجلون", "المفرق",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSetupScreen(
    viewModel: AuthViewModel,
    onComplete: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var fullName by remember { mutableStateOf("") }
    var governorate by remember { mutableStateOf("") }
    var dropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Authenticated) onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "أكمل ملفك الشخصي",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "هذه المعلومات تظهر للمستخدمين الآخرين",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = fullName,
            onValueChange = { fullName = it },
            label = { Text("الاسم الكامل") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = uiState !is AuthUiState.Loading,
        )

        Spacer(modifier = Modifier.height(16.dp))

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
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                enabled = uiState !is AuthUiState.Loading,
            )
            ExposedDropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
            ) {
                JORDANIAN_GOVERNORATES.forEach { gov ->
                    DropdownMenuItem(
                        text = { Text(gov) },
                        onClick = {
                            governorate = gov
                            dropdownExpanded = false
                        },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (uiState is AuthUiState.Error) {
            Text(
                text = (uiState as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 12.dp),
                textAlign = TextAlign.Center,
            )
        }

        Button(
            onClick = { viewModel.submitProfile(fullName.trim(), governorate) },
            modifier = Modifier.fillMaxWidth(),
            enabled = fullName.length >= 2 && governorate.isNotEmpty() && uiState !is AuthUiState.Loading,
        ) {
            if (uiState is AuthUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text("ابدأ")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
