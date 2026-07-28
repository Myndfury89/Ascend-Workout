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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.sin
import kotlin.random.Random

/*
 * The deepest layers of the composition: a dark holographic gradient, a low haze, and a small
 * field of slow luminous particles. Particle count is capped and scaled by effects quality;
 * the drift animation is paused when the surface isn't running (off-screen / reduced motion).
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

private const val MAX_PARTICLES = 26

/** The particle count for a given effects quality (0..1), capped — shared with diagnostics. */
fun particleCountFor(effectsQuality: Float): Int = (MAX_PARTICLES * effectsQuality).toInt().coerceIn(0, MAX_PARTICLES)

/** The dark holographic ground + a subtle upper haze. Cheap, no animation. */
@Composable
fun StatusAtmosphere(
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawRect(
            brush =
                Brush.radialGradient(
                    colors = listOf(Color(0xFF0E1420), Color(0xFF080B12), Color(0xFF05070B)),
                    center = Offset(size.width * 0.5f, size.height * 0.28f),
                    radius = size.maxDimension * 0.8f,
                ),
        )
        drawRect(
            brush =
                Brush.verticalGradient(
                    colors = listOf(accent.copy(alpha = 0.06f), Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.4f,
                ),
        )
    }
}

/**
 * A capped field of slowly rising particles. [running] gates the infinite animation so it
 * pauses when the panel isn't visible; [effectsQuality] scales the particle count (0 hides
 * them entirely for the simplified-effects mode).
 */
@Composable
fun StatusParticleField(
    accent: Color,
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
                    radius = 1.2f + rnd.nextFloat() * 2.2f,
                    speed = 0.4f + rnd.nextFloat() * 0.6f,
                    drift = (rnd.nextFloat() - 0.5f) * 0.06f,
                    phase = rnd.nextFloat(),
                    alpha = 0.15f + rnd.nextFloat() * 0.35f,
                )
            }
        }

    val transition = rememberInfiniteTransition(label = "particles")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (running) 1f else 0f,
        animationSpec = infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart),
        label = "particle-time",
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val progress = (p.baseY - t * p.speed + p.phase)
            val y = ((progress % 1f) + 1f) % 1f
            val wobble = sin((t + p.phase) * 6.28f) * p.drift
            val cx = ((p.x + wobble) % 1f) * size.width
            val cy = (1f - y) * size.height
            drawCircle(accent.copy(alpha = p.alpha), radius = p.radius, center = Offset(cx, cy))
        }
    }
}
