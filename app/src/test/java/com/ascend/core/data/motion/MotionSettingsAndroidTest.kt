package com.ascend.core.data.motion

import android.content.Context
import android.os.Looper
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class MotionSettingsAndroidTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `system setting reads animator duration scale and reacts to changes`() =
        runTest {
            Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
            AndroidSystemMotionSettings(context).observeSystemReducedMotion().test {
                assertFalse(awaitItem())
                Settings.Global.putFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 0f)
                shadowOf(Looper.getMainLooper()).idle()
                assertTrue(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `in-app toggle is persisted across instances`() =
        runTest {
            assertEquals(false, DataStoreMotionPreferences(context).observeInAppReducedMotion().first())
            DataStoreMotionPreferences(context).setInAppReducedMotion(true)
            assertEquals(true, DataStoreMotionPreferences(context).observeInAppReducedMotion().first())
            DataStoreMotionPreferences(context).setInAppReducedMotion(false)
        }
}
