package com.ascend.feature.dashboard

import androidx.compose.animation.core.Animatable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.ascend.core.designsystem.motion.AscendEasing
import com.ascend.core.designsystem.motion.AscendMotionTokens
import com.ascend.core.designsystem.motion.MotionSpec
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ProgressionEvent
import com.ascend.core.model.ProgressionEventType
import com.ascend.core.model.Rank
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The **animation‑state layer** of the Status screen — the only place that touches
 * durations, easing and interpolation. It's a plain holder of [Animatable]s driven
 * from the composable's effects; the ViewModel feeds it domain values and event
 * batches and never sees a single millisecond.
 *
 * The XP bar is driven off a single animated **lifetime‑XP** value swept through the
 * level curve, so a level‑up (the bar filling, snapping, and continuing) falls out
 * of the math for free; the LEVEL_UP event only fires the celebratory flash.
 */
@Stable
class StatusMotion(private val attributes: List<AttributeType>) {
    val lifetime = Animatable(0f)
    val headerAlpha = Animatable(0f)
    val xpBarAlpha = Animatable(0f)
    val levelFlash = Animatable(0f)
    val rankFlash = Animatable(0f)
    val attrValue = attributes.associateWith { Animatable(0f) }
    val attrReveal = attributes.associateWith { Animatable(0f) }
    val attrPulse = attributes.associateWith { Animatable(1f) }

    var displayedRank by mutableStateOf(Rank.INITIATE)
        private set

    // Serialises entrance vs. batch playback so their Animatables never fight.
    private val lock = Mutex()

    /** Staged reveal: header, XP bar fill, then each attribute row counts up 0 → now. */
    suspend fun playEntrance(
        domain: StatusDomain,
        motion: MotionSpec,
    ) = lock.withLock {
        displayedRank = domain.rank
        headerAlpha.snapTo(0f)
        xpBarAlpha.snapTo(0f)
        lifetime.snapTo(0f)
        levelFlash.snapTo(0f)
        rankFlash.snapTo(0f)
        attributes.forEach {
            attrReveal.getValue(it).snapTo(0f)
            attrValue.getValue(it).snapTo(0f)
            attrPulse.getValue(it).snapTo(1f)
        }

        headerAlpha.animateTo(1f, motion.tween(AscendMotionTokens.STANDARD, AscendEasing.settle))
        xpBarAlpha.animateTo(1f, motion.tween(AscendMotionTokens.QUICK))
        lifetime.animateTo(domain.lifetimeXp.toFloat(), motion.tween(AscendMotionTokens.DELIBERATE, AscendEasing.settle))

        coroutineScope {
            attributes.forEach { attr ->
                launch {
                    attrReveal.getValue(attr).animateTo(1f, motion.tween(AscendMotionTokens.QUICK))
                    attrValue.getValue(attr)
                        .animateTo(
                            (domain.attributes[attr] ?: 0L).toFloat(),
                            motion.tween(AscendMotionTokens.STANDARD, AscendEasing.settle),
                        )
                }
                delay(motion.stagger(1).toLong())
            }
        }
    }

    /** Play one earned batch as a chain: XP gain → attribute pulses → level/rank beats. */
    suspend fun playBatch(
        events: List<ProgressionEvent>,
        motion: MotionSpec,
    ) = lock.withLock {
        events.forEach { event ->
            when (event.type) {
                ProgressionEventType.XP_GAINED ->
                    lifetime.animateTo(event.toValue.toFloat(), motion.tween(AscendMotionTokens.DELIBERATE, AscendEasing.settle))

                ProgressionEventType.ATTRIBUTE_CHANGED -> {
                    val attr = event.attribute ?: return@forEach
                    val value = attrValue.getValue(attr)
                    value.snapTo(event.fromValue.toFloat())
                    coroutineScope {
                        launch { pulse(attr, motion) }
                        value.animateTo(event.toValue.toFloat(), motion.tween(AscendMotionTokens.STANDARD, AscendEasing.settle))
                    }
                }

                ProgressionEventType.LEVEL_UP -> flash(levelFlash, motion)

                ProgressionEventType.RANK_UP -> {
                    displayedRank = Rank.entries.getOrElse(event.toValue.toInt()) { displayedRank }
                    flash(rankFlash, motion)
                }
            }
        }
    }

    private suspend fun pulse(
        attribute: AttributeType,
        motion: MotionSpec,
    ) {
        val p = attrPulse.getValue(attribute)
        p.animateTo(PULSE_PEAK, motion.tween(AscendMotionTokens.QUICK, AscendEasing.emphasize))
        p.animateTo(1f, motion.tween(AscendMotionTokens.STANDARD, AscendEasing.pulse))
    }

    private suspend fun flash(
        anim: Animatable<Float, *>,
        motion: MotionSpec,
    ) {
        anim.snapTo(0f)
        anim.animateTo(1f, motion.tween(AscendMotionTokens.QUICK, AscendEasing.emphasize))
        anim.animateTo(0f, motion.tween(AscendMotionTokens.DRAMATIC, AscendEasing.pulse))
    }

    private companion object {
        const val PULSE_PEAK = 1.18f
    }
}
