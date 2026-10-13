package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = PosIndigoLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = PosEmeraldLight,
    onSecondary = Color.Black,
    secondaryContainer = PosEmeraldDark,
    onSecondaryContainer = Color(0xFFD1FAE5),
    tertiary = PosAmber,
    background = PosSlate900,
    surface = PosSlate800,
    surfaceVariant = Color(0xFF334155),
    onBackground = PosSlate50,
    onSurface = PosSlate50
)

private val LightColorScheme = lightColorScheme(
    primary = PosIndigoDark,
    onPrimary = Color.White,
    primaryContainer = PosIndigoSubtle,
    onPrimaryContainer = PosIndigoDark,
    secondary = PosEmerald,
    onSecondary = Color.White,
    secondaryContainer = PosEmeraldContainer,
    onSecondaryContainer = PosEmeraldDark,
    tertiary = PosAmber,
    background = PosSlate50,
    surface = Color.White,
    surfaceVariant = PosSlate100,
    onBackground = PosSlate900,
    onSurface = PosSlate900
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use our crisp POS brand palette by default
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
