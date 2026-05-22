package com.raed.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val RaedLightColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = OnNavy,
    primaryContainer = NavyContainer,
    onPrimaryContainer = OnNavyContainer,
    secondary = GoldSecondary,
    onSecondary = OnGold,
    secondaryContainer = GoldContainer,
    onSecondaryContainer = OnGoldContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Surface,
    onSurface = OnSurface,
    outline = Outline,
    error = ErrorRed,
    onError = OnError,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
)

private val RaedDarkColorScheme = darkColorScheme(
    primary = NavyContainer,
    onPrimary = NavyDark,
    primaryContainer = NavyDark,
    onPrimaryContainer = OnNavyContainer,
    secondary = GoldContainer,
    onSecondary = OnGoldContainer,
    secondaryContainer = GoldDark,
    onSecondaryContainer = GoldContainer,
    error = ErrorContainer,
    onError = OnErrorContainer,
    errorContainer = ErrorRed,
    onErrorContainer = ErrorContainer,
)

@Composable
fun RaedTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) RaedDarkColorScheme else RaedLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RaedTypography,
        content = content,
    )
}
