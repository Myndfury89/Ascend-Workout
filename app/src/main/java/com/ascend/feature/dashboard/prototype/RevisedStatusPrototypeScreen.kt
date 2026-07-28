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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.core.designsystem.motion.AscendMotionTokens
import com.ascend.core.designsystem.motion.MotionSpec
import kotlinx.coroutines.delay

private const val ATTRIBUTE_COUNT = 5

/**
 * The revised, fake-data Status motion prototype plus its debug review tooling. One dominant
 * edge-lit panel over a dark holographic field, a centred class sigil, and a clean hierarchy —
 * with entrance modes (everyday vs major event), a speed selector, effects-quality modes, a
 * looped-review state machine, a hierarchy overlay, and a diagnostics overlay. **Nothing here is
 * wired to the ProgressionEventQueue, repositories, or real reward logic** — all simulated.
 */
@Composable
fun RevisedStatusPrototypeScreen(
    modifier: Modifier = Modifier,
    controller: PrototypeReviewController = rememberPrototypeReviewController(),
) {
    val data =
        if (controller.textStress) {
            FakeStatusPrototype.stressData(controller.variant, controller.reducedMotion)
        } else {
            FakeStatusPrototype.dataFor(controller.stateId, controller.variant, controller.reducedMotion, controller.forceSecondary)
        }

    val reduced = controller.reducedMotion || data.reducedMotion
    val motionSpec = MotionSpec(reducedMotion = reduced, speedScale = controller.motionSpeed.scale)
    val effects = controller.effectsQuality.toConfig()
    val sigil = StatusSigilVariant.of(controller.variant)
    val accent = sigil.core
    val measurer = rememberHierarchyLabeler()

    val motion = remember { StatusPrototypeMotion(ATTRIBUTE_COUNT) }
    LaunchedEffect(
        controller.stateId,
        controller.variant,
        controller.replayKey,
        reduced,
        controller.textStress,
        controller.entranceMode,
        controller.forceSecondary,
    ) {
        motion.play(data, motionSpec, controller.entranceMode)
    }

    // Looped review — advances only while running and not paused; never auto-starts.
    LaunchedEffect(controller.loopActive, controller.loopIntervalMs) {
        while (controller.loopActive) {
            delay(controller.loopIntervalMs.toLong())
            if (controller.loopActive) controller.nextState()
        }
    }

    val rotationRunning = !reduced && effects.sigilIdle
    val scanRunning = !reduced && effects.scan
    val particlesRunning = !reduced && effects.particleQuality > 0f
    val glowActive = !reduced && effects.glowPulses

    val infinite = rememberInfiniteTransition(label = "ambient")
    val rotation by infinite.animateFloat(
        0f,
        if (rotationRunning) 360f else 0f,
        infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot",
    )
    val glowPulse by infinite.animateFloat(
        if (glowActive) 0.55f else 1f,
        1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val scan by infinite.animateFloat(
        0f,
        if (scanRunning) 1f else 0f,
        infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        label = "scan",
    )

    val zone: (String) -> Modifier = { label -> Modifier.hierarchyZone(label, controller.hierarchyOverlay, measurer, accent) }

    Box(modifier.fillMaxSize().background(Color(0xFF05070B))) {
        StatusAtmosphere(accent)
        if (particlesRunning || effects.particleQuality > 0f) {
            StatusParticleField(accent, running = particlesRunning, effectsQuality = effects.particleQuality)
        }

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val widthMod = controller.deviceWidth.widthDp?.let { Modifier.width(it.dp) } ?: Modifier.fillMaxWidth()
            Box(widthMod) {
                EdgeLitStatusPanel(
                    accent = accent,
                    materialize = motion.panelMaterialize.value,
                    scan = scan,
                    effectsQuality = effects.scanQuality,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    PanelContent(data, accent, sigil, motion, rotation, glowPulse, zone)
                }
                Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp).then(zone("Event overlay"))) {
                    EventOverlayChip(data.overlay, accent, motion.overlayReveal.value, motion.eventFlash.value)
                }
            }

            Spacer(Modifier.height(20.dp))
            if (controller.entranceMode == EntranceMode.EVERYDAY_OPEN) {
                Text(
                    "Everyday-open replays: ${controller.everydayReplays} · tap Replay 10–15× to feel repeated use",
                    color = Color(0xFF7E8CA0),
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(10.dp))
            }
            StatusPrototypeControls(controller = controller, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(28.dp))
        }

        if (controller.diagnosticsOverlay) {
            val frameMs = rememberApproxFrameMs(active = true)
            val majorActive = controller.entranceMode == EntranceMode.MAJOR_EVENT && data.overlay.kind != StatusOverlayKind.NONE
            StatusDiagnosticsPanel(
                info =
                    DiagnosticsInfo(
                        state = data.stateId.label,
                        variant = controller.variant.displayName,
                        speed = controller.motionSpeed.label,
                        reducedMotion = reduced,
                        effectsQuality = controller.effectsQuality.label,
                        particleCount = particleCountFor(effects.particleQuality),
                        particlesActive = particlesRunning,
                        scanActive = scanRunning,
                        sigilIdleActive = rotationRunning,
                        infinitePaused = !(rotationRunning || scanRunning || particlesRunning),
                        entranceMode = controller.entranceMode.label,
                        majorEventActive = majorActive,
                        approxEntranceMs = approxEntranceMs(controller.entranceMode, motionSpec),
                        deviceWidth = controller.deviceWidth.label,
                    ),
                frameMs = frameMs,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
            )
        }
    }
}

/** A rough critical-path entrance duration for the diagnostics read-out (approximate by design). */
private fun approxEntranceMs(
    entranceMode: EntranceMode,
    motion: MotionSpec,
): Int {
    val t = AscendMotionTokens
    val base =
        if (entranceMode == EntranceMode.EVERYDAY_OPEN) {
            t.QUICK + t.STANDARD + 4 * t.STAGGER + t.STANDARD
        } else {
            t.STANDARD + t.DELIBERATE + t.DELIBERATE + 4 * t.STAGGER + t.STANDARD + t.DRAMATIC
        }
    return motion.duration(base)
}

@Composable
private fun PanelContent(
    data: StatusPrototypeData,
    accent: Color,
    sigil: StatusSigilVariant,
    motion: StatusPrototypeMotion,
    rotation: Float,
    glowPulse: Float,
    zone: (String) -> Modifier,
) {
    Column(Modifier.fillMaxWidth().padding(24.dp)) {
        Box(zone("Identity + rank")) { IdentityBlock(data, accent, motion.levelReveal.value) }
        Spacer(Modifier.height(16.dp))
        Box(zone("Sigil"), contentAlignment = Alignment.Center) { SigilBlock(data, sigil, motion, rotation, glowPulse) }
        Spacer(Modifier.height(20.dp))
        Box(zone("XP + class XP")) {
            ProgressionBars(data, accent, motion.xpFill.value, motion.classXpFill.value, motion.secondaryXpFill.value)
        }
        Spacer(Modifier.height(22.dp))
        Box(zone("Attributes")) {
            AttributeMeters(
                data,
                reveals = motion.attrReveal.map { it.value },
                values = motion.attrValue.map { it.value },
                pulses = motion.attrPulse.map { it.value },
            )
        }
        Spacer(Modifier.height(6.dp))
        Box(zone("Unique proficiency")) { ProficiencyBlock(data, accent, motion.proficiencyReveal.value, motion.proficiencyPulse.value) }
        Spacer(Modifier.height(20.dp))
        Box(zone("Adaptive training + quest")) { ProgressionInfo(data, accent, motion.proficiencyReveal.value) }
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
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StatusSigil(
            state = state,
            animation = StatusSigilAnimation(motion.sigilAssembly.value, rotation, glowPulse),
            effectsQuality = 1f,
            modifier =
                Modifier
                    .fillMaxWidth(0.5f)
                    .aspectRatio(1f)
                    .semantics { contentDescription = "${data.variant.displayName} sigil, tier ${data.trainingTier}" }
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
