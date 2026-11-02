package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.ascend.core.designsystem.theme.AscendTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden baselines (Roborazzi) of the current production [OrnateSigil], called with the same
 * flags as `StatusScreen`, for every class variant × {INITIATE, GOLD, MYTHIC} in the settled
 * state. Animation inputs are fixed values and the compose clock is paused, so every frame is
 * deterministic. Record: `./gradlew recordRoborazziDebug`; verify: `./gradlew verifyRoborazziDebug`.
 */
@RunWith(ParameterizedRobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE, qualifiers = "w400dp-h400dp-xhdpi")
class OrnateSigilBaselineScreenshotTest(
    private val variant: StatusClassVariant,
    private val rank: RankTier,
) {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun settled() {
        compose.mainClock.autoAdvance = false
        val hasClass = variant != StatusClassVariant.NEUTRAL
        val state =
            OrnateSigilState(
                rankTier = rank,
                variant = variant,
                playerRing = SETTLED_PLAYER_RING,
                classRing = if (hasClass) SETTLED_CLASS_RING else null,
                activeMedallion = 0,
                showProficiency = hasClass,
            )
        val animation =
            OrnateSigilAnimation(
                assembly = 1f,
                rotation = 0f,
                glow = 0f,
                playerRingTrim = 1f,
                classRingTrim = 1f,
                medallionPulse = List(MedallionGlyph.attributeGlyphs.size) { 0f },
                proficiencyPulse = 0f,
                wave = 0f,
            )
        compose.setContent {
            AscendTheme(darkTheme = true) {
                Box(Modifier.size(360.dp).background(Color.Black)) {
                    OrnateSigil(
                        state = state,
                        animation = animation,
                        semanticDescription = "sigil",
                        modifier = Modifier.size(360.dp),
                        frameMotion = false,
                        rotationProfile = SigilRotationProfile.CEREMONIAL_MIDDLE,
                        opacityProfile = SigilOpacityProfile.RING_FORWARD,
                        classGeometry = true,
                        internalGlow = true,
                        warmAccents = true,
                    )
                }
            }
        }
        compose.mainClock.advanceTimeByFrame()
        compose.onRoot().captureRoboImage(
            "src/test/screenshots/ornate_sigil_${variant.name.lowercase()}_${rank.name.lowercase()}_settled.png",
        )
    }

    companion object {
        private const val SETTLED_PLAYER_RING = 0.62f
        private const val SETTLED_CLASS_RING = 0.38f

        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "{0}_{1}")
        fun params(): List<Array<Any>> =
            StatusClassVariant.entries.flatMap { variant ->
                listOf(RankTier.INITIATE, RankTier.GOLD, RankTier.MYTHIC).map { arrayOf<Any>(variant, it) }
            }
    }
}
