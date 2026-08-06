package com.ascend.feature.dashboard.prototype

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import com.ascend.core.designsystem.motion.AscendEasing
import com.ascend.core.designsystem.motion.AscendMotionTokens
import com.ascend.core.designsystem.motion.AscendVerb
import com.ascend.core.designsystem.motion.MotionSpec
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The animation-state layer for the revised prototype — the only place touching durations and
 * easing. A plain holder of [Animatable]s orchestrated by [play] into the staged reveal, then
 * the event-specific beat for the current state. Reduced motion collapses every tween to an
 * instant cut through [MotionSpec] without changing this code path. No real reward logic here —
 * these visualise fake state transitions only.
 */
@Stable
class StatusPrototypeMotion(attributeCount: Int) {
    val frameEnergy = Animatable(0f)
    val panelMaterialize = Animatable(0f)
    val sigilAssembly = Animatable(0f)
    val levelReveal = Animatable(0f)
    val xpFill = Animatable(0f)
    val classXpFill = Animatable(0f)
    val secondaryXpFill = Animatable(0f)
    val questFill = Animatable(0f)
    val questPulse = Animatable(1f)
    val proficiencyReveal = Animatable(0f)
    val proficiencyPulse = Animatable(1f)
    val overlayReveal = Animatable(0f)
    val eventFlash = Animatable(0f)
    val wave = Animatable(0f)

    /** Reduced-motion recognition cue: a brief flat, non-kinetic panel flash so a Reward event
     *  (especially a personal record) is never silent when all kinetic motion is collapsed. */
    val recognitionCue = Animatable(0f)

    /** Quest Complete HUD window: [questWindowReveal] is its assemble (0..1); [questWindowPulse] is
     *  the single restrained energy pulse that travels through the panel as it locks in. */
    val questWindowReveal = Animatable(0f)
    val questWindowPulse = Animatable(1f)

    val attrReveal = List(attributeCount) { Animatable(0f) }
    val attrValue = List(attributeCount) { Animatable(0f) }
    val attrPulse = List(attributeCount) { Animatable(1f) }

    /**
     * The staged entrance + event beat for one fake snapshot. [entranceMode] selects the timing
     * profile: [EntranceMode.EVERYDAY_OPEN] is a fast, low-ceremony open (short tokens, minimal
     * sigil assembly, critical content readable quickly); [EntranceMode.MAJOR_EVENT] uses the
     * fuller cinematic beat. Both read every duration from the shared [MotionSpec].
     */
    suspend fun play(
        data: StatusPrototypeData,
        motion: MotionSpec,
        entranceMode: EntranceMode = EntranceMode.MAJOR_EVENT,
    ) {
        reset()
        val everyday = entranceMode == EntranceMode.EVERYDAY_OPEN
        val panelToken = if (everyday) AscendMotionTokens.QUICK else AscendMotionTokens.STANDARD
        val sigilToken = if (everyday) AscendMotionTokens.QUICK else AscendMotionTokens.DELIBERATE
        val sigilEasing = if (everyday) AscendEasing.settle else AscendEasing.emphasize
        val fillToken = if (everyday) AscendMotionTokens.STANDARD else AscendMotionTokens.DELIBERATE

        // 0. The outer energy frame wakes first — quickly in everyday, more deliberately for a
        // major event so the shell carries the drama while the centre stays controlled.
        coroutineScope {
            launch {
                val frameToken = if (everyday) AscendMotionTokens.QUICK else AscendMotionTokens.DELIBERATE
                frameEnergy.animateTo(1f, motion.tween(frameToken, if (everyday) AscendEasing.settle else AscendEasing.emphasize))
            }
            // 1. Panel materialises; in everyday open the sigil barely assembles (no reconstruction).
            launch { panelMaterialize.animateTo(1f, motion.tween(panelToken, AscendEasing.settle)) }
        }
        if (everyday) sigilAssembly.snapTo(0.7f)
        sigilAssembly.animateTo(1f, motion.tween(sigilToken, sigilEasing))

        // 2. Identity + progression bars fill (parallel so critical content is quick).
        levelReveal.animateTo(1f, motion.tween(AscendMotionTokens.QUICK))
        coroutineScope {
            launch { xpFill.animateTo(data.playerXpFraction, motion.tween(fillToken, AscendEasing.settle)) }
            launch { classXpFill.animateTo(data.classXpFraction, motion.tween(fillToken, AscendEasing.settle)) }
            data.secondaryClassXpFraction?.let { frac ->
                launch { secondaryXpFill.animateTo(frac, motion.tween(fillToken, AscendEasing.settle)) }
            }
        }

        // 3. Attributes stagger in and count up.
        coroutineScope {
            data.attributes.forEachIndexed { i, line ->
                if (i >= attrReveal.size) return@forEachIndexed
                launch {
                    attrReveal[i].animateTo(1f, motion.tween(AscendMotionTokens.QUICK))
                    attrValue[i].animateTo(line.value.toFloat(), motion.tween(AscendMotionTokens.STANDARD, AscendEasing.settle))
                }
                delay(motion.stagger(1).toLong())
            }
        }

        // 4. Unique proficiency reveal + quest fill.
        proficiencyReveal.animateTo(1f, motion.tween(AscendMotionTokens.QUICK))
        questFill.animateTo(data.questFraction, motion.tween(AscendMotionTokens.STANDARD, AscendEasing.settle))

        // 5. The event beat (fake — no reward logic). A quest completion plays its own ordered plan
        // (Quest Complete window first, then the resulting Ascension); every other batch keeps the
        // single-overlay path unchanged.
        val quest = data.presentationPlan?.questComplete
        if (quest != null) {
            playQuestCompletionPlan(data, motion, everyday)
            return
        }
        // The overlay always reveals (quick in everyday open). Reward-tier emphasis (the attribute
        // center-out wave + medallion/quest pulse) fires in BOTH modes — a Reward cue is never silent;
        // the cinematic Ascension flash stays major-only.
        if (data.overlay.kind != StatusOverlayKind.NONE) {
            val overlayToken = if (everyday) AscendMotionTokens.QUICK else AscendMotionTokens.STANDARD
            overlayReveal.animateTo(1f, motion.tween(overlayToken, AscendEasing.emphasize))
        }
        playRewardEmphasis(data, motion)
        if (!everyday) playAscensionFlash(data, motion)
        // Reduced motion collapses every kinetic beat to nothing, so a Reward event would be silent.
        // Emit the required brief flat recognition cue instead (a real duration, bypassing collapse).
        if (motion.reducedMotion && isRewardOverlay(data.overlay.kind)) playRecognitionCue()
    }

    /**
     * The quest-completion plan: Quest Complete window (Reward-tier) → brief settle → the resulting
     * Ascension beat (rank / player level / class level) when the same batch crossed one. Ordinary
     * XP/attribute beats are NOT replayed — they are summarised inside the window. Under reduced
     * motion the window resolves instantly with a readable dwell + the flat recognition cue, and any
     * Ascension collapses to its immediate value (never a full Ascension-length animation).
     */
    private suspend fun playQuestCompletionPlan(
        data: StatusPrototypeData,
        motion: MotionSpec,
        everyday: Boolean,
    ) {
        playQuestWindowBeat(motion, everyday)
        val ascension = data.presentationPlan?.ascensionOverlay ?: return
        delay(QUEST_SETTLE_MS)
        val overlayToken = if (everyday) AscendMotionTokens.QUICK else AscendMotionTokens.STANDARD
        overlayReveal.animateTo(1f, motion.tween(overlayToken, AscendEasing.emphasize))
        // data.overlay == the Ascension overlay for a quest batch (panelOverlay prefers it), so the
        // existing Ascension flash + breakthrough burst fire here — after the quest window, not instead.
        if (ascension.kind != StatusOverlayKind.NONE && !everyday) playAscensionFlash(data, motion)
    }

    /** Assemble the Quest Complete window, one restrained energy pulse, a readable dwell, then fade. */
    private suspend fun playQuestWindowBeat(
        motion: MotionSpec,
        everyday: Boolean,
    ) {
        questWindowReveal.snapTo(0f)
        questWindowPulse.snapTo(1f)
        if (motion.reducedMotion) {
            // No assembly travel / scaling / pulse — final content immediately, plus the flat cue.
            questWindowReveal.snapTo(1f)
            playRecognitionCue()
            delay(QUEST_WINDOW_DWELL_MS)
            questWindowReveal.snapTo(0f)
            return
        }
        val revealToken = if (everyday) AscendMotionTokens.STANDARD else AscendMotionTokens.DELIBERATE
        questWindowReveal.animateTo(1f, motion.tween(revealToken, AscendEasing.emphasize))
        questWindowPulse.animateTo(QUEST_PULSE_PEAK, motion.tween(AscendMotionTokens.QUICK, AscendEasing.emphasize))
        questWindowPulse.animateTo(1f, motion.tween(AscendMotionTokens.STANDARD, AscendEasing.pulse))
        delay(QUEST_WINDOW_DWELL_MS)
        questWindowReveal.animateTo(0f, motion.tween(AscendMotionTokens.STANDARD, AscendEasing.settle))
    }

    /** Reward-tier cue: the attribute center-out wave + medallion pulse, or a proficiency/quest pulse. */
    private suspend fun playRewardEmphasis(
        data: StatusPrototypeData,
        motion: MotionSpec,
    ) {
        when (data.overlay.kind) {
            StatusOverlayKind.ATTRIBUTE -> {
                val idx = data.attributes.indexOfFirst { it.emphasized }.coerceAtLeast(0)
                coroutineScope {
                    launch { pulse(attrPulse.getOrNull(idx), motion) }
                    launch { playAttributeWave(idx, motion) }
                }
            }
            StatusOverlayKind.PROFICIENCY -> pulse(proficiencyPulse, motion)
            StatusOverlayKind.QUEST_PROGRESS, StatusOverlayKind.QUEST_COMPLETE -> pulse(questPulse, motion)
            else -> Unit
        }
    }

    /** Ascension-tier cue: the celebratory flash for a level-up / rank / PR / progression-complete. */
    private suspend fun playAscensionFlash(
        data: StatusPrototypeData,
        motion: MotionSpec,
    ) {
        when (data.overlay.kind) {
            StatusOverlayKind.PLAYER_LEVEL_UP, StatusOverlayKind.CLASS_LEVEL_UP,
            StatusOverlayKind.PERSONAL_RECORD, StatusOverlayKind.RANK_PROMOTION,
            StatusOverlayKind.PROGRESSION_COMPLETE,
            -> flash(motion)
            else -> Unit
        }
    }

    private suspend fun pulse(
        anim: Animatable<Float, *>?,
        motion: MotionSpec,
    ) {
        anim ?: return
        anim.animateTo(PULSE_PEAK, motion.tween(AscendMotionTokens.QUICK, AscendEasing.emphasize))
        anim.animateTo(1f, motion.tween(AscendMotionTokens.STANDARD, AscendEasing.pulse))
    }

    /**
     * The attribute center-out wave: expands 0→1 with the attribute's personality, then settles to 0
     * (invisible). Reduced motion skips the kinetic wave — the flat medallion brighten is the cue.
     */
    private suspend fun playAttributeWave(
        index: Int,
        motion: MotionSpec,
    ) {
        if (motion.reducedMotion) return
        val profile = AttributeWaveCatalog.forIndex(index)
        wave.snapTo(0f)
        wave.animateTo(
            1f,
            tween(durationMillis = (profile.durationMs * motion.speedScale).toInt().coerceAtLeast(1), easing = profile.easing),
        )
        wave.snapTo(0f)
    }

    /**
     * The reduced-motion recognition cue: a flat non-kinetic flash over [AscendVerb.RECOGNITION_CUE_MS]
     * (up then down, linear). Uses a raw tween on purpose — it must have a real duration even when
     * reduced motion collapses every other tween to zero. No scale, no movement — alpha only.
     */
    private suspend fun playRecognitionCue() {
        val half = (AscendVerb.RECOGNITION_CUE_MS / 2).coerceAtLeast(1)
        recognitionCue.snapTo(0f)
        recognitionCue.animateTo(1f, tween(durationMillis = half, easing = LinearEasing))
        recognitionCue.animateTo(0f, tween(durationMillis = half, easing = LinearEasing))
    }

    private suspend fun flash(motion: MotionSpec) {
        eventFlash.snapTo(0f)
        eventFlash.animateTo(1f, motion.tween(AscendMotionTokens.QUICK, AscendEasing.emphasize))
        eventFlash.animateTo(0f, motion.tween(AscendMotionTokens.DRAMATIC, AscendEasing.pulse))
    }

    private suspend fun reset() {
        frameEnergy.snapTo(0f)
        panelMaterialize.snapTo(0f)
        sigilAssembly.snapTo(0f)
        levelReveal.snapTo(0f)
        xpFill.snapTo(0f)
        classXpFill.snapTo(0f)
        secondaryXpFill.snapTo(0f)
        questFill.snapTo(0f)
        questPulse.snapTo(1f)
        proficiencyReveal.snapTo(0f)
        proficiencyPulse.snapTo(1f)
        overlayReveal.snapTo(0f)
        eventFlash.snapTo(0f)
        wave.snapTo(0f)
        recognitionCue.snapTo(0f)
        questWindowReveal.snapTo(0f)
        questWindowPulse.snapTo(1f)
        attrReveal.forEach { it.snapTo(0f) }
        attrValue.forEach { it.snapTo(0f) }
        attrPulse.forEach { it.snapTo(1f) }
    }

    private companion object {
        const val PULSE_PEAK = 1.18f
        const val QUEST_PULSE_PEAK = 1.15f

        /** How long the Quest Complete window holds, readable, before it settles away. Kept short so
         *  several quests completing close together stay satisfying rather than tedious. */
        const val QUEST_WINDOW_DWELL_MS = 1200L

        /** The brief settle between the Quest Complete window and any following Ascension beat. */
        const val QUEST_SETTLE_MS = 220L
    }
}

/** Reward-tier overlays that must never be silent — they get the recognition cue under reduced motion. */
internal fun isRewardOverlay(kind: StatusOverlayKind): Boolean =
    kind == StatusOverlayKind.ATTRIBUTE ||
        kind == StatusOverlayKind.PROFICIENCY ||
        kind == StatusOverlayKind.QUEST_PROGRESS ||
        kind == StatusOverlayKind.QUEST_COMPLETE ||
        kind == StatusOverlayKind.PERSONAL_RECORD
