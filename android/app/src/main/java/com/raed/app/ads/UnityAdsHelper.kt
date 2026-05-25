package com.raed.app.ads

import android.app.Activity
import android.content.Context
import android.util.Log
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
        Log.d("UnityAds", "Initializing with gameId: $GAME_ID, testMode: $testMode")
        UnityAds.initialize(
            context, GAME_ID, testMode,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    Log.d("UnityAds", "Unity Ads initialized successfully")
                }
                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String,
                ) {
                    Log.e("UnityAds", "Unity Ads initialization failed: $error - $message")
                }
            },
        )
    }

    fun showRewarded(activity: Activity, onRewarded: () -> Unit, onFailed: () -> Unit) {
        Log.d("UnityAds", "Loading rewarded ad: $PLACEMENT_REWARDED")
        UnityAds.load(
            PLACEMENT_REWARDED,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String) {
                    Log.d("UnityAds", "Rewarded ad loaded: $placementId — showing now")
                    UnityAds.show(
                        activity, placementId,
                        object : IUnityAdsShowListener {
                            override fun onUnityAdsShowFailure(
                                id: String,
                                error: UnityAds.UnityAdsShowError,
                                message: String,
                            ) {
                                Log.e("UnityAds", "Rewarded show failed: $error - $message")
                                onFailed()
                            }
                            override fun onUnityAdsShowStart(id: String) {
                                Log.d("UnityAds", "Rewarded ad started: $id")
                            }
                            override fun onUnityAdsShowClick(id: String) {}
                            override fun onUnityAdsShowComplete(
                                id: String,
                                state: UnityAds.UnityAdsShowCompletionState,
                            ) {
                                Log.d("UnityAds", "Rewarded ad complete: $id, state: $state")
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
                ) {
                    Log.e("UnityAds", "Rewarded ad failed to load: $error - $message")
                    onFailed()
                }
            },
        )
    }

    fun showInterstitial(activity: Activity, onComplete: () -> Unit, onFailed: () -> Unit) {
        Log.d("UnityAds", "Loading interstitial ad: $PLACEMENT_INTERSTITIAL")
        UnityAds.load(
            PLACEMENT_INTERSTITIAL,
            object : IUnityAdsLoadListener {
                override fun onUnityAdsAdLoaded(placementId: String) {
                    Log.d("UnityAds", "Interstitial ad loaded: $placementId — showing now")
                    UnityAds.show(
                        activity, placementId,
                        object : IUnityAdsShowListener {
                            override fun onUnityAdsShowFailure(
                                id: String,
                                error: UnityAds.UnityAdsShowError,
                                message: String,
                            ) {
                                Log.e("UnityAds", "Interstitial show failed: $error - $message")
                                onFailed()
                            }
                            override fun onUnityAdsShowStart(id: String) {
                                Log.d("UnityAds", "Interstitial ad started: $id")
                            }
                            override fun onUnityAdsShowClick(id: String) {}
                            override fun onUnityAdsShowComplete(
                                id: String,
                                state: UnityAds.UnityAdsShowCompletionState,
                            ) {
                                Log.d("UnityAds", "Interstitial ad complete: $id, state: $state")
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
                    Log.e("UnityAds", "Interstitial ad failed to load: $error - $message")
                    onFailed()
                }
            },
        )
    }

    fun createBannerView(activity: Activity): BannerView {
        Log.d("UnityAds", "Creating banner view: $PLACEMENT_BANNER")
        return BannerView(activity, PLACEMENT_BANNER, UnityBannerSize(320, 50)).apply {
            listener = object : BannerView.IListener {
                override fun onBannerLoaded(bannerAdView: BannerView) {
                    Log.d("UnityAds", "Banner loaded successfully")
                }
                override fun onBannerClick(bannerAdView: BannerView) {}
                override fun onBannerFailedToLoad(bannerAdView: BannerView, errorInfo: BannerErrorInfo) {
                    Log.e("UnityAds", "Banner failed to load: ${errorInfo.errorMessage}")
                }
                override fun onBannerLeftApplication(bannerView: BannerView) {}
                override fun onBannerShown(bannerAdView: BannerView) {
                    Log.d("UnityAds", "Banner shown")
                }
            }
        }
    }
}
