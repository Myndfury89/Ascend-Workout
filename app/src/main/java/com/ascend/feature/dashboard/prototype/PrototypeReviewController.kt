package com.ascend.feature.dashboard.prototype

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

/**
 * Holds every debug-only review knob and the looped-review state machine for the prototype.
 * Pure UI state — no production data, no repositories. Created with [rememberPrototypeReviewController].
 */
@Stable
class PrototypeReviewController {
    var stateId by mutableStateOf(StatusPrototypeStateId.STANDARD)
    var variant by mutableStateOf(StatusClassVariant.BERSERKER)
    var forceSecondary by mutableStateOf(false)
    var entranceMode by mutableStateOf(EntranceMode.MAJOR_EVENT)
    var motionSpeed by mutableStateOf(MotionSpeed.STANDARD)
    var reducedMotion by mutableStateOf(false)
    var effectsQuality by mutableStateOf(EffectsQuality.FULL)
    var deviceWidth by mutableStateOf(DeviceWidth.FILL)
    var textStress by mutableStateOf(false)
    var hierarchyOverlay by mutableStateOf(false)
    var diagnosticsOverlay by mutableStateOf(false)

    // Ornate sigil knobs (null override = follow the current state's data).
    var weightUnit by mutableStateOf(com.ascend.core.common.WeightUnit.KILOGRAMS)
    var rankTierOverride by mutableStateOf<RankTier?>(null)
    var playerRingOverride by mutableStateOf<Float?>(null)
    var classRingOverride by mutableStateOf<Float?>(null)
    var activeMedallionOverride by mutableStateOf<Int?>(null)
    var showProficiencyOverride by mutableStateOf<Boolean?>(null)
    var sigilOpacity by mutableStateOf(OrnateSigilState.DEFAULT_SETTLED_OPACITY)

    var loopRunning by mutableStateOf(false)
        private set
    var loopPaused by mutableStateOf(false)
        private set
    var loopIntervalMs by mutableIntStateOf(DEFAULT_LOOP_INTERVAL_MS)

    var replayKey by mutableIntStateOf(0)
        private set
    var everydayReplays by mutableIntStateOf(0)
        private set

    private val states = StatusPrototypeStateId.entries

    fun selectState(id: StatusPrototypeStateId) {
        stateId = id
        replay()
    }

    fun replay() {
        replayKey++
        if (entranceMode == EntranceMode.EVERYDAY_OPEN) everydayReplays++
    }

    fun nextState() = selectState(states[(states.indexOf(stateId) + 1) % states.size])

    fun previousState() = selectState(states[(states.indexOf(stateId) - 1 + states.size) % states.size])

    fun restartSequence() = selectState(states.first())

    fun startLoop() {
        loopRunning = true
        loopPaused = false
    }

    fun stopLoop() {
        loopRunning = false
        loopPaused = false
    }

    fun pauseLoop() {
        loopPaused = true
    }

    fun resumeLoop() {
        loopPaused = false
    }

    /** True while the loop should be advancing (running and not paused). */
    val loopActive: Boolean get() = loopRunning && !loopPaused

    companion object {
        const val DEFAULT_LOOP_INTERVAL_MS = 2600
    }
}

@Composable
fun rememberPrototypeReviewController(): PrototypeReviewController = remember { PrototypeReviewController() }
