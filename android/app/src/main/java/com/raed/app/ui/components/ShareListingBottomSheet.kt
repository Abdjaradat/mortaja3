package com.raed.app.ui.components

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.raed.app.BuildConfig

private val Gold = Color(0xFFC9A961)

private data class SharePlatform(
    val label: String,
    val emoji: String,
    val packageName: String?,
)

private val SHARE_PLATFORMS = listOf(
    SharePlatform("واتساب",    "📱", "com.whatsapp"),
    SharePlatform("فيسبوك",   "📘", "com.facebook.katana"),
    SharePlatform("تيك توك",  "🎵", "com.zhiliaoapp.musically"),
    SharePlatform("إنستغرام", "📸", "com.instagram.android"),
    SharePlatform("تيليغرام", "✈️", "org.telegram.messenger"),
    SharePlatform("مشاركة عامة", "🔗", null),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShareListingBottomSheet(
    listingTitle: String,
    listingPrice: Int?,
    onShare: (platform: String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shareText = buildShareText(listingTitle, listingPrice)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "شارك إعلانك واكسب توكنز! 🎁",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                "كل مشاركة = 10 توكن (حتى 3 مرات يومياً = 30 توكن)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(SHARE_PLATFORMS) { platform ->
                    SharePlatformButton(
                        label = platform.label,
                        emoji = platform.emoji,
                        onClick = {
                            launchShareIntent(context, shareText, platform.packageName)
                            onShare(platform.label)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SharePlatformButton(label: String, emoji: String, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(72.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(emoji, fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}

private fun buildShareText(title: String, price: Int?): String {
    val priceStr = price?.let { "$it د.أ" } ?: ""
    return buildString {
        appendLine("🚗 شاهد هذا الإعلان على تطبيق مرتجع")
        appendLine(title)
        if (priceStr.isNotEmpty()) appendLine(priceStr)
        appendLine("📥 حمّل التطبيق: ${BuildConfig.APP_DOWNLOAD_URL}")
    }.trim()
}

private fun launchShareIntent(context: android.content.Context, text: String, packageName: String?) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }

    if (packageName != null) {
        val pm = context.packageManager
        val isInstalled = try {
            pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES)
            true
        } catch (_: PackageManager.NameNotFoundException) { false }

        if (isInstalled) {
            intent.setPackage(packageName)
        }
    }

    context.startActivity(Intent.createChooser(intent, "مشاركة الإعلان"))
}
