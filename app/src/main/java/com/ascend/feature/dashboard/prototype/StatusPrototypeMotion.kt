package com.ascend.feature.dashboard.prototype

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Stable
import com.ascend.core.designsystem.motion.AscendEasing
import com.ascend.core.designsystem.motion.AscendMotionTokens
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

        // 5. The event beat (fake — no reward logic). The overlay always reveals (quick in
        // everyday open); the celebratory pulses/flashes are reserved for major events.
        if (data.overlay.kind != StatusOverlayKind.NONE) {
            val overlayToken = if (everyday) AscendMotionTokens.QUICK else AscendMotionTokens.STANDARD
            overlayReveal.animateTo(1f, motion.tween(overlayToken, AscendEasing.emphasize))
        }
        if (!everyday) playEventEmphasis(data, motion)
    }

    private suspend fun playEventEmphasis(
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
        attrReveal.forEach { it.snapTo(0f) }
        attrValue.forEach { it.snapTo(0f) }
        attrPulse.forEach { it.snapTo(1f) }
    }

    private companion object {
        const val PULSE_PEAK = 1.18f
    }
}
