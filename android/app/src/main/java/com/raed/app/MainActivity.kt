package com.raed.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.raed.app.navigation.RaedNavGraph
import com.raed.app.navigation.Screen
import com.raed.app.ui.theme.RaedTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()
        pendingRoute = intent.toNotificationRoute()
        setContent {
            RaedTheme {
                RaedNavGraph(
                    pendingRoute = pendingRoute,
                    onPendingRouteConsumed = { pendingRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingRoute = intent.toNotificationRoute()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                0,
            )
        }
    }
}

private fun Intent.toNotificationRoute(): String? = when (getStringExtra("type")) {
    "NEW_MESSAGE" -> getStringExtra("conversationId")?.let { Screen.Conversation.createRoute(it) }
    "NEW_BID"     -> getStringExtra("requestId")?.let { Screen.Bid.createRoute(it) }
    "TOKENS_EARNED" -> Screen.TokenWallet.route
    else -> null
}
