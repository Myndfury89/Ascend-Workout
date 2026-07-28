package com.ascend.core.designsystem.motion

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Ascend's original motion language. Timings and curves are deliberately distinct
 * from any franchise's — a calm, weighty "system coming online" feel: quick reads,
 * a firm settle, and a single dramatic beat reserved for ascension. All durations
 * are in milliseconds and flow through [MotionSpec.duration] so reduced‑motion can
 * collapse them in one place.
 */
object AscendMotionTokens {
    // Durations (ms). Named by intent, not by number, so tuning never touches call sites.
    const val INSTANT = 0
    const val QUICK = 130
    const val STANDARD = 260
    const val DELIBERATE = 440
    const val DRAMATIC = 760

    // Stagger between sequential elements (attribute rows, entrance stages).
    const val STAGGER = 70

    // A short hold at the top of an emphasis beat (pulse peak, level‑up flash).
    const val HOLD = 180
}

/**
 * Original easing set. [settle] decelerates hard into place (entrance, value
 * counts); [emphasize] overshoots gently for celebratory reveals; [surge]
 * accelerates out (things leaving); [pulse] is symmetric for a heartbeat swell.
 */
object AscendEasing {
    val settle: Easing = CubicBezierEasing(0.16f, 0.84f, 0.24f, 1f)
    val emphasize: Easing = CubicBezierEasing(0.18f, 1.12f, 0.30f, 1f)
    val surge: Easing = CubicBezierEasing(0.45f, 0f, 0.85f, 0.35f)
    val pulse: Easing = CubicBezierEasing(0.36f, 0f, 0.30f, 1f)
    val linear: Easing = LinearEasing
}

/**
 * The resolved motion configuration for a subtree. When [reducedMotion] is set,
 * every derived duration and stagger collapses to zero, so animations become
 * instant cuts while the exact same code path runs — no branching at call sites.
 *
 * [speedScale] is a single multiplier over every token duration (and stagger), so a whole
 * speed profile (e.g. a faster "everyday" feel vs. a slower "cinematic" one) is expressed
 * without duplicating any timing constant. It defaults to 1.0, leaving the base language
 * unchanged, and is orthogonal to [reducedMotion] — reduced motion always wins and returns 0.
 */
data class MotionSpec(
    val reducedMotion: Boolean = false,
    val speedScale: Float = 1f,
) {
    /** Scale a token duration by [speedScale]; 0 under reduced motion. */
    fun duration(tokenMs: Int): Int = if (reducedMotion) 0 else (tokenMs * speedScale).toInt()

    /** Stagger delay for [index], scaled by [speedScale]; 0 under reduced motion. */
    fun stagger(
        index: Int,
        stepMs: Int = AscendMotionTokens.STAGGER,
    ): Int = if (reducedMotion) 0 else (index * stepMs * speedScale).toInt()

    /** A tween built from a token + easing, respecting reduced motion. */
    fun <T> tween(
        tokenMs: Int,
        easing: Easing = AscendEasing.settle,
        delayMs: Int = 0,
    ): FiniteAnimationSpec<T> =
        tween(
            durationMillis = duration(tokenMs),
            delayMillis = duration(delayMs),
            easing = easing,
        )

    companion object {
        val Full = MotionSpec(reducedMotion = false)
        val Reduced = MotionSpec(reducedMotion = true)
    }
}

/** Ambient motion configuration. Screens provide it from a user/system preference. */
val LocalMotionSpec = staticCompositionLocalOf { MotionSpec.Full }

@Composable
fun rememberMotionSpec(reducedMotion: Boolean): MotionSpec = MotionSpec(reducedMotion = reducedMotion)
