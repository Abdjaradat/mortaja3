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
            } catch (_: Exception) {}
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val data = message.data
        // Backend sends data-only messages so onMessageReceived fires in all app states.
        // Fallback to notification block covers any legacy messages.
        val title = data["title"] ?: message.notification?.title ?: return
        val body  = data["body"]  ?: message.notification?.body  ?: return
        val type  = data["type"] ?: "NEW_MESSAGE"

        val channelId = when (type) {
            "NEW_MESSAGE" -> RaedApplication.CHANNEL_MESSAGES
            "VERIFICATION_APPROVED", "VERIFICATION_REJECTED" -> RaedApplication.CHANNEL_VERIFICATION
            else -> RaedApplication.CHANNEL_GENERAL
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("type", type)
            data["conversationId"]?.let { putExtra("conversationId", it) }
            data["requestId"]?.let { putExtra("requestId", it) }
            data["listingId"]?.let { putExtra("listingId", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
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

        getSystemService(NotificationManager::class.java)
            .notify(System.currentTimeMillis().toInt(), notification)
    }
}
