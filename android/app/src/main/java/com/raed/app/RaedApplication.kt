package com.raed.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.google.android.gms.ads.MobileAds
import com.raed.app.BuildConfig
import com.raed.app.ads.UnityAdsHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class RaedApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
        UnityAdsHelper.initialize(this, BuildConfig.DEBUG)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MESSAGES,
                    "الرسائل",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "إشعارات الرسائل الجديدة" }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_VERIFICATION,
                    "التوثيق",
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { description = "تحديثات حالة التوثيق" }
            )

            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_GENERAL,
                    "عام",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "إشعارات عامة — توكن وعروض" }
            )
        }
    }

    companion object {
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_VERIFICATION = "verification"
        const val CHANNEL_GENERAL = "general"
    }
}
