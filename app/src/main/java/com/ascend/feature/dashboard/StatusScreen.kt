package com.ascend.feature.dashboard

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ascend.core.designsystem.motion.MotionSpec
import com.ascend.feature.dashboard.prototype.AttributeMeters
import com.ascend.feature.dashboard.prototype.EdgeLitStatusPanel
import com.ascend.feature.dashboard.prototype.EntranceMode
import com.ascend.feature.dashboard.prototype.EventOverlayChip
import com.ascend.feature.dashboard.prototype.IdentityBlock
import com.ascend.feature.dashboard.prototype.OrnateSigil
import com.ascend.feature.dashboard.prototype.OrnateSigilAnimation
import com.ascend.feature.dashboard.prototype.OrnateSigilState
import com.ascend.feature.dashboard.prototype.ProficiencyBlock
import com.ascend.feature.dashboard.prototype.ProgressionBars
import com.ascend.feature.dashboard.prototype.StatusAtmosphere
import com.ascend.feature.dashboard.prototype.StatusClassVariant
import com.ascend.feature.dashboard.prototype.StatusFog
import com.ascend.feature.dashboard.prototype.StatusPalette
import com.ascend.feature.dashboard.prototype.StatusParticleField
import com.ascend.feature.dashboard.prototype.StatusPrototypeData
import com.ascend.feature.dashboard.prototype.StatusPrototypeMotion
import com.ascend.feature.dashboard.prototype.StatusSigilVariant
import com.ascend.feature.dashboard.prototype.sigilDescription

private const val ATTRIBUTE_COUNT = 5

/**
 * The production Status screen: the approved ornate composition — a dominant edge-lit panel over a
 * dark holographic field, framed by an outer energy border, with the centred class sigil behind the
 * identity — driven by **real** progression data. Domain facts (level / rank / XP / attributes /
 * class) come from [StatusMotionViewModel]; the entrance and event beats replay the persisted
 * ProgressionEventQueue drained on real quest/workout completions, exactly once. Reduced motion
 * collapses every tween and stops all ambient animation.
 */
@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    viewModel: StatusMotionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val domain = state.domain

    if (state.phase == StatusPhase.LOADING || domain == null) {
        Box(modifier.fillMaxSize().background(StatusPalette.groundDeep), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val data =
        StatusComposition.map(
            hunterName = state.hunterName,
            domain = domain,
            classInfo = state.classInfo,
            batch = state.pendingBatch,
            reducedMotion = state.reducedMotion,
        )
    val reduced = state.reducedMotion
    val motionSpec = MotionSpec(reducedMotion = reduced)
    val accent = StatusSigilVariant.of(data.variant).core

    val motion = remember { StatusPrototypeMotion(ATTRIBUTE_COUNT) }

    // Entrance: fast, low-ceremony everyday open. Replays when the ViewModel bumps entranceKey.
    LaunchedEffect(state.entranceKey, domain.level, domain.rank) {
        motion.play(data, motionSpec, EntranceMode.EVERYDAY_OPEN)
    }
    // A real earning batch drained from the queue: play its beat (cinematic for a major unlock),
    // then mark it consumed so it never replays.
    LaunchedEffect(state.pendingBatch) {
        val batch = state.pendingBatch
        if (batch.isNotEmpty()) {
            val mode = if (data.majorUnlock) EntranceMode.MAJOR_EVENT else EntranceMode.EVERYDAY_OPEN
            motion.play(data, motionSpec, mode)
            viewModel.onBatchPlayed(batch.map { it.id })
        }
    }

    // Ambient motion — off entirely under reduced motion.
    val ambient = !reduced
    val infinite = rememberInfiniteTransition(label = "ambient")
    val rotation by infinite.animateFloat(
        0f,
        if (ambient) 360f else 0f,
        infiniteRepeatable(tween(24_000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot",
    )
    val scan by infinite.animateFloat(
        0f,
        if (ambient) 1f else 0f,
        infiniteRepeatable(tween(5200, easing = LinearEasing), RepeatMode.Restart),
        label = "scan",
    )
    val sweep by infinite.animateFloat(
        0f,
        if (ambient) 1f else 0.5f,
        infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweep",
    )

    Box(modifier.fillMaxSize().background(StatusPalette.groundDeep)) {
        StatusAtmosphere(sweep = sweep)
        StatusFog(running = ambient)
        StatusParticleField(running = ambient)

        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            StatusFramedPanel(data, motion, accent, rotation, scan, ambient)
            Spacer(Modifier.height(20.dp))
            StatusControls(
                reducedMotion = reduced,
                onReplay = viewModel::replayEntrance,
                onReducedMotionChange = viewModel::setReducedMotion,
            )
            Spacer(Modifier.height(28.dp))
        }
    }
}

/**
 * Structure A: a single dominant edge-lit panel whose own edge-lighting *is* the outer energy frame
 * (no separate wrapping shell). The frame energy/pulse/rotation ride the panel's perimeter.
 */
@Composable
private fun StatusFramedPanel(
    data: StatusPrototypeData,
    motion: StatusPrototypeMotion,
    accent: Color,
    rotation: Float,
    scan: Float,
    ambient: Boolean,
) {
    Box(Modifier.fillMaxWidth()) {
        EdgeLitStatusPanel(
            accent = accent,
            materialize = motion.panelMaterialize.value,
            scan = scan,
            modifier = Modifier.fillMaxWidth(),
            selfFraming = true,
            frameEnergy = motion.frameEnergy.value,
            framePulse = motion.eventFlash.value,
            frameRotation = rotation,
            frameMotion = ambient,
        ) {
            StatusPanelContent(data, accent, motion, rotation, ambient)
        }
        Box(Modifier.align(Alignment.TopCenter).padding(top = 8.dp)) {
            EventOverlayChip(data.overlay, accent, motion.overlayReveal.value, motion.eventFlash.value)
        }
    }
}

@Composable
private fun StatusPanelContent(
    data: StatusPrototypeData,
    accent: Color,
    motion: StatusPrototypeMotion,
    rotation: Float,
    ambient: Boolean,
) {
    val hasClass = data.variant != StatusClassVariant.NEUTRAL
    val sigilState =
        OrnateSigilState(
            rankTier = data.rankTier,
            variant = data.variant,
            playerRing = data.playerXpFraction,
            classRing = if (hasClass) data.classXpFraction else null,
            activeMedallion = data.activeMedallionIndex,
            showProficiency = data.showProficiencyMedallion && hasClass,
            newRankLayer = data.majorUnlock && data.overlay.emphasizedAttribute == null,
        )
    val sigilAnim =
        OrnateSigilAnimation(
            assembly = motion.sigilAssembly.value,
            rotation = rotation,
            glow = motion.eventFlash.value,
            playerRingTrim = motion.xpFill.value,
            classRingTrim = motion.classXpFill.value,
            medallionPulse = motion.attrPulse.map { it.value },
            proficiencyPulse = motion.proficiencyPulse.value,
        )

    Box(Modifier.fillMaxWidth().clipToBounds()) {
        // The ornate seal sits BEHIND the readable content at low opacity — atmospheric.
        OrnateSigil(
            state = sigilState,
            animation = sigilAnim,
            semanticDescription =
                sigilDescription(data, data.playerXpFraction, if (hasClass) data.classXpFraction else null, data.activeMedallionIndex),
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(0.98f)
                    .aspectRatio(1f)
                    .offset(y = (-28).dp),
            frameMotion = ambient,
        )
        Column(Modifier.fillMaxWidth().padding(24.dp)) {
            IdentityBlock(data, accent, motion.levelReveal.value)
            Spacer(Modifier.height(20.dp))
            ProgressionBars(data, accent, motion.xpFill.value, motion.classXpFill.value, motion.secondaryXpFill.value)
            Spacer(Modifier.height(22.dp))
            AttributeMeters(
                data,
                reveals = motion.attrReveal.map { it.value },
                values = motion.attrValue.map { it.value },
                pulses = motion.attrPulse.map { it.value },
            )
            if (hasClass) {
                Spacer(Modifier.height(6.dp))
                ProficiencyBlock(data, accent, motion.proficiencyReveal.value, motion.proficiencyPulse.value)
            }
        }
    }
}

/**
 * Production Status controls — presentation only. Progression is earned from real quest and workout
 * completions (which enqueue the animated events), so there is nothing to "simulate": the controls
 * just re-run the entrance and toggle reduced motion.
 */
@Composable
private fun StatusControls(
    reducedMotion: Boolean,
    onReplay: () -> Unit,
    onReducedMotionChange: (Boolean) -> Unit,
) {
    Surface(
        color = Color(0xFF10151F).copy(alpha = 0.85f),
        shape = androidx.compose.material3.MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("STATUS", color = StatusPalette.label, fontSize = 11.sp, letterSpacing = 2.sp)
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onReplay, modifier = Modifier.fillMaxWidth()) { Text("Replay entrance") }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = reducedMotion, onCheckedChange = onReducedMotionChange)
                Text("  Reduced motion", color = StatusPalette.infoLine)
            }
        }
    }
}
