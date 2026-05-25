package com.raed.app.ads

import android.app.Activity
import android.content.Context
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

object UnityAdsHelper {

    private const val GAME_ID = "800000934"
    const val PLACEMENT_REWARDED = "Rewarded_Android"
    const val PLACEMENT_INTERSTITIAL = "Interstitial_Android"
    const val PLACEMENT_BANNER = "Banner_Android"

    fun initialize(context: Context, testMode: Boolean) {
        UnityAds.initialize(
            context, GAME_ID, testMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {}
                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String,
                ) {}
            },
        )
    }

    fun showRewarded(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit) {
        UnityAds.load(
            PLACEMENT_REWARDED,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String) {
                    UnityAds.show(
                        activity, placementId,
                        object : IUnityAdsShowListener {
                            override fun onUnityAdsShowFailure(
                                id: String,
                                error: UnityAds.UnityAdsShowError,
                                message: String,
                            ) { onFailed() }
                            override fun onUnityAdsShowStart(id: String) {}
                            override fun onUnityAdsShowClick(id: String) {}
                            override fun onUnityAdsShowComplete(
                                id: String,
                                state: UnityAds.UnityAdsShowCompletionState,
                            ) {
                                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) onRewarded()
                                else onFailed()
                            }
                        },
                    )
                }
                override fun onUnityAdsFailedToLoad(
                    id: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String,
                ) { onFailed() }
            },
        )
    }

    fun showInterstitial(activity: Activity, onComplete: () -> Unit, onFailed: () -> Unit) {
        UnityAds.load(
            PLACEMENT_INTERSTITIAL,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String) {
                    UnityAds.show(
                        activity, placementId,
                        object : IUnityAdsShowListener {
                            override fun onUnityAdsShowFailure(
                                id: String,
                                error: UnityAds.UnityAdsShowError,
                                message: String,
                            ) { onFailed() }
                            override fun onUnityAdsShowStart(id: String) {}
                            override fun onUnityAdsShowClick(id: String) {}
                            override fun onUnityAdsShowComplete(
                                id: String,
                                state: UnityAds.UnityAdsShowCompletionState,
                            ) { onComplete() }
                        },
                    )
                }
                override fun onUnityAdsFailedToLoad(
                    id: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String,
                ) { onFailed() }
            },
        )
    }

    fun createBannerView(activity: Activity): BannerView =
        BannerView(activity, PLACEMENT_BANNER, UnityBannerSize(320, 50)).apply {
            listener = object : BannerView.IListener {
                override fun onBannerLoaded(bannerAdView: BannerView) {}
                override fun onBannerClick(bannerAdView: BannerView) {}
                override fun onBannerFailedToLoad(bannerAdView: BannerView, errorInfo: BannerErrorInfo) {}
                override fun onBannerLeftApplication(bannerView: BannerView) {}
                override fun onBannerShown(bannerAdView: BannerView) {}
            }
        }
}
