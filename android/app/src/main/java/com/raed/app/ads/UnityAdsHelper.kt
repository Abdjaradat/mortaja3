package com.raed.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.ads.UnityAdsLoadOptions
import com.unity3d.ads.UnityAdsShowOptions
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize

object UnityAdsHelper {
    private const val GAME_ID = "800000934"
    const val PLACEMENT_REWARDED     = "Rewarded_Android"
    const val PLACEMENT_INTERSTITIAL = "Interstitial_Android"
    const val PLACEMENT_BANNER       = "Banner_Android"
    private var isInitialized = false

    fun initialize(context: Context, testMode: Boolean) {
        UnityAds.initialize(context.applicationContext, GAME_ID, testMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    isInitialized = true
                    Log.d("UnityAds", "Initialized successfully")
                }
                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String,
                ) {
                    Log.e("UnityAds", "Init failed: $error - $message")
                }
            },
        )
    }

    fun loadAndShowRewarded(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit) {
        if (!isInitialized) { Log.e("UnityAds", "Not initialized"); onFailed(); return }
        UnityAds.load(PLACEMENT_REWARDED, UnityAdsLoadOptions(),
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String) {
                    Log.d("UnityAds", "Ad loaded: $placementId")
                    UnityAds.show(activity, PLACEMENT_REWARDED, UnityAdsShowOptions(),
                        object : IUnityAdsShowListener {
                            override fun onUnityAdsShowFailure(
                                placementId: String,
                                error: UnityAds.UnityAdsShowError,
                                message: String,
                            ) {
                                Log.e("UnityAds", "Show failed: $error - $message")
                                onFailed()
                            }
                            override fun onUnityAdsShowStart(placementId: String) {}
                            override fun onUnityAdsShowClick(placementId: String) {}
                            override fun onUnityAdsShowComplete(
                                placementId: String,
                                state: UnityAds.UnityAdsShowCompletionState,
                            ) {
                                if (state == UnityAds.UnityAdsShowCompletionState.COMPLETED) {
                                    Log.d("UnityAds", "Ad completed - rewarding user")
                                    onRewarded()
                                } else {
                                    onFailed()
                                }
                            }
                        },
                    )
                }
                override fun onUnityAdsFailedToLoad(
                    placementId: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String,
                ) {
                    Log.e("UnityAds", "Load failed: $error - $message")
                    onFailed()
                }
            },
        )
    }

    fun showInterstitial(activity: Activity, onComplete: () -> Unit, onFailed: () -> Unit) {
        if (!isInitialized) { Log.e("UnityAds", "Not initialized"); onFailed(); return }
        UnityAds.load(PLACEMENT_INTERSTITIAL, UnityAdsLoadOptions(),
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String) {
                    UnityAds.show(activity, placementId, UnityAdsShowOptions(),
                        object : IUnityAdsShowListener {
                            override fun onUnityAdsShowFailure(
                                id: String,
                                error: UnityAds.UnityAdsShowError,
                                message: String,
                            ) {
                                Log.e("UnityAds", "Interstitial show failed: $error - $message")
                                onFailed()
                            }
                            override fun onUnityAdsShowStart(id: String) {}
                            override fun onUnityAdsShowClick(id: String) {}
                            override fun onUnityAdsShowComplete(
                                id: String,
                                state: UnityAds.UnityAdsShowCompletionState,
                            ) {
                                Log.d("UnityAds", "Interstitial complete: $state")
                                onComplete()
                            }
                        },
                    )
                }
                override fun onUnityAdsFailedToLoad(
                    id: String,
                    error: UnityAds.UnityAdsLoadError,
                    message: String,
                ) {
                    Log.e("UnityAds", "Interstitial load failed: $error - $message")
                    onFailed()
                }
            },
        )
    }

    fun createBannerView(context: Context, onLoaded: (BannerView) -> Unit) {
        if (!isInitialized) { Log.e("UnityAds", "Not initialized for banner"); return }
        val banner = BannerView(
            context as Activity,
            PLACEMENT_BANNER,
            UnityBannerSize.getDynamicSize(context),
        )
        banner.listener = object : BannerView.IListener {
            override fun onBannerLoaded(bannerAdView: BannerView) {
                Log.d("UnityAds", "Banner loaded")
                onLoaded(bannerAdView)
            }
            override fun onBannerFailedToLoad(bannerAdView: BannerView, errorInfo: BannerErrorInfo) {
                Log.e("UnityAds", "Banner failed: ${errorInfo.errorMessage}")
            }
            override fun onBannerClick(bannerAdView: BannerView) {}
            override fun onBannerLeftApplication(bannerAdView: BannerView) {}
            override fun onBannerShown(bannerAdView: BannerView) {}
        }
        banner.load()
    }
}
