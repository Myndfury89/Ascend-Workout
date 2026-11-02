package com.ascend.core.designsystem.motion

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

/** Reads the device-wide "Animator duration scale" developer/accessibility setting. */
interface SystemMotionSettings {
    /** Emits true while the system animator duration scale is 0 (animations off), and on every change. */
    fun observeSystemReducedMotion(): Flow<Boolean>
}

/** Persisted in-app reduced-motion preference. */
interface MotionPreferences {
    fun observeInAppReducedMotion(): Flow<Boolean>

    suspend fun setInAppReducedMotion(value: Boolean)
}

/** Android's default animator duration scale when the setting has never been changed. */
const val DEFAULT_ANIMATOR_DURATION_SCALE = 1f

/** A scale of 0 means the user turned animations off system-wide; treat that as reduced motion. */
fun isReducedMotionScale(scale: Float): Boolean = scale <= 0f

/** Reduced motion applies when either the system setting or the in-app toggle asks for it. */
fun effectiveReducedMotion(
    system: Boolean,
    inApp: Boolean,
): Boolean = system || inApp

/** Both reduced-motion inputs at once. */
data class ReducedMotionFlags(
    val system: Boolean,
    val inApp: Boolean,
) {
    val effective: Boolean get() = effectiveReducedMotion(system = system, inApp = inApp)
}

/** Single entry point for screens: combines the system setting with the persisted in-app toggle. */
class ReducedMotionController
    @Inject
    constructor(
        private val systemSettings: SystemMotionSettings,
        private val preferences: MotionPreferences,
    ) {
        fun observe(): Flow<ReducedMotionFlags> =
            combine(
                systemSettings.observeSystemReducedMotion(),
                preferences.observeInAppReducedMotion(),
            ) { system, inApp -> ReducedMotionFlags(system = system, inApp = inApp) }

        suspend fun setInAppReducedMotion(value: Boolean) = preferences.setInAppReducedMotion(value)
    }
