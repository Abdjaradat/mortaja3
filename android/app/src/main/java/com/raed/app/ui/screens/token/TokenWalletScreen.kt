package com.raed.app.ui.screens.token

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import kotlinx.coroutines.launch
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.raed.app.data.api.models.TokenTransactionDto

private data class TokenPackage(
    val name: String,
    val tokens: Int,
    val priceJod: String,
    val popular: Boolean = false,
)

private val TOKEN_PACKAGES = listOf(
    TokenPackage("مبتدئ", 100, "0.99"),
    TokenPackage("أساسي", 500, "3.99", popular = true),
    TokenPackage("محترف", 1000, "6.99"),
    TokenPackage("نخبة", 3000, "16.99"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TokenWalletScreen(
    onBack: () -> Unit,
    viewModel: TokenViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val clipboard = LocalClipboardManager.current
    val activity = LocalContext.current as Activity
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.loadBalance() }

    state.error?.let { error ->
        LaunchedEffect(error) {
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("محفظة التوكنز") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "رجوع")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
        ) {
            item {
                BalanceCard(
                    balance = state.balance,
                    totalEarned = state.totalEarned,
                    totalSpent = state.totalSpent,
                )
            }

            item {
                WatchAdCard(
                    isLoading = state.isWatchingAd,
                    dailyCount = state.adDailyCount,
                    onWatchAd = {
                        loadAndShowRewardedAd(
                            activity = activity,
                            onRewarded = { viewModel.watchAd() },
                            onFailed = { viewModel.clearError() },
                        )
                    },
                )
            }

            item {
                Text(
                    "شراء توكنز",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            item {
                val scope = androidx.compose.runtime.rememberCoroutineScope()
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TOKEN_PACKAGES.forEach { pkg ->
                        TokenPackageCard(
                            pkg = pkg,
                            onBuy = {
                                scope.launch {
                                    snackbarHostState.showSnackbar("المشتريات داخل التطبيق قريباً 🛒")
                                }
                            },
                        )
                    }
                }
            }

            if (state.referralCode.isNotEmpty()) {
                item {
                    ReferralCard(
                        code = state.referralCode,
                        onCopy = { clipboard.setText(AnnotatedString(state.referralCode)) },
                    )
                }
            }

            item {
                Text(
                    "آخر المعاملات",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }

            if (state.transactions.isEmpty() && !state.isLoading) {
                item {
                    Text(
                        "لا توجد معاملات بعد",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }

            items(state.transactions) { tx ->
                TransactionRow(tx)
            }
        }
    }
}

@Composable
private fun BalanceCard(balance: Int, totalEarned: Int, totalSpent: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("رصيدك الحالي", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "🪙",
                    fontSize = 32.sp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "$balance",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                StatItem(label = "مكتسب", value = "+$totalEarned", color = MaterialTheme.colorScheme.primary)
                StatItem(label = "مُنفق", value = "-$totalSpent", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun WatchAdCard(isLoading: Boolean, dailyCount: Int, onWatchAd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("شاهد إعلاناً", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "احصل على 🪙 10 • ($dailyCount/20 اليوم)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onWatchAd,
                enabled = !isLoading && dailyCount < 20,
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text(if (dailyCount >= 20) "اكتمل" else "شاهد")
                }
            }
        }
    }
}

@Composable
private fun ReferralCard(code: String, onCopy: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("كود الإحالة", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    code,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                IconButton(onClick = onCopy) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ")
                }
            }
            Text(
                "شارك الكود واحصل على 🪙 100 عند تسجيل كل شخص",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val Gold = Color(0xFFC9A961)

@Composable
private fun TokenPackageCard(pkg: TokenPackage, onBuy: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = if (pkg.popular)
            CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.12f))
        else
            CardDefaults.cardColors(),
        border = if (pkg.popular)
            androidx.compose.foundation.BorderStroke(1.5.dp, Gold)
        else null,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("🪙", fontSize = 28.sp)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            "%,d توكن".format(pkg.tokens),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        if (pkg.popular) {
                            Surface(
                                shape = MaterialTheme.shapes.extraSmall,
                                color = Gold,
                            ) {
                                Text(
                                    "الأكثر شيوعاً",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                    }
                    Text(
                        pkg.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = onBuy,
                colors = if (pkg.popular)
                    ButtonDefaults.buttonColors(containerColor = Gold)
                else
                    ButtonDefaults.buttonColors(),
            ) {
                Text("${pkg.priceJod} د.أ")
            }
        }
    }
}

@Composable
private fun TransactionRow(tx: TokenTransactionDto) {
    val isEarn = tx.type == "EARN"
    val amountText = if (isEarn) "+${tx.amount}" else "${tx.amount}"
    val amountColor = if (isEarn) Color(0xFF2E7D32) else Color(0xFFC62828)
    val emoji = when (tx.reason) {
        "WELCOME" -> "🎁"
        "AD_WATCH" -> "📺"
        "REFERRAL" -> "👥"
        "POST_LISTING" -> "📋"
        "POST_EXEMPTION" -> "📄"
        "REVEAL_CONTACT" -> "📞"
        "START_CONVERSATION" -> "💬"
        "BOOST_LISTING" -> "🚀"
        "RENEW_LISTING" -> "🔄"
        "PURCHASE" -> "💳"
        else -> "🪙"
    }
    val label = when (tx.reason) {
        "WELCOME" -> "مكافأة التسجيل"
        "AD_WATCH" -> "مشاهدة إعلان"
        "REFERRAL" -> "إحالة"
        "POST_LISTING" -> "نشر إعلان مركبة"
        "POST_EXEMPTION" -> "نشر إعلان إعفاء"
        "REVEAL_CONTACT" -> "كشف معلومات التواصل"
        "START_CONVERSATION" -> "بدء محادثة"
        "BOOST_LISTING" -> "تعزيز الإعلان"
        "RENEW_LISTING" -> "تجديد الإعلان"
        "PURCHASE" -> "شراء توكنز"
        else -> "معاملة"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    "الرصيد بعد: ${tx.balanceAfter}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(amountText, fontWeight = FontWeight.Bold, color = amountColor)
    }
}
