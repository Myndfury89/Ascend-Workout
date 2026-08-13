package com.ascend.feature.build

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.core.domain.build.BuildCharacteristic
import com.ascend.core.domain.build.BuildClass
import com.ascend.core.domain.build.BuildTrend
import com.ascend.core.domain.build.CharacteristicScore
import com.ascend.core.domain.build.ClassAffinity
import com.ascend.core.domain.build.EvidenceState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class BuildIdentityContentRenderTest {
    @get:Rule
    val compose = createComposeRule()

    private fun state(): BuildIdentityUiState =
        BuildIdentityUiState(
            loading = false,
            characteristics =
                listOf(
                    CharacteristicScore(BuildCharacteristic.STRENGTH, 82.0, EvidenceState.OK, BuildTrend.DEVELOPING, 0.9),
                    CharacteristicScore(BuildCharacteristic.DISTANCE, 0.0, EvidenceState.INSUFFICIENT, BuildTrend.UNKNOWN, 0.2),
                    CharacteristicScore(BuildCharacteristic.RECOVERY, 0.0, EvidenceState.UNAVAILABLE, BuildTrend.UNKNOWN, 0.0),
                ),
            affinities =
                listOf(
                    ClassAffinity(BuildClass.BERSERKER, affinity = 0.78, coverage = 0.8, confidence = 0.7, dominantEligible = true),
                    ClassAffinity(BuildClass.RANGER, affinity = 0.66, coverage = 0.2, confidence = 0.1, dominantEligible = false),
                ),
            dominant = ClassAffinity(BuildClass.BERSERKER, 0.78, 0.8, 0.7, dominantEligible = true),
            currentClass = ClassAffinity(BuildClass.MONK, 0.55, 0.6, 0.5, dominantEligible = true),
            currentClassName = "Monk",
        )

    @Test
    fun `renders characteristics, the truthful ranking, and the current class beside it`() {
        compose.setContent { AscendTheme(darkTheme = true) { BuildIdentityContent(state()) } }

        compose.onNodeWithText("What your recent training resembles").assertExists()
        compose.onNodeWithText("Strength").assertExists()
        compose.onNodeWithText("Berserker").assertExists()
        // Current class is shown beside the ranking, not anchored inside it.
        compose.onNodeWithText("Monk · Affinity 55%").assertExists()
    }

    @Test
    fun `a low-coverage class shows its evidence state, not a confident percentage`() {
        compose.setContent { AscendTheme(darkTheme = true) { BuildIdentityContent(state()) } }

        // Ranger (coverage 0.2) is not dominant-eligible: the state outranks the score.
        compose.onNodeWithText("Insufficient evidence").assertExists()
        compose.onNodeWithText("Potential · Yet to Awaken").assertExists()
    }

    @Test
    fun `an unavailable characteristic reads as not tracked, never as zero`() {
        compose.setContent { AscendTheme(darkTheme = true) { BuildIdentityContent(state()) } }
        compose.onNodeWithText("Not tracked yet").assertExists()
    }
}
