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

@Composable
fun UserTypeScreen(
    viewModel: AuthViewModel,
    onNeedsProfile: () -> Unit,
    onAuthenticated: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState) {
        when (uiState) {
            is AuthUiState.NeedsProfile -> onNeedsProfile()
            is AuthUiState.Authenticated -> onAuthenticated()
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "أنت...",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "اختر نوع حسابك. لا يمكن تغيير هذا لاحقاً.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(32.dp))

        UserTypeCard(
            title = "ضابط عامل",
            subtitle = "أنا في الخدمة الفعلية ولديّ حق إعفاء",
            emoji = "🎖",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            enabled = uiState !is AuthUiState.Loading,
            onClick = { viewModel.confirmUserType("OFFICER", "ACTIVE") },
        )

        Spacer(modifier = Modifier.height(12.dp))

        UserTypeCard(
            title = "ضابط متقاعد",
            subtitle = "أنا متقاعد ولديّ حق إعفاء غير مستخدم",
            emoji = "🪖",
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            enabled = uiState !is AuthUiState.Loading,
            onClick = { viewModel.confirmUserType("OFFICER", "RETIRED") },
        )

        Spacer(modifier = Modifier.height(12.dp))

        UserTypeCard(
            title = "معفي طبي",
            subtitle = "لديّ وثيقة إعاقة وأريد الاستفادة من الإعفاء",
            emoji = "♿",
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            enabled = uiState !is AuthUiState.Loading,
            onClick = { viewModel.confirmUserType("MEDICAL_EXEMPT") },
        )

        Spacer(modifier = Modifier.height(12.dp))

        UserTypeCard(
            title = "مشتري",
            subtitle = "أريد شراء سيارة بسعر مميّز عبر ضابط",
            emoji = "🚗",
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            enabled = uiState !is AuthUiState.Loading,
            onClick = { viewModel.confirmUserType("BUYER") },
        )

        if (uiState is AuthUiState.Loading) {
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator()
        }

        if (uiState is AuthUiState.Error) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = (uiState as AuthUiState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun UserTypeCard(
    title: String,
    subtitle: String,
    emoji: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        enabled = enabled,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(emoji, style = MaterialTheme.typography.headlineMedium)
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.8f),
                )
            }
        }
    }
}
