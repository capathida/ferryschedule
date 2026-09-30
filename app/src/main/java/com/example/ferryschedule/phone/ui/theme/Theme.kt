package com.example.ferryschedule.phone.ui.theme

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

val OceanBlue = Color(0xFF005A9E)
val LightOcean = Color(0xFFE5F1FB)
val DeepNavy = Color(0xFF072138)
val AccentTeal = Color(0xFF0098A6)
val SoftGray = Color(0xFFF4F6F9)

private val LightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = LightOcean,
    onPrimaryContainer = DeepNavy,
    secondary = AccentTeal,
    background = SoftGray,
    surface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF82B1FF),
    onPrimary = Color.Black,
    primaryContainer = DeepNavy,
    onPrimaryContainer = Color.White,
    secondary = AccentTeal,
    background = Color(0xFF121417),
    surface = Color(0xFF1B1E23)
)

@Composable
fun FerryTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
