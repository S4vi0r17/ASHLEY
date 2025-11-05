package com.grupo2.ashley.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally

object AnimationConstants {

    // ========== Duraciones ==========
    const val FLUID_DURATION = 400
    const val QUICK_DURATION = 200
    const val SLOW_DURATION = 600

    // ========== Easing Functions ==========
    val FluidEasing = FastOutSlowInEasing
    val QuickEasing = FastOutLinearInEasing
    val SlowEasing = LinearOutSlowInEasing

    // ========== Tween Specs ==========
    fun <T> fluidTween() = tween<T>(
        durationMillis = FLUID_DURATION, easing = FluidEasing
    )

    fun <T> quickTween() = tween<T>(
        durationMillis = QUICK_DURATION, easing = QuickEasing
    )

    fun <T> slowTween() = tween<T>(
        durationMillis = SLOW_DURATION, easing = SlowEasing
    )

    // ========== Spring Specs ==========
    val FluidSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow
    )

    val QuickSpring = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium
    )

    // ========== Navigation Transitions ==========

    /** Fade In para navegación */
    val NavigationFadeIn = fadeIn(
        animationSpec = tween(
            durationMillis = FLUID_DURATION, easing = FluidEasing
        )
    )

    /** Fade Out para navegación */
    val NavigationFadeOut = fadeOut(
        animationSpec = tween(
            durationMillis = FLUID_DURATION, easing = FluidEasing
        )
    )

    /** Slide In desde la derecha */
    val NavigationSlideIn = slideInHorizontally(
        animationSpec = tween(
            durationMillis = FLUID_DURATION, easing = FluidEasing
        ), initialOffsetX = { fullWidth -> fullWidth })

    /** Slide Out hacia la izquierda */
    val NavigationSlideOut = slideOutHorizontally(
        animationSpec = tween(
            durationMillis = FLUID_DURATION, easing = FluidEasing
        ), targetOffsetX = { fullWidth -> -fullWidth })

    // ========== Delayed Animations ==========

    /** Delay corto para animaciones secuenciales */
    const val SHORT_DELAY = 50

    /** Delay medio para animaciones en cascada */
    const val MEDIUM_DELAY = 100

    /** Delay largo para efectos dramáticos */
    const val LONG_DELAY = 200
}
