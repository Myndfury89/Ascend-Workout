package com.ascend.feature.dashboard.prototype

import androidx.compose.animation.core.Animatable
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

    val attrReveal = List(attributeCount) { Animatable(0f) }
    val attrValue = List(attributeCount) { Animatable(0f) }
    val attrPulse = List(attributeCount) { Animatable(1f) }

    /** The full entrance + event beat for one fake snapshot. */
    suspend fun play(
        data: StatusPrototypeData,
        motion: MotionSpec,
    ) {
        reset()

        // 1. Panel materialises, then the sigil assembles from the centre.
        panelMaterialize.animateTo(1f, motion.tween(AscendMotionTokens.STANDARD, AscendEasing.settle))
        sigilAssembly.animateTo(1f, motion.tween(AscendMotionTokens.DELIBERATE, AscendEasing.emphasize))

        // 2. Identity + progression bars fill.
        levelReveal.animateTo(1f, motion.tween(AscendMotionTokens.QUICK))
        coroutineScope {
            launch { xpFill.animateTo(data.playerXpFraction, motion.tween(AscendMotionTokens.DELIBERATE, AscendEasing.settle)) }
            launch { classXpFill.animateTo(data.classXpFraction, motion.tween(AscendMotionTokens.DELIBERATE, AscendEasing.settle)) }
            data.secondaryClassXpFraction?.let { frac ->
                launch { secondaryXpFill.animateTo(frac, motion.tween(AscendMotionTokens.DELIBERATE, AscendEasing.settle)) }
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

        // 5. The event beat for this state (fake — no reward logic).
        playEventBeat(data, motion)
    }

    private suspend fun playEventBeat(
        data: StatusPrototypeData,
        motion: MotionSpec,
    ) {
        if (data.overlay.kind == StatusOverlayKind.NONE) return
        overlayReveal.animateTo(1f, motion.tween(AscendMotionTokens.STANDARD, AscendEasing.emphasize))
        when (data.overlay.kind) {
            StatusOverlayKind.ATTRIBUTE -> {
                val idx = data.attributes.indexOfFirst { it.emphasized }.coerceAtLeast(0)
                pulse(attrPulse.getOrNull(idx), motion)
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

    private suspend fun flash(motion: MotionSpec) {
        eventFlash.snapTo(0f)
        eventFlash.animateTo(1f, motion.tween(AscendMotionTokens.QUICK, AscendEasing.emphasize))
        eventFlash.animateTo(0f, motion.tween(AscendMotionTokens.DRAMATIC, AscendEasing.pulse))
    }

    private suspend fun reset() {
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
        attrReveal.forEach { it.snapTo(0f) }
        attrValue.forEach { it.snapTo(0f) }
        attrPulse.forEach { it.snapTo(1f) }
    }

    private companion object {
        const val PULSE_PEAK = 1.18f
    }
}
