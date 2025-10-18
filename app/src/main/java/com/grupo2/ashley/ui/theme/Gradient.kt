package com.grupo2.ashley.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Gradientes predefinidos
 * Todos los gradientes son diagonales (topStart → bottomEnd)
 */
object AppGradients {

    // ========== Gradientes Primarios ==========

    /** Gradiente principal púrpura (para botones principales) */
    val PrimaryGradient = Brush.linearGradient(
        colors = listOf(Purple60, Purple50),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /** Gradiente primario claro (para modo oscuro) */
    val PrimaryGradientDark = Brush.linearGradient(
        colors = listOf(Purple70, Purple60),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // ========== Gradientes Secundarios ==========

    /** Gradiente secundario turquesa/cyan */
    val SecondaryGradient = Brush.linearGradient(
        colors = listOf(Cyan60, Cyan50),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /** Gradiente secundario para modo oscuro */
    val SecondaryGradientDark = Brush.linearGradient(
        colors = listOf(Cyan70, Cyan60),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // ========== Gradientes Mixtos ==========

    /** Gradiente púrpura-turquesa (para elementos destacados) */
    val AccentGradient = Brush.linearGradient(
        colors = listOf(Purple60, Cyan60),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /** Gradiente suave para cards (tinte púrpura) */
    val CardGradientLight = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFFFBFF), Color(0xFFF8F7FF)
        ), start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /** Gradiente suave para cards modo oscuro */
    val CardGradientDark = Brush.linearGradient(
        colors = listOf(
            Color(0xFF2A2A2E), Color(0xFF1E1E1E)
        ), start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // ========== Gradientes de Fondo ==========

    /** Gradiente de fondo sutil para pantallas completas (modo claro) */
    val BackgroundGradientLight = Brush.linearGradient(
        colors = listOf(
            Color(0xFFFAF9FF), Color(0xFFF0EFFF)
        ), start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /** Gradiente de fondo para modo oscuro */
    val BackgroundGradientDark = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1A1625), Color(0xFF121212)
        ), start = Offset(0f, 0f), end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    // ========== Gradientes de Estado ==========

    /** Gradiente para indicadores de éxito */
    val SuccessGradient = Brush.linearGradient(
        colors = listOf(Color(0xFF4CAF50), Color(0xFF2E7D32)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /** Gradiente para indicadores de error */
    val ErrorGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFF44336), Color(0xFFB3261E)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )

    /** Gradiente para indicadores de advertencia */
    val WarningGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFF9800), Color(0xFFF57C00)),
        start = Offset(0f, 0f),
        end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    )
}
