package com.example.myhermes.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = GeminiBlue,
    secondary = GeminiPurple,
    tertiary = GeminiCyan,
    background = GeminiBackgroundDark,
    surface = GeminiSurfaceDark,
    surfaceVariant = GeminiSurfaceVariantDark,
    onBackground = Color(0xFFE3E2E6),
    onSurface = Color(0xFFE3E2E6),
    onSurfaceVariant = Color(0xFFC4C6D0),
    outline = GeminiCardBorderDark
)

private val LightColorScheme = lightColorScheme(
    primary = GeminiBlue,
    secondary = GeminiPurple,
    tertiary = GeminiCyan,
    background = GeminiBackgroundLight,
    surface = GeminiSurfaceLight,
    surfaceVariant = GeminiSurfaceVariantLight,
    onBackground = Color(0xFF191C20),
    onSurface = Color(0xFF191C20),
    onSurfaceVariant = Color(0xFF44474E),
    outline = GeminiCardBorderLight
)

@Composable
fun MyHermesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color support on Android 12+
    dynamicColor: Boolean = false, // Disabled by default for consistent Gemini branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
