package com.ascend.feature.dashboard.sigil

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.ascend.core.designsystem.motion.MotionSpec
import com.ascend.core.designsystem.sigil.ClassKey
import com.ascend.core.designsystem.sigil.SigilState
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.feature.dashboard.prototype.RankTier
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden settled frames of [ClassSigil] for every class × state at GOLD rank. Rendered with reduced
 * motion (no drift; verbs cross-fade to their end frame) and a paused clock advanced past every verb,
 * so each image is the deterministic settled frame.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE, qualifiers = "w400dp-h400dp-xhdpi")
class ClassSigilScreenshotTest(
    private val classKey: ClassKey,
    private val state: SigilState,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun settled() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AscendTheme(darkTheme = true) {
                Box(Modifier.size(360.dp).background(Color.Black)) {
                    ClassSigil(
                        classKey = classKey,
                        state = state,
                        rank = RankTier.GOLD,
                        modifier = Modifier.size(360.dp),
                        motion = MotionSpec.Reduced,
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        compose.onRoot().captureRoboImage(
            "src/test/screenshots/class_sigil_${classKey.name.lowercase()}_${state.name.lowercase()}.png",
        )
    }

    companion object {
        private const val SETTLE_MS = 3_000L

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun params(): List<Array<Any>> =
            ClassKey.entries.flatMap { key ->
                SigilState.entries.map { arrayOf<Any>(key, it) }
            }
    }
}
