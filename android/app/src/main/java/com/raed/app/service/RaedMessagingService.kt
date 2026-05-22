package com.raed.app.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.raed.app.MainActivity
import com.raed.app.R
import com.raed.app.RaedApplication
import com.raed.app.data.api.RaedApi
import com.raed.app.data.api.models.FcmTokenRequest
import com.raed.app.data.local.SessionDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RaedMessagingService : FirebaseMessagingService() {

    @Inject lateinit var sessionDataStore: SessionDataStore
    @Inject lateinit var api: RaedApi

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val accessToken = sessionDataStore.accessToken.firstOrNull()
                if (accessToken != null) {
                    api.updateFcmToken(FcmTokenRequest(token = token))
                }
            } catch (_: Exception) {
                // Silently fail — will retry on next token refresh
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title ?: message.data["title"] ?: return
        val body = message.notification?.body ?: message.data["body"] ?: return
        val type = message.data["type"] ?: "NEW_MESSAGE"

        val channelId = if (type == "NEW_MESSAGE") {
            RaedApplication.CHANNEL_MESSAGES
        } else {
            RaedApplication.CHANNEL_VERIFICATION
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            if (type == "NEW_MESSAGE") {
                putExtra("conversationId", message.data["conversationId"])
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
