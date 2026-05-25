package com.raed.app.ui.screens.auth

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNeedsUserType: () -> Unit,
    onNeedsProfile: () -> Unit,
    onAuthenticated: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val activity = LocalContext.current as Activity

    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var codeSent by remember { mutableStateOf(false) }

    val isLoading = uiState is AuthUiState.Loading

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.OtpSent    -> codeSent = true
            is AuthUiState.NeedsUserType -> onNeedsUserType()
            is AuthUiState.NeedsProfile  -> onNeedsProfile()
            is AuthUiState.Authenticated -> onAuthenticated()
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "مرتجع",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = "منصة إعفاءات المركبات",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )

        Spacer(Modifier.height(48.dp))

        if (!codeSent) {
            // — Phone number step —
            Text(
                text = "أدخل رقم هاتفك",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "+962",
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 18.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                }
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() } },
                    placeholder = { Text("7xxxxxxxxx") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Phone,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    enabled = !isLoading,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    viewModel.clearError()
                    val digits = phone.trimStart().removePrefix("0")
                    viewModel.sendOtp("+962$digits", activity)
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = phone.length >= 9 && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("إرسال رمز التحقق")
                }
            }
        } else {
            // — OTP step —
            Text(
                text = "أدخل رمز التحقق",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "تم إرسال رمز مكوّن من 6 أرقام إلى +962${phone.removePrefix("0")}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = code,
                onValueChange = { code = it.filter { c -> c.isDigit() }.take(6) },
                placeholder = { Text("------", fontSize = 20.sp, letterSpacing = 8.sp) },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                enabled = !isLoading,
            )

            Spacer(Modifier.height(8.dp))

            if (uiState is AuthUiState.Error) {
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }

            Button(
                onClick = { viewModel.verifyCode(code) },
                modifier = Modifier.fillMaxWidth(),
                enabled = code.length == 6 && !isLoading,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("تحقق")
                }
            }

            TextButton(
                onClick = {
                    code = ""
                    codeSent = false
                    viewModel.resetToIdle()
                },
                enabled = !isLoading,
            ) {
                Text("تغيير رقم الهاتف")
            }
        }
    }
}
