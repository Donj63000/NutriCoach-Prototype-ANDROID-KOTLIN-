// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.designsystem

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary = LeafGreen,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = LeafContainer,
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFF082019),
    secondary = WarmOrange,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    secondaryContainer = WarmContainer,
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF2D1600),
    background = BackgroundLight,
    surface = SurfaceLight
)

private val DarkColorScheme = darkColorScheme(
    primary = LeafGreenDark,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF00382B),
    primaryContainer = LeafContainerDark,
    onPrimaryContainer = androidx.compose.ui.graphics.Color(0xFFC2EAD9),
    secondary = WarmOrangeDark,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF4A2800),
    secondaryContainer = WarmContainerDark,
    onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFFFDDBB),
    background = BackgroundDark,
    surface = SurfaceDark
)

@Composable
fun NutriCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = NutriCoachTypography,
        content = content
    )
}
