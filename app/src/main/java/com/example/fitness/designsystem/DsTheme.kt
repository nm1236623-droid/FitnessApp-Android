package com.example.fitness.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Thin wrapper to provide extra DS tokens while still relying on MaterialTheme.
 * Use this inside FitnessTheme so the whole app gets premium tokens.
 */
@Composable
fun DsProvideTokens(
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    // Keep tokens simple and stable; use Material colors as a base.
    val scheme = MaterialTheme.colorScheme

    val brand = if (darkTheme) {
        DsBrandColors(
            accent = Color(0xFFFF6A00),     // neon orange
            accent2 = Color(0xFFFFB000),    // warm amber for secondary accent
            success = Color(0xFF22C55E),
            warning = Color(0xFFFFB000),
            danger = Color(0xFFF87171),
        )
    } else {
        DsBrandColors(
            accent = Color(0xFFFF6A00),
            accent2 = Color(0xFFFF8A33),
            success = Color(0xFF16A34A),
            warning = Color(0xFFFFB000),
            danger = Color(0xFFEF4444),
        )
    }

    val gradients = DsGradients(
        hero = Brush.linearGradient(
            listOf(
                brand.accent,
                scheme.primary,
                brand.accent2,
            )
        ),
        card = Brush.linearGradient(
            listOf(
                scheme.surfaceVariant,
                scheme.primary,
            )
        )
    )

    CompositionLocalProvider(
        LocalDsBrandColors provides brand,
        LocalDsGradients provides gradients,
        content = content
    )
}
