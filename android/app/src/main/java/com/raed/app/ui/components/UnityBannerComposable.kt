package com.raed.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.raed.app.ads.UnityAdsHelper
import com.unity3d.services.banners.BannerView

@Composable
fun UnityBannerAd(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var bannerView by remember { mutableStateOf<BannerView?>(null) }

    LaunchedEffect(Unit) {
        UnityAdsHelper.createBannerView(context) { banner ->
            bannerView = banner
        }
    }

    bannerView?.let { banner ->
        AndroidView(
            factory = { banner },
            modifier = modifier
                .fillMaxWidth()
                .height(50.dp),
        )
    }
}
