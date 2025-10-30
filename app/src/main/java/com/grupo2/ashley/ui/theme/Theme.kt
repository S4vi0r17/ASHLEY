package com.grupo2.ashley.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// ========== Esquema de Colores Modo Claro ==========
private val LightColorScheme = lightColorScheme(
    primary = Purple60,
    onPrimary = White,
    primaryContainer = Purple90,
    onPrimaryContainer = Purple50,

    secondary = Cyan60,
    onSecondary = White,
    secondaryContainer = Cyan90,
    onSecondaryContainer = Cyan50,

    tertiary = Purple70,
    onTertiary = White,
    tertiaryContainer = Purple90,
    onTertiaryContainer = Purple50,

    error = ErrorLight,
    onError = White,
    errorContainer = Color(0xFFFFF0EE),
    onErrorContainer = ErrorLight,

    background = LightBackground,
    onBackground = TextPrimaryLight,

    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,

    outline = GrayMedium,
    outlineVariant = GrayLight,

    scrim = ScrimDark,

    inverseSurface = DarkSurface,
    inverseOnSurface = TextPrimaryDark,
    inversePrimary = Purple70,

    surfaceTint = Purple60
)

// ========== Esquema de Colores Modo Oscuro ==========
private val DarkColorScheme = darkColorScheme(
    primary = Purple60,
    onPrimary = Color.White,
    primaryContainer = Purple50,
    onPrimaryContainer = Purple90,

    secondary = Cyan60,
    onSecondary = Color.Black,
    secondaryContainer = Cyan50,
    onSecondaryContainer = Cyan90,

    tertiary = Purple70,
    onTertiary = Color.White,
    tertiaryContainer = Purple50,
    onTertiaryContainer = Purple90,

    error = ErrorDark,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = ErrorDark,

    background = DarkBackground,
    onBackground = TextPrimaryDark,

    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,

    outline = GrayMedium,
    outlineVariant = GrayDark,

    scrim = ScrimDark,

    inverseSurface = LightSurface,
    inverseOnSurface = TextPrimaryLight,
    inversePrimary = Purple70,

    surfaceTint = Purple60
)

// ========== Formas/Shapes Redondeadas ==========
private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun ASHLEYTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme, typography = Typography, shapes = AppShapes, content = content
    )
}
