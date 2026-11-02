package com.ascend.core.designsystem.sigil

import com.ascend.core.designsystem.motion.MotionVerb

/**
 * What a class sigil is doing. Each state names the motion verb that drives its entry and whether
 * it keeps an ambient loop running once the verb settles.
 */
enum class SigilState(
    val verb: MotionVerb?,
    val loops: Boolean,
) {
    /** At rest: ambient drift only. */
    Idle(verb = null, loops = true),

    /** Picked/confirmed: snaps in (Lock), then drifts like Idle. */
    Selected(verb = MotionVerb.LOCK, loops = true),

    /** Energy gathering before an event (Charge); glow holds until the state changes. */
    Charging(verb = MotionVerb.CHARGE, loops = true),

    /** Rank/level gained (Ascend): the new-rank layer is emphasised and a wave radiates out. */
    LevelUp(verb = MotionVerb.ASCEND, loops = true),

    /** First view / class pick: geometry assembles from nothing (Assemble). */
    Reveal(verb = MotionVerb.ASSEMBLE, loops = true),

    /** A class the player has not unlocked yet: static, dimmed and desaturated, no motion. */
    Locked(verb = null, loops = false),
}

/**
 * The renderer targets for one state. [entry] is where the verb starts and [settled] is the end
 * frame; under reduced motion the sigil cross-fades straight to [settled] and never loops.
 */
data class SigilFrame(
    val assembly: Float,
    val glow: Float,
    val wave: Float,
    val alpha: Float,
    val saturation: Float,
    val newRankLayer: Boolean,
)

/** Peak glow held while Charging (before the renderer's own cap). */
const val CHARGE_GLOW = 0.6f

/** Alpha and saturation of a Locked (not yet unlocked) sigil. */
const val LOCKED_ALPHA = 0.5f
const val LOCKED_SATURATION = 0f

private val Rest = SigilFrame(assembly = 1f, glow = 0f, wave = 0f, alpha = 1f, saturation = 1f, newRankLayer = false)

/** The end frame each state settles to. */
fun SigilState.settledFrame(): SigilFrame =
    when (this) {
        SigilState.Idle, SigilState.Selected, SigilState.Reveal -> Rest
        SigilState.Charging -> Rest.copy(glow = CHARGE_GLOW)
        SigilState.LevelUp -> Rest.copy(newRankLayer = true)
        SigilState.Locked -> Rest.copy(alpha = LOCKED_ALPHA, saturation = LOCKED_SATURATION)
    }

/** The frame each state's verb starts from (equal to [settledFrame] for states without a verb). */
fun SigilState.entryFrame(): SigilFrame =
    when (this) {
        SigilState.Idle, SigilState.Locked, SigilState.Charging -> settledFrame()
        SigilState.Selected -> Rest.copy(glow = 1f)
        SigilState.LevelUp -> Rest.copy(glow = 1f, newRankLayer = true)
        SigilState.Reveal -> Rest.copy(assembly = 0f)
    }

/** Whether ambient drift runs: only looping states, and never under reduced motion. */
fun SigilState.drifts(reducedMotion: Boolean): Boolean = loops && !reducedMotion
