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

/**
 * The four ceremonial **motion verbs** (design handoff v1). Distinct from the everyday tokens above
 * and additive — nothing here replaces or deletes them. Every ceremonial/progression animation in the
 * sigil system should read as exactly one of these. Durations in ms.
 *
 * - **Assemble** — geometry/panels/sigils form from fragments or traced lines.
 * - **Charge** — energy gathers before an important event. *Least-validated token*: re-check feel at
 *   real particle counts / on-device frame rate.
 * - **Lock** — a value/result snaps into its final precise state.
 * - **Ascend** — rank/identity elements permanently gain complexity (rare, cinematic).
 *
 * Under reduced motion every verb collapses to a single [AscendVerb.REDUCED_MS] linear cross-fade
 * (see [MotionSpec.verbTween]); Reward-tier events additionally emit a brief [AscendVerb.RECOGNITION_CUE_MS]
 * non-kinetic cue so a silent personal record never reads as a bug.
 */
object AscendVerb {
    const val ASSEMBLE_MS = 850
    const val CHARGE_MS = 1100
    const val LOCK_MS = 380
    const val ASCEND_MS = 2200

    /** Reduced-motion collapse for every verb: a short linear cross-fade, never a hard 0 cut. */
    const val REDUCED_MS = 140

    /** Reduced-motion Reward-tier recognition cue (a flat colour pulse) — the silent-PR guard. */
    const val RECOGNITION_CUE_MS = 320
}

/** Original easings for the verbs (kept separate from [AscendEasing] so neither set drifts). */
object AscendVerbEasing {
    val assemble: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f) // expo decelerate
    val charge: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f) // symmetric ease in-out
    val lock: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f) // snap overshoot
    val ascend: Easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f) // slow decelerate
}

/** A verb bundles its default duration + easing so call sites name intent, never numbers. */
enum class MotionVerb(val durationMs: Int, val easing: Easing) {
    ASSEMBLE(AscendVerb.ASSEMBLE_MS, AscendVerbEasing.assemble),
    CHARGE(AscendVerb.CHARGE_MS, AscendVerbEasing.charge),
    LOCK(AscendVerb.LOCK_MS, AscendVerbEasing.lock),
    ASCEND(AscendVerb.ASCEND_MS, AscendVerbEasing.ascend),
}

/**
 * The three motion tiers and their duration windows. [referenceVerb] is the verb that best
 * characterises the tier; the window is a design guide, not a hard clamp.
 */
enum class MotionTier(val minMs: Int, val maxMs: Int, val referenceVerb: MotionVerb) {
    EVERYDAY(200, 700, MotionVerb.LOCK),
    REWARD(700, 1500, MotionVerb.CHARGE),
    ASCENSION(1500, 3000, MotionVerb.ASCEND),
}

/**
 * Verb-review speed profile from the handoff (Fast ×0.55, Standard ×1, Cinematic ×1.6). Applied as
 * [MotionSpec.speedScale]; kept distinct from the prototype's own everyday speed control.
 */
enum class VerbSpeed(val scale: Float) {
    FAST(0.55f),
    STANDARD(1f),
    CINEMATIC(1.6f),
}

/**
 * A verb-aware tween. Unlike [MotionSpec.duration] (which collapses to 0), reduced motion here
 * collapses to a [AscendVerb.REDUCED_MS] **linear cross-fade** — the ceremonial system must still
 * cross-fade, never hard-cut. Otherwise the verb's own duration is scaled by [MotionSpec.speedScale].
 */
fun <T> MotionSpec.verbTween(
    verb: MotionVerb,
    delayMs: Int = 0,
): FiniteAnimationSpec<T> =
    if (reducedMotion) {
        tween(durationMillis = AscendVerb.REDUCED_MS, easing = AscendEasing.linear)
    } else {
        tween(
            durationMillis = (verb.durationMs * speedScale).toInt().coerceAtLeast(1),
            delayMillis = (delayMs * speedScale).toInt(),
            easing = verb.easing,
        )
    }

/** Ambient motion configuration. Screens provide it from a user/system preference. */
val LocalMotionSpec = staticCompositionLocalOf { MotionSpec.Full }

@Composable
fun rememberMotionSpec(reducedMotion: Boolean): MotionSpec = MotionSpec(reducedMotion = reducedMotion)
