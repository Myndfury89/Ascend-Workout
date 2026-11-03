package com.ascend.feature.dashboard.sigil

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import com.ascend.core.designsystem.motion.LocalMotionSpec
import com.ascend.core.designsystem.motion.MotionSpec
import com.ascend.core.designsystem.motion.MotionVerb
import com.ascend.core.designsystem.motion.verbTween
import com.ascend.core.designsystem.sigil.ClassKey
import com.ascend.core.designsystem.sigil.SigilState
import com.ascend.core.designsystem.sigil.drifts
import com.ascend.core.designsystem.sigil.entryFrame
import com.ascend.core.designsystem.sigil.settledFrame
import com.ascend.feature.dashboard.prototype.MedallionGlyph
import com.ascend.feature.dashboard.prototype.OrnateSigil
import com.ascend.feature.dashboard.prototype.OrnateSigilAnimation
import com.ascend.feature.dashboard.prototype.OrnateSigilState
import com.ascend.feature.dashboard.prototype.RankTier
import com.ascend.feature.dashboard.prototype.SigilOpacityProfile
import com.ascend.feature.dashboard.prototype.SigilRotationProfile
import com.ascend.feature.dashboard.prototype.statusVariant
import kotlinx.coroutines.launch

/** One full ambient revolution of the base layer; the class rotationFactor scales it. */
private const val AMBIENT_PERIOD_MS = 24_000
private const val FULL_TURN = 360f

/**
 * A class sigil driven by [state]. Built on [ClassSigilCatalog] data and the motion verbs; draws
 * through the existing [OrnateSigil] renderer with the production flags. Under reduced motion
 * ([MotionSpec.reducedMotion], which screens feed from the PR 2 reduced-motion controller) there is
 * no drift and every verb cross-fades straight to its settled frame.
 */
@Suppress("FunctionNaming") // Compose convention; existing composables sit in the detekt baseline.
@Composable
fun ClassSigil(
    classKey: ClassKey,
    state: SigilState,
    rank: RankTier,
    modifier: Modifier = Modifier,
    motion: MotionSpec = LocalMotionSpec.current,
) {
    val contentDescription = "${classKey.name.lowercase()} sigil, ${state.name.lowercase()}"
    val driver = rememberSigilDriver(state, motion)
    val hasClass = classKey != ClassKey.NEUTRAL
    val sigilState =
        OrnateSigilState(
            rankTier = rank,
            variant = classKey.statusVariant(),
            playerRing = 1f,
            classRing = if (hasClass) 1f else null,
            activeMedallion = -1,
            showProficiency = hasClass,
            newRankLayer = state.settledFrame().newRankLayer,
        )
    val animation =
        OrnateSigilAnimation(
            assembly = driver.assembly.value,
            rotation = driver.rotation(),
            glow = driver.glow.value,
            playerRingTrim = 1f,
            classRingTrim = 1f,
            medallionPulse = List(MedallionGlyph.attributeGlyphs.size) { 0f },
            proficiencyPulse = 0f,
            wave = driver.wave.value,
        )
    OrnateSigil(
        state = sigilState,
        animation = animation,
        semanticDescription = contentDescription,
        modifier =
            modifier
                .graphicsLayer { alpha = driver.alpha.value }
                .desaturate { driver.saturation.value },
        frameMotion = state.drifts(motion.reducedMotion),
        rotationProfile = SigilRotationProfile.CEREMONIAL_MIDDLE,
        opacityProfile = SigilOpacityProfile.RING_FORWARD,
        classGeometry = true,
        internalGlow = true,
        warmAccents = true,
    )
}

/** Animated scalars for one [SigilState]; [rotation] reads 0 whenever the state does not drift. */
private class SigilDriver(
    val assembly: Animatable<Float, *>,
    val glow: Animatable<Float, *>,
    val wave: Animatable<Float, *>,
    val alpha: Animatable<Float, *>,
    val saturation: Animatable<Float, *>,
    val rotation: () -> Float,
)

@Composable
private fun rememberSigilDriver(
    state: SigilState,
    motion: MotionSpec,
): SigilDriver {
    val reduced = motion.reducedMotion
    val settled = state.settledFrame()
    val assembly = remember { Animatable(settled.assembly) }
    val glow = remember { Animatable(settled.glow) }
    val wave = remember { Animatable(settled.wave) }
    val alpha = remember { Animatable(settled.alpha) }
    val saturation = remember { Animatable(settled.saturation) }

    LaunchedEffect(state, reduced) {
        val verb = state.verb
        if (verb == null || reduced) {
            // Plain changes and every reduced-motion change cross-fade straight to the end frame.
            val spec = motion.verbTween<Float>(verb ?: MotionVerb.LOCK)
            launch { assembly.animateTo(settled.assembly, spec) }
            launch { glow.animateTo(settled.glow, spec) }
            launch { wave.animateTo(settled.wave, spec) }
            launch { alpha.animateTo(settled.alpha, spec) }
            launch { saturation.animateTo(settled.saturation, spec) }
        } else {
            val start = state.entryFrame()
            assembly.snapTo(start.assembly)
            glow.snapTo(start.glow)
            alpha.snapTo(settled.alpha)
            saturation.snapTo(settled.saturation)
            launch { assembly.animateTo(settled.assembly, motion.verbTween(verb)) }
            launch { glow.animateTo(settled.glow, motion.verbTween(verb)) }
            launch {
                if (state == SigilState.LevelUp) {
                    wave.snapTo(0f)
                    wave.animateTo(1f, motion.verbTween(verb))
                }
                wave.snapTo(settled.wave)
            }
        }
    }

    val drifting = state.drifts(reduced)
    val infinite = rememberInfiniteTransition(label = "classSigilDrift")
    val drift =
        infinite.animateFloat(
            initialValue = 0f,
            targetValue = if (drifting) FULL_TURN else 0f,
            animationSpec = infiniteRepeatable(tween(AMBIENT_PERIOD_MS, easing = LinearEasing), RepeatMode.Restart),
            label = "drift",
        )
    return SigilDriver(assembly, glow, wave, alpha, saturation) { if (drifting) drift.value else 0f }
}

/** Draws content through a saturation colour matrix (1 = unchanged, 0 = greyscale). */
private fun Modifier.desaturate(saturation: () -> Float): Modifier =
    drawWithContent {
        val s = saturation()
        if (s >= 1f) {
            drawContent()
        } else {
            val matrix = ColorMatrix().apply { setToSaturation(s) }
            val paint = Paint().apply { colorFilter = ColorFilter.colorMatrix(matrix) }
            drawIntoCanvas { canvas ->
                canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
                drawContent()
                canvas.restore()
            }
        }
    }
