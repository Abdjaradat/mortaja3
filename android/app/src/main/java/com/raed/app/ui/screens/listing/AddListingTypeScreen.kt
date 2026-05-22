package com.raed.app.ui.screens.listing

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Gold = Color(0xFFC9A961)
private val Green = Color(0xFF2E7D32)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddListingTypeScreen(
    onBack: () -> Unit,
    onAddCar: () -> Unit,
    onAddExemption: () -> Unit,
    onPostRequest: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("نشر إعلان") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "ماذا تريد أن تنشر؟",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            ListingTypeCard(
                emoji = "🚗",
                title = "أضف سيارة للبيع",
                subtitle = "انشر سيارتك ويشوفها آلاف المشترين",
                badge = "50 🪙",
                badgeColor = Gold,
                onClick = onAddCar,
            )
            ListingTypeCard(
                emoji = "🎖",
                title = "أعلن عن إعفاء",
                subtitle = "ضابط أو وسيط؟ أعلن هنا",
                badge = "30 🪙",
                badgeColor = Gold,
                onClick = onAddExemption,
            )
            ListingTypeCard(
                emoji = "🔍",
                title = "أنا مشتري — انشر طلبك",
                subtitle = "اعرض ما تبحث عنه وسيتواصل معك الوسطاء",
                badge = "مجاناً 🎁",
                badgeColor = Green,
                onClick = onPostRequest,
            )
        }
    }
}

@Composable
private fun ListingTypeCard(
    emoji: String,
    title: String,
    subtitle: String,
    badge: String,
    badgeColor: Color = Gold,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(emoji, fontSize = 44.sp)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Surface(
                shape = MaterialTheme.shapes.small,
                color = badgeColor.copy(alpha = 0.15f),
            ) {
                Text(
                    badge,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = badgeColor,
                )
            }
        }
    }
}
