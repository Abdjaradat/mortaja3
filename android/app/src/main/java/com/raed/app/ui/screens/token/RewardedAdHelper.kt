package com.raed.app.ui.screens.token

import android.app.Activity
import com.raed.app.ads.UnityAdsHelper

fun loadAndShowRewardedAd(
    activity: Activity,
    onRewarded: () -> Unit,
    onFailed: (String) -> Unit,
) {
    UnityAdsHelper.loadAndShowRewarded(
        activity = activity,
        onRewarded = onRewarded,
        onFailed = { onFailed("Unity rewarded ad failed") },
    )
}
