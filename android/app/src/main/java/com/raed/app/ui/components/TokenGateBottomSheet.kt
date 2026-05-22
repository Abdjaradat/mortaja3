package com.raed.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenGateBottomSheet(
    required: Int,
    current: Int,
    onWatchAd: () -> Unit,
    onBuyTokens: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("🪙", fontSize = 48.sp)
            Text(
                text = "رصيدك غير كافٍ",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "تحتاج $required توكن لإتمام هذا الإجراء\nرصيدك الحالي: $current توكن",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = { onWatchAd(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("شاهد إعلاناً واحداً (+10🪙)")
            }
            OutlinedButton(
                onClick = { onBuyTokens(); onDismiss() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("اشترِ توكنز")
            }
        }
    }
}
