package com.example.fitness.designsystem

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Immutable
data class DsBrandColors(
    val accent: Color,
    val accent2: Color,
    val success: Color,
    val warning: Color,
    val danger: Color,
)

@Immutable
data class DsGradients(
    val hero: Brush,
    val card: Brush,
)

val LocalDsBrandColors = staticCompositionLocalOf {
    DsBrandColors(
        accent = Color(0xFF7C4DFF),
        accent2 = Color(0xFF00D4FF),
        success = Color(0xFF22C55E),
        warning = Color(0xFFFFB020),
        danger = Color(0xFFEF4444),
    )
}

val LocalDsGradients = staticCompositionLocalOf {
    DsGradients(
        hero = Brush.linearGradient(
            colors = listOf(Color(0xFF5B21B6), Color(0xFF2563EB), Color(0xFF06B6D4))
        ),
        card = Brush.linearGradient(
            colors = listOf(Color(0xFF1D4ED8), Color(0xFF7C3AED))
        ),
    )
}



