package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ClonvoDarkColorScheme = darkColorScheme(
    primary = ElectricPurpleBright,
    onPrimary = Color.White,
    primaryContainer = ElectricPurpleContainer,
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = NeonCyanBright,
    onSecondary = StudioObsidian,
    secondaryContainer = NeonCyanContainer,
    onSecondaryContainer = Color(0xFFCFFAFE),
    tertiary = EmeraldSuccess,
    onTertiary = StudioObsidian,
    background = StudioObsidian,
    onBackground = Color(0xFFF8FAFC),
    surface = StudioNavyDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = StudioNavySurface,
    onSurfaceVariant = Color(0xFFCBD5E1),
    error = CoralError,
    onError = Color.White
)

private val ClonvoLightColorScheme = lightColorScheme(
    primary = ElectricPurple,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF2E1065),
    secondary = NeonCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF083344),
    tertiary = EmeraldSuccess,
    onTertiary = Color.White,
    background = LightStudioBackground,
    onBackground = DeepSlateText,
    surface = LightStudioSurface,
    onSurface = DeepSlateText,
    surfaceVariant = LightStudioSurfaceVariant,
    onSurfaceVariant = Color(0xFF475569),
    error = CoralError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) ClonvoDarkColorScheme else ClonvoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
