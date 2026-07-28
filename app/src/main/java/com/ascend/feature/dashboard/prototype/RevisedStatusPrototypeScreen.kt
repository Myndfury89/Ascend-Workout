package com.ascend.feature.dashboard.prototype

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.core.designsystem.motion.MotionSpec

private const val ATTRIBUTE_COUNT = 5

/**
 * The revised, fake-data Status motion prototype: one dominant edge-lit panel over a dark
 * holographic field, a centred class sigil, a clean top-to-bottom hierarchy, and a debug
 * selector for all 14 states, both motion modes, and an effects-quality toggle. **Nothing here
 * is wired to the ProgressionEventQueue, repositories, or real reward logic** — every value and
 * transition is simulated for visual review.
 */
@Composable
fun RevisedStatusPrototypeScreen(
    modifier: Modifier = Modifier,
    initialState: StatusPrototypeStateId = StatusPrototypeStateId.STANDARD,
    initialVariant: StatusClassVariant = StatusClassVariant.BERSERKER,
) {
    var stateId by remember { mutableStateOf(initialState) }
    var variant by remember { mutableStateOf(initialVariant) }
    var reducedMotionToggle by remember { mutableStateOf(false) }
    var simplifiedEffects by remember { mutableStateOf(false) }
    var replayKey by remember { mutableIntStateOf(0) }

    val data = FakeStatusPrototype.dataFor(stateId, variant, reducedMotionToggle)
    val motionSpec = MotionSpec(reducedMotion = data.reducedMotion)
    val effectsQuality = if (simplifiedEffects) 0.35f else 1f
    val sigil = StatusSigilVariant.of(variant)
    val accent = sigil.core

    val motion = remember { StatusPrototypeMotion(ATTRIBUTE_COUNT) }
    LaunchedEffect(stateId, variant, replayKey, data.reducedMotion) {
        motion.play(data, motionSpec)
    }

    // Ambient infinite effects — held still under reduced motion so nothing loops.
    val infinite = rememberInfiniteTransition(label = "ambient")
    val running = !data.reducedMotion
    val rotation by infinite.animateFloat(
        0f,
        if (running) 360f else 0f,
        infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot",
    )
    val glowPulse by infinite.animateFloat(
        if (running) 0.55f else 1f,
        1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val scan by infinite.animateFloat(
        0f,
        if (running) 1f else 0f,
        infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        label = "scan",
    )

    Box(modifier.fillMaxSize().background(Color(0xFF05070B))) {
        StatusAtmosphere(accent)
        StatusParticleField(accent, running = running, effectsQuality = effectsQuality)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            Box {
                EdgeLitStatusPanel(
                    accent = accent,
                    materialize = motion.panelMaterialize.value,
                    scan = scan,
                    effectsQuality = effectsQuality,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PanelContent(data, accent, sigil, motion, rotation, glowPulse)
                }
                Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
                    EventOverlayChip(data.overlay, accent, motion.overlayReveal.value, motion.eventFlash.value)
                }
            }

            Spacer(Modifier.height(20.dp))
            StatusPrototypeControls(
                stateId = stateId,
                variant = variant,
                reducedMotion = reducedMotionToggle,
                simplifiedEffects = simplifiedEffects,
                onState = { stateId = it },
                onVariant = { variant = it },
                onReplay = { replayKey++ },
                onReducedMotion = { reducedMotionToggle = it },
                onSimplifiedEffects = { simplifiedEffects = it },
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

@Composable
private fun PanelContent(
    data: StatusPrototypeData,
    accent: Color,
    sigil: StatusSigilVariant,
    motion: StatusPrototypeMotion,
    rotation: Float,
    glowPulse: Float,
) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        IdentityBlock(data, accent, motion.levelReveal.value)
        Spacer(Modifier.height(16.dp))
        SigilBlock(data, sigil, motion, rotation, glowPulse)
        Spacer(Modifier.height(20.dp))
        ProgressionBars(data, accent, motion.xpFill.value, motion.classXpFill.value, motion.secondaryXpFill.value)
        Spacer(Modifier.height(22.dp))
        AttributeMeters(
            data,
            reveals = motion.attrReveal.map { it.value },
            values = motion.attrValue.map { it.value },
            pulses = motion.attrPulse.map { it.value },
        )
        Spacer(Modifier.height(6.dp))
        ProficiencyBlock(data, accent, motion.proficiencyReveal.value, motion.proficiencyPulse.value)
        Spacer(Modifier.height(20.dp))
        ProgressionInfo(data, accent, motion.proficiencyReveal.value)
    }
}

/** The centred, assembling class sigil — hierarchy tier 2, just under identity. */
@Composable
private fun SigilBlock(
    data: StatusPrototypeData,
    sigil: StatusSigilVariant,
    motion: StatusPrototypeMotion,
    rotation: Float,
    glowPulse: Float,
) {
    val state =
        StatusSigilState(
            variant = sigil,
            tier = data.trainingTier,
            assembly = motion.sigilAssembly.value,
            rotationDegrees = rotation,
            glow = 0.55f + 0.25f * motion.eventFlash.value,
            progressionActive = data.pendingRecommendation != null || data.recentPersonalRecord != null,
            majorUnlock = data.majorUnlock,
        )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            StatusSigil(
                state = state,
                animation = StatusSigilAnimation(motion.sigilAssembly.value, rotation, glowPulse),
                effectsQuality = 1f,
                modifier =
                    Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1f)
                        .graphicsLayer { alpha = 0.4f + 0.6f * motion.sigilAssembly.value },
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "TIER ${data.trainingTier} · ${data.variant.classTitle.uppercase()}",
                color = sigil.core.copy(alpha = 0.8f),
                fontSize = 10.sp,
                letterSpacing = 2.sp,
            )
        }
    }
}
