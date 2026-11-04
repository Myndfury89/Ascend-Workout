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
import com.ascend.feature.dashboard.prototype.MedallionGlyph
import com.ascend.feature.dashboard.prototype.RankTier
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden settled frames of [ClassSigil] as the Status screen drives it: partial XP rings, an
 * emphasised attribute medallion and the host beat at rest, for each class in Idle and LevelUp.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE, qualifiers = "w400dp-h400dp-xhdpi")
class StatusClassSigilScreenshotTest(
    private val classKey: ClassKey,
    private val state: SigilState,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun statusReadouts() {
        val hasClass = classKey != ClassKey.NEUTRAL
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
                        host =
                            SigilHostLayers(
                                readouts =
                                    SigilReadouts(
                                        playerRing = 0.62f,
                                        classRing = if (hasClass) 0.35f else null,
                                        playerRingTrim = 1f,
                                        classRingTrim = 1f,
                                        activeMedallion = 2,
                                        medallionPulse = List(MedallionGlyph.attributeGlyphs.size) { 0f },
                                        showProficiency = hasClass,
                                        proficiencyPulse = 0f,
                                    ),
                                beat = SigilBeat(assembly = 1f, glow = 0f, wave = 0f),
                                semanticDescription = "status sigil",
                            ),
                    )
                }
            }
        }
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        compose.onRoot().captureRoboImage(
            "src/test/screenshots/status_class_sigil_${classKey.name.lowercase()}_${state.name.lowercase()}.png",
        )
    }

    companion object {
        private const val SETTLE_MS = 3_000L

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun params(): List<Array<Any>> =
            ClassKey.entries.flatMap { key ->
                listOf(SigilState.Idle, SigilState.LevelUp).map { arrayOf<Any>(key, it) }
            }
    }
}
