package com.ascend.core.designsystem.motion

import androidx.compose.ui.test.junit4.createComposeRule
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** PR 5b: the app-root provider resolves system + in-app reduced motion into [LocalMotionSpec]. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ProvideMotionSpecTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun appMotionSpec_isFullOnlyWhenNeitherInputAsksForReducedMotion() {
        assertEquals(MotionSpec.Full, appMotionSpec(ReducedMotionFlags(system = false, inApp = false)))
        assertEquals(MotionSpec.Reduced, appMotionSpec(ReducedMotionFlags(system = true, inApp = false)))
        assertEquals(MotionSpec.Reduced, appMotionSpec(ReducedMotionFlags(system = false, inApp = true)))
        assertEquals(MotionSpec.Reduced, appMotionSpec(ReducedMotionFlags(system = true, inApp = true)))
    }

    @Test
    fun defaultSettings_provideTheSameSpecAsTheUnprovidedDefault() {
        var unprovided: MotionSpec? = null
        var provided: MotionSpec? = null
        val flags = MutableStateFlow(ReducedMotionFlags(system = false, inApp = false))
        compose.setContent {
            unprovided = LocalMotionSpec.current
            ProvideMotionSpec(flags) { provided = LocalMotionSpec.current }
        }
        compose.waitForIdle()
        assertEquals(MotionSpec.Full, unprovided)
        assertEquals(unprovided, provided)
    }

    @Test
    fun systemAnimatorScaleZeroOrInAppToggle_reducesMotionLive() {
        var provided: MotionSpec? = null
        val flags = MutableStateFlow(ReducedMotionFlags(system = false, inApp = false))
        compose.setContent { ProvideMotionSpec(flags) { provided = LocalMotionSpec.current } }

        compose.waitForIdle()
        assertEquals(MotionSpec.Full, provided)

        flags.value = ReducedMotionFlags(system = true, inApp = false)
        compose.waitForIdle()
        assertEquals(MotionSpec.Reduced, provided)

        flags.value = ReducedMotionFlags(system = false, inApp = true)
        compose.waitForIdle()
        assertEquals(MotionSpec.Reduced, provided)

        flags.value = ReducedMotionFlags(system = false, inApp = false)
        compose.waitForIdle()
        assertEquals(MotionSpec.Full, provided)
    }
}
