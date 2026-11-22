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
import com.ascend.core.designsystem.sigil.MedallionGlyph
import com.ascend.core.designsystem.sigil.RankTier
import com.ascend.core.designsystem.sigil.SigilState
import com.ascend.core.designsystem.theme.AscendTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden settled frames of [ClassSigil] with the medallions at their resting pulse (1f), so all six
 * medallions — the five attributes plus the class proficiency glyph — actually render (the other
 * sigil goldens pass pulse 0, which draws medallions at radius 0). Every class in Idle and Locked.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE, qualifiers = "w400dp-h400dp-xhdpi")
class ProficiencyMedallionScreenshotTest(
    private val classKey: ClassKey,
    private val state: SigilState,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun medallionsAtRest() {
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
                                        classRing = 0.35f,
                                        playerRingTrim = 1f,
                                        classRingTrim = 1f,
                                        activeMedallion = -1,
                                        medallionPulse = List(MedallionGlyph.attributeGlyphs.size) { REST_PULSE },
                                        showProficiency = true,
                                        proficiencyPulse = REST_PULSE,
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
            "src/test/screenshots/proficiency_medallion_${classKey.name.lowercase()}_${state.name.lowercase()}.png",
        )
    }

    companion object {
        private const val SETTLE_MS = 3_000L
        private const val REST_PULSE = 1f

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun params(): List<Array<Any>> =
            ClassKey.entries.filter { it != ClassKey.NEUTRAL }.flatMap { key ->
                listOf(SigilState.Idle, SigilState.Locked).map { arrayOf<Any>(key, it) }
            }
    }
}
