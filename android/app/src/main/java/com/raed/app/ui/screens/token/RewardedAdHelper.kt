package com.raed.app.ui.screens.token

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

// Test unit ID — replace with real ID before release
private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

fun loadAndShowRewardedAd(
    activity: Activity,
    onRewarded: () -> Unit,
    onFailed: (String) -> Unit,
) {
    RewardedAd.load(
        activity,
        REWARDED_AD_UNIT_ID,
        AdRequest.Builder().build(),
        object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) {
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdFailedToShowFullScreenContent(error: AdError) {
                        onFailed(error.message)
                    }
                }
                ad.show(activity) { onRewarded() }
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                onFailed(error.message)
            }
        },
    )
}
