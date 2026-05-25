package com.raed.app.ui.components

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.raed.app.ui.screens.token.loadAndShowRewardedAd

private const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111" // test banner
private val Gold = Color(0xFFC9A961)

object AdSessionTracker {
    private var count = 0
    fun canShowAd(): Boolean = count < 3
    fun recordAdShown() { if (count < 3) count++ }
    fun undoLastRecord() { if (count > 0) count-- }
}

@Composable
fun AdBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = BANNER_AD_UNIT_ID
                    loadAd(AdRequest.Builder().build())
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun AdEarnCard(
    onAdWatched: () -> Unit,
    label: String = "شاهد إعلاناً واكسب 10 توكن 🪙",
    modifier: Modifier = Modifier,
) {
    if (!AdSessionTracker.canShowAd()) return
    val activity = LocalContext.current as? Activity ?: return

    Card(
        onClick = {
            AdSessionTracker.recordAdShown()
            loadAndShowRewardedAd(
                activity = activity,
                onRewarded = { onAdWatched() },
                onFailed = { AdSessionTracker.undoLastRecord() },
            )
        },
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Gold.copy(alpha = 0.08f)),
        border = BorderStroke(1.dp, Gold.copy(alpha = 0.35f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("▶", fontSize = 13.sp, color = Gold)
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = Gold,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
