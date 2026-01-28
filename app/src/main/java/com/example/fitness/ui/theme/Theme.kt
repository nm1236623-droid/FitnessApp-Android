package com.example.fitness.ui.theme

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
import com.example.fitness.designsystem.DsProvideTokens

// Enhanced color palette for professional app design
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6366F1),        // Indigo - modern & professional
    secondary = Color(0xFF8B5CF6),      // Purple - energetic
    tertiary = Color(0xFF06B6D4),       // Cyan - fresh
    background = Color(0xFF0F172A),     // Slate 900 - deep & elegant
    surface = Color(0xFF1E293B),        // Slate 800 - layered depth
    surfaceVariant = Color(0xFF334155), // Slate 700
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFF1F5F9),   // Slate 100
    onSurface = Color(0xFFF1F5F9),
    primaryContainer = Color(0xFF312E81), // Indigo 900
    onPrimaryContainer = Color(0xFFE0E7FF), // Indigo 100
    secondaryContainer = Color(0xFF581C87), // Purple 900
    onSecondaryContainer = Color(0xFFF3E8FF), // Purple 100
    outline = Color(0xFF475569),        // Slate 600
    error = Color(0xFFEF4444),          // Red 500
    errorContainer = Color(0xFF7F1D1D), // Red 900
    onErrorContainer = Color(0xFFFEE2E2) // Red 100
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF6366F1),        // Indigo
    secondary = Color(0xFF8B5CF6),      // Purple
    tertiary = Color(0xFF06B6D4),       // Cyan
    background = Color(0xFFF8FAFC),     // Slate 50
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9), // Slate 100
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),   // Slate 900
    onSurface = Color(0xFF1E293B),      // Slate 800
    primaryContainer = Color(0xFFE0E7FF), // Indigo 100
    onPrimaryContainer = Color(0xFF312E81), // Indigo 900
    secondaryContainer = Color(0xFFF3E8FF), // Purple 100
    onSecondaryContainer = Color(0xFF581C87), // Purple 900
    outline = Color(0xFFCBD5E1),        // Slate 300
    error = Color(0xFFEF4444),          // Red 500
    errorContainer = Color(0xFFFEE2E2), // Red 100
    onErrorContainer = Color(0xFF7F1D1D) // Red 900
)

@Composable
fun FitnessTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
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
        shapes = com.example.fitness.ui.theme.Shapes
    ) {
        DsProvideTokens(darkTheme = darkTheme) {
            content()
        }
    }
}