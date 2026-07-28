package com.ascend.feature.dashboard.prototype

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

/*
 * The deepest layers of the composition — the sense of being suspended in empty space: a
 * near-black deep-navy ground with a dim radial illumination behind the panel, a soft vignette
 * pulling attention inward, an occasional slow horizontal light sweep, drifting fog, and a small
 * field of soft white-blue particles. Everything is capped and quality-scaled; the drift
 * animations pause when not running (off-screen / reduced motion / minimal effects).
 */

private data class Particle(
    val x: Float,
    val baseY: Float,
    val radius: Float,
    val speed: Float,
    val drift: Float,
    val phase: Float,
    val alpha: Float,
)

private data class FogBlob(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float,
    val phase: Float,
    val tint: Color,
)

private const val MAX_PARTICLES = 24

/** The particle count for a given effects quality (0..1), capped — shared with diagnostics. */
fun particleCountFor(effectsQuality: Float): Int = (MAX_PARTICLES * effectsQuality).toInt().coerceIn(0, MAX_PARTICLES)

/**
 * The static ground: a deep blue-violet radial that is brighter at centre, plus a soft vignette.
 * [sweep] (0..1) positions an occasional faint horizontal light band; hold it constant to pause.
 */
@Composable
fun StatusAtmosphere(
    sweep: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        // Deep-navy ground, brighter toward the centre-behind-panel.
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(StatusPalette.groundTint, StatusPalette.groundNavy, StatusPalette.groundDeep),
                    center = Offset(size.width * 0.5f, size.height * 0.34f),
                    radius = size.maxDimension * 0.72f,
                ),
        )
        // Dim violet illumination directly behind the panel.
        drawCircle(
            brush =
                Brush.radialGradient(
                    colors = listOf(StatusPalette.violet.copy(alpha = 0.10f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.32f),
                    radius = size.width * 0.7f,
                ),
            radius = size.width * 0.7f,
            center = Offset(size.width * 0.5f, size.height * 0.32f),
        )
        // Occasional horizontal light band drifting across (a soft vertical column moving in x).
        val bandX = sweep * size.width
        drawRect(
            brush =
                Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, StatusPalette.cyan.copy(alpha = 0.05f), Color.Transparent),
                    startX = bandX - 120f,
                    endX = bandX + 120f,
                ),
            topLeft = Offset(bandX - 120f, 0f),
            size = Size(240f, size.height),
        )
        // Soft vignette so attention stays on the interface.
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color.Transparent, Color.Transparent, StatusPalette.groundDeep.copy(alpha = 0.85f)),
                    center = Offset(size.width * 0.5f, size.height * 0.42f),
                    radius = size.maxDimension * 0.62f,
                ),
        )
    }
}

/** A few large, very faint drifting fog blobs (violet / cyan). Paused when not [running]. */
@Composable
fun StatusFog(
    running: Boolean,
    modifier: Modifier = Modifier,
    effectsQuality: Float = 1f,
) {
    if (effectsQuality <= 0f) return
    val blobs =
        remember {
            val rnd = Random(7)
            List(4) {
                FogBlob(
                    x = rnd.nextFloat(),
                    y = 0.2f + rnd.nextFloat() * 0.6f,
                    radius = 0.4f + rnd.nextFloat() * 0.35f,
                    speed = 0.2f + rnd.nextFloat() * 0.3f,
                    phase = rnd.nextFloat(),
                    tint = if (it % 2 == 0) StatusPalette.violet else StatusPalette.cyan,
                )
            }
        }
    val transition = rememberInfiniteTransition(label = "fog")
    val t by transition.animateFloat(
        0f,
        if (running) 1f else 0f,
        infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart),
        label = "fog-t",
    )
    Canvas(modifier = modifier.fillMaxSize()) {
        blobs.forEach { b ->
            val cx = (((b.x + t * b.speed) % 1f) + sin((t + b.phase) * 6.28f) * 0.03f) * size.width
            val cy = b.y * size.height
            val r = b.radius * size.minDimension
            drawCircle(
                brush = Brush.radialGradient(listOf(b.tint.copy(alpha = 0.05f * effectsQuality), Color.Transparent), Offset(cx, cy), r),
                radius = r,
                center = Offset(cx, cy),
            )
        }
    }
}

/**
 * A capped field of slowly rising, soft white-blue particles. [running] gates the animation;
 * [effectsQuality] scales the count (0 hides them for the minimal-effects mode).
 */
@Composable
fun StatusParticleField(
    running: Boolean,
    modifier: Modifier = Modifier,
    effectsQuality: Float = 1f,
) {
    if (effectsQuality <= 0f) return
    val count = particleCountFor(effectsQuality)
    if (count == 0) return

    val particles =
        remember(count) {
            val rnd = Random(42)
            List(count) {
                Particle(
                    x = rnd.nextFloat(),
                    baseY = rnd.nextFloat(),
                    radius = 1.0f + rnd.nextFloat() * 1.8f,
                    speed = 0.3f + rnd.nextFloat() * 0.5f,
                    drift = (rnd.nextFloat() - 0.5f) * 0.05f,
                    phase = rnd.nextFloat(),
                    alpha = 0.12f + rnd.nextFloat() * 0.3f,
                )
            }
        }

    val transition = rememberInfiniteTransition(label = "particles")
    val t by transition.animateFloat(
        0f,
        if (running) 1f else 0f,
        infiniteRepeatable(tween(16_000, easing = LinearEasing), RepeatMode.Restart),
        label = "particle-time",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val progress = (p.baseY - t * p.speed + p.phase)
            val y = ((progress % 1f) + 1f) % 1f
            val wobble = sin((t + p.phase) * 6.28f) * p.drift
            val cx = ((p.x + wobble) % 1f) * size.width
            val cy = (1f - y) * size.height
            val tint = if (p.phase > 0.6f) StatusPalette.cyanSoft else StatusPalette.infoLine
            drawCircle(tint.copy(alpha = p.alpha), radius = p.radius, center = Offset(cx, cy))
        }
    }
}
