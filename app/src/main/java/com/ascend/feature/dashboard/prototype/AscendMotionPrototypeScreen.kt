package com.ascend.feature.dashboard.prototype

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ascend.core.designsystem.motion.AscendVerb
import com.ascend.core.designsystem.motion.MotionSpec
import com.ascend.core.designsystem.motion.MotionTier
import com.ascend.core.designsystem.motion.MotionVerb
import kotlinx.coroutines.delay

private const val VIEWPORT_MAX_WIDTH_DP = 412
private const val SIGIL_SIZE_DP = 300
private const val ATTRIBUTE_COUNT = 5

// Local review-chrome colours (the shared StatusPalette carries no chip fill/border tokens).
private val ChipFill = Color(0xFF12172B)
private val ChipBorder = Color(0xFF2A335A)
private val AccentFill = Color(0xFF20264A)
private val ConsoleFill = Color(0xFF10141C)

/**
 * Debug-only, HTML-faithful motion-review screen (the corrected prototype). Unlike the production
 * Status information panel, this is a **sigil-dominant review viewport**: a compact ASCEND/rank
 * header, a large centred ceremonial sigil as the focal point, a compact identity + attribute
 * chips + Skill dock + timing/audio-hook labels, with the motion-review controls in a scrollable
 * lower console. Reuses the refined [OrnateSigil], motion tokens, and fake data — no production
 * wiring, no ProgressionEventQueue, no repositories.
 */
@Composable
fun rememberMotionPrototypeController(): PrototypeReviewController = remember { PrototypeReviewController().apply { sigilRefined = true } }

@Composable
fun AscendMotionPrototypeScreen(
    modifier: Modifier = Modifier,
    controller: PrototypeReviewController = rememberMotionPrototypeController(),
) {
    val data =
        FakeStatusPrototype.dataFor(
            controller.stateId,
            controller.variant,
            controller.reducedMotion,
            controller.forceSecondary,
            controller.weightUnit,
        )
    val reduced = controller.reducedMotion || data.reducedMotion
    val motionSpec = MotionSpec(reducedMotion = reduced, speedScale = controller.motionSpeed.scale)
    // Refined prototype warms Berserker to a red-orange (handoff hue ~25); Monk/Mage unchanged.
    val accent = classAccent(controller.variant, controller.sigilRefined)

    val motion = remember { StatusPrototypeMotion(ATTRIBUTE_COUNT) }
    LaunchedEffect(controller.stateId, controller.variant, controller.replayKey, reduced, controller.entranceMode) {
        motion.play(data, motionSpec, controller.entranceMode)
    }
    // Chained progression demo: attribute increase → player level-up (a preview of the CP3 chain).
    LaunchedEffect(controller.chainedReplayKey) {
        if (controller.chainedReplayKey > 0) {
            controller.selectState(StatusPrototypeStateId.ATTRIBUTE_UP)
            delay(1400)
            controller.selectState(StatusPrototypeStateId.PLAYER_LEVEL_UP)
        }
    }

    val rotationRunning = !reduced && controller.effectsQuality != EffectsQuality.MINIMAL
    val infinite = rememberInfiniteTransition(label = "ambient")
    val rotation by infinite.animateFloat(
        0f,
        if (rotationRunning) 360f else 0f,
        infiniteRepeatable(tween(controller.rotationPace.periodMs, easing = LinearEasing), RepeatMode.Restart),
        label = "rot",
    )
    val sweep by infinite.animateFloat(
        0f,
        if (rotationRunning) 1f else 0.5f,
        infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "sweep",
    )

    // Ambient particles/fog — faint, alive, and readability-safe; fully off under reduced motion,
    // minimal effects, or the particles toggle.
    val particlesRunning = !reduced && controller.particlesOn && controller.effectsQuality != EffectsQuality.MINIMAL
    val particleQuality = if (particlesRunning) 0.7f else 0f

    // The overlaid HUD window assembles in when selected and breathes with a slow living pulse.
    val windowOpen = controller.reviewWindow != HudWindowKind.NONE
    val windowReveal by animateFloatAsState(
        targetValue = if (windowOpen) 1f else 0f,
        animationSpec = if (reduced) snap() else tween(AscendVerb.ASSEMBLE_MS, easing = LinearEasing),
        label = "windowReveal",
    )
    val windowPulse by infinite.animateFloat(
        0.3f,
        if (reduced) 0.3f else 1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Reverse),
        label = "windowPulse",
    )

    Box(modifier.fillMaxSize().background(StatusPalette.groundDeep)) {
        StatusAtmosphere(sweep = sweep)
        StatusFog(running = particlesRunning, effectsQuality = if (particlesRunning) 0.6f else 0f)
        StatusParticleField(running = particlesRunning, effectsQuality = particleQuality)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MotionReviewViewport(
                data = data,
                controller = controller,
                accent = accent,
                motion = motion,
                rotation = rotation,
                reduced = reduced,
                motionSpec = motionSpec,
                modifier = Modifier.widthIn(max = VIEWPORT_MAX_WIDTH_DP.dp).fillMaxWidth(),
            )
            Spacer(Modifier.height(18.dp))
            MotionReviewExtraControls(controller, Modifier.widthIn(max = VIEWPORT_MAX_WIDTH_DP.dp).fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            StatusPrototypeControls(controller = controller, modifier = Modifier.widthIn(max = VIEWPORT_MAX_WIDTH_DP.dp).fillMaxWidth())
            Spacer(Modifier.height(28.dp))
        }

        // Overlaid HUD window (protocol menu) — the background subtly dims behind it, then the panel
        // assembles and breathes. Dismiss by selecting "None" in the Window control.
        if (windowReveal > 0.01f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f * windowReveal)))
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                HudReviewWindow(
                    kind = controller.reviewWindow,
                    accent = accent,
                    reveal = windowReveal,
                    pulse = windowPulse,
                    modifier = Modifier.widthIn(max = 380.dp).fillMaxWidth(),
                    burstMode = controller.burstMode,
                    skill = controller.skillVariant,
                )
            }
        }

        // Reduced-motion recognition cue: a brief flat accent flash so a Reward event is never silent.
        if (motion.recognitionCue.value > 0.001f) {
            Box(Modifier.fillMaxSize().background(accent.copy(alpha = motion.recognitionCue.value * 0.14f)))
        }
    }
}

@Composable
private fun MotionReviewViewport(
    data: StatusPrototypeData,
    controller: PrototypeReviewController,
    accent: Color,
    motion: StatusPrototypeMotion,
    rotation: Float,
    reduced: Boolean,
    motionSpec: MotionSpec,
    modifier: Modifier = Modifier,
) {
    val hasClass = data.variant != StatusClassVariant.NEUTRAL
    val classRingValue = if (hasClass) (controller.classRingOverride ?: data.classXpFraction) else null
    val body: @Composable () -> Unit = {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(StatusPalette.groundDeep)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Compact ASCEND / rank header.
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ASCEND", color = accent, fontSize = 13.sp, fontWeight = FontWeight.Bold, letterSpacing = 4.sp)
                Spacer(Modifier.weight(1f))
                Chip(data.rankLabel, accent)
            }

            // Dominant centred sigil (the focal point), with the transient event overlay on top.
            Box(
                Modifier.size(SIGIL_SIZE_DP.dp).padding(top = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                OrnateSigil(
                    state =
                        OrnateSigilState(
                            rankTier = controller.rankTierOverride ?: data.rankTier,
                            variant = data.variant,
                            playerRing = controller.playerRingOverride ?: data.playerXpFraction,
                            classRing = classRingValue,
                            activeMedallion = controller.attributeVariant,
                            showProficiency = data.showProficiencyMedallion && hasClass,
                            settledOpacity = controller.sigilOpacity,
                            newRankLayer = data.stateId == StatusPrototypeStateId.RANK_PROMOTION,
                        ),
                    animation =
                        OrnateSigilAnimation(
                            assembly = motion.sigilAssembly.value,
                            rotation = rotation,
                            glow = motion.eventFlash.value,
                            playerRingTrim = motion.xpFill.value,
                            classRingTrim = motion.classXpFill.value,
                            medallionPulse = motion.attrPulse.map { it.value },
                            proficiencyPulse = motion.proficiencyPulse.value,
                            wave = motion.wave.value,
                        ),
                    semanticDescription = "${data.variant.displayName} ceremonial sigil, ${data.rankLabel}.",
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                    frameMotion = !reduced,
                    rotationProfile = controller.rotationProfile,
                    opacityProfile = controller.opacityProfile,
                    stationaryOverlay = controller.stationaryOverlay,
                    classGeometry = controller.sigilRefined,
                    internalGlow = controller.sigilRefined && controller.sigilGlow,
                    warmAccents = controller.sigilRefined,
                )
                // CP3 level-up beat: the breakthrough burst radiates from the sigil centre during a
                // major-unlock flash (Charge lead-in → Breakthrough → settle as the flash recedes).
                if (data.majorUnlock && motion.eventFlash.value > 0.01f) {
                    BreakthroughBurst(
                        accent = accent,
                        pulse = motion.eventFlash.value,
                        mode = controller.burstMode,
                        modifier =
                            Modifier
                                .size((SIGIL_SIZE_DP * 0.62f).dp)
                                .graphicsLayer { alpha = motion.eventFlash.value },
                    )
                }
                Box(Modifier.align(Alignment.TopCenter)) {
                    EventOverlayChip(data.overlay, accent, motion.overlayReveal.value, motion.eventFlash.value)
                }
            }

            // Class identity below the sigil.
            Text(data.variant.displayName, color = StatusPalette.textPrimary, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Text(data.variant.classTitle, color = accent, fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Text("${data.hunterName}  ·  Lv ${data.level}", color = StatusPalette.textMuted, fontSize = 13.sp)

            // Compact attribute chips.
            Spacer(Modifier.height(12.dp))
            AttributeChips(data, controller.attributeVariant)

            // Skill dock.
            Spacer(Modifier.height(12.dp))
            SkillDock(controller)

            // Timing + audio/haptic debug labels.
            if (controller.showTimingLabels || controller.showAudioHaptic) {
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (controller.showTimingLabels) {
                        Text(
                            timingLabel(controller, motionSpec),
                            color = StatusPalette.textMuted,
                            fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    if (controller.showAudioHaptic) Text("♪ cue · ⌁ haptic", color = accent.copy(alpha = 0.8f), fontSize = 11.sp)
                }
            }
        }
    }

    if (controller.frameMode == FrameMode.FRAMED) {
        StatusEnergyFrame(
            energy = motion.frameEnergy.value,
            pulse = motion.eventFlash.value,
            rotation = rotation,
            frameMotion = !reduced,
            modifier = modifier,
        ) { body() }
    } else {
        Box(modifier) { body() }
    }
}

private fun timingLabel(
    controller: PrototypeReviewController,
    motionSpec: MotionSpec,
): String {
    val ms =
        if (motionSpec.reducedMotion) {
            AscendVerb.REDUCED_MS
        } else {
            (controller.motionVerb.durationMs * controller.motionSpeed.scale).toInt()
        }
    return "${controller.motionVerb.name} · ${ms}ms · ${controller.tier.name.lowercase()} · ×${controller.motionSpeed.label}"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AttributeChips(
    data: StatusPrototypeData,
    activeIndex: Int,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        data.attributes.forEachIndexed { i, line ->
            val active = i == activeIndex
            val border = if (active) StatusPalette.cyan else ChipBorder
            Row(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(ChipFill)
                    .border(1.dp, border, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
                    .semantics { contentDescription = "${line.name} ${line.value}${if (active) ", active" else ""}" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(line.name.take(3).uppercase(), color = StatusPalette.textMuted, fontSize = 10.sp)
                Spacer(Modifier.size(6.dp))
                Text("${line.value}", color = StatusPalette.textPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SkillDock(controller: PrototypeReviewController) {
    Column(Modifier.fillMaxWidth()) {
        Text("SKILL DOCK", color = StatusPalette.textMuted, fontSize = 10.sp, letterSpacing = 2.sp)
        Spacer(Modifier.height(6.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PrototypeSkill.entries.forEach { skill ->
                val selected = skill == controller.skillVariant
                Row(
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (selected) AccentFill else ChipFill)
                        .border(1.dp, if (selected) StatusPalette.cyan else ChipBorder, RoundedCornerShape(999.dp))
                        .clickable { controller.skillVariant = skill }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .semantics { contentDescription = "Skill ${skill.displayName}${if (selected) ", selected" else ""}" },
                ) {
                    Text(skill.displayName, color = if (selected) StatusPalette.textPrimary else StatusPalette.textMuted, fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun Chip(
    label: String,
    accent: Color,
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(AccentFill)
            .border(1.dp, accent, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp),
    ) {
        Text(label, color = accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}

private val AttrShortNames = listOf("STR", "END", "AGI", "DIS", "REC")

/**
 * The HTML-specific motion-review controls (the rest live in the shared [StatusPrototypeControls]
 * console below): FRAMED/CANVAS, motion verb, tier, attribute selector, review toggles, a chained
 * progression replay, and the implementation-notes panel.
 */
@Composable
private fun MotionReviewExtraControls(
    controller: PrototypeReviewController,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ConsoleFill)
            .padding(16.dp),
    ) {
        Label("HUD window")
        SelectRow(HudWindowKind.entries, controller.reviewWindow, { it.label }) { controller.reviewWindow = it }

        Label("Level-up burst")
        SelectRow(BurstMode.entries, controller.burstMode, { it.label }) { controller.burstMode = it }

        Label("Framing")
        SelectRow(FrameMode.entries, controller.frameMode, { it.label }) { controller.frameMode = it }

        Label("Motion verb")
        SelectRow(
            MotionVerb.entries,
            controller.motionVerb,
            { "${it.name.lowercase().replaceFirstChar(Char::uppercase)} ${it.durationMs}ms" },
        ) {
            controller.motionVerb = it
        }

        Label("Tier")
        SelectRow(MotionTier.entries, controller.tier, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { controller.tier = it }

        Label("Attribute variant")
        SelectRow((0 until ATTRIBUTE_COUNT).toList(), controller.attributeVariant, { AttrShortNames.getOrElse(it) { "A$it" } }) {
            controller.attributeVariant = it
        }

        Label("Energy")
        ToggleChip("Ambient particles", controller.particlesOn) { controller.particlesOn = it }
        ToggleChip("Sigil internal glow", controller.sigilGlow) { controller.sigilGlow = it }

        Label("Review")
        ToggleChip("Timing labels", controller.showTimingLabels) { controller.showTimingLabels = it }
        ToggleChip("Audio / haptic hooks", controller.showAudioHaptic) { controller.showAudioHaptic = it }
        ToggleChip("Implementation notes", controller.showImplNotes) { controller.showImplNotes = it }

        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Action("Replay", Modifier.weight(1f)) { controller.replay() }
            Action("Chained replay", Modifier.weight(1f)) { controller.chainedReplay() }
        }

        if (controller.showImplNotes) {
            Spacer(Modifier.height(12.dp))
            Label("Compose implementation notes")
            ImplNote("XP ring trace → Canvas drawArc, animated sweep, cached Path")
            ImplNote("Center star / anchor → AnimatedVectorDrawable candidate")
            ImplNote("Medallion stagger → AnimatedVisibility + staggered delay")
            ImplNote("Value lock → tween(CubicBezier 0.34,1.56,0.64,1) / spring MediumBouncy")
            ImplNote("Level-up burst → pre-authored vector sequence (AVD)")
            ImplNote("Particle convergence → requires real-device profiling")
            ImplNote("Ambient rotation → rememberInfiniteTransition, 150–260s, never a spinner")
        }
    }
}

@Composable
private fun Label(text: String) {
    Text(
        text.uppercase(),
        color = StatusPalette.label,
        fontSize = 11.sp,
        letterSpacing = 1.5.sp,
        modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun <T> SelectRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { option ->
            val on = option == selected
            Box(
                Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (on) AccentFill else ChipFill)
                    .border(1.dp, if (on) StatusPalette.cyan else ChipBorder, RoundedCornerShape(8.dp))
                    .clickable { onSelect(option) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(label(option), color = if (on) StatusPalette.textPrimary else StatusPalette.textMuted, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun ToggleChip(
    label: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (checked) AccentFill else ChipFill)
            .border(1.dp, if (checked) StatusPalette.cyan else ChipBorder, RoundedCornerShape(8.dp))
            .clickable { onChange(!checked) }
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .semantics { contentDescription = "$label ${if (checked) "on" else "off"}" },
    ) {
        Text(
            "${if (checked) "☑" else "☐"}  $label",
            color = if (checked) StatusPalette.textPrimary else StatusPalette.textMuted,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun Action(
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AccentFill)
            .border(1.dp, StatusPalette.cyan, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = StatusPalette.cyan, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun ImplNote(text: String) {
    Text("· $text", color = StatusPalette.textMuted, fontSize = 11.sp, modifier = Modifier.padding(vertical = 2.dp))
}
