package com.ascend.feature.ascended.prototype

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.feature.ascended.prototype.body.ClassSilhouetteFigure
import com.ascend.feature.ascended.prototype.body.SilhouetteFidelity
import com.ascend.feature.ascended.prototype.controls.ClassReviewControls
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
import com.ascend.feature.ascended.prototype.model.EvolutionStage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** JVM render checks for the CP3 evolution-stage + Perception review. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class ProgressionRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the figure renders at the base and mastered stages with the Perception manifestation`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                Column {
                    ClassSilhouetteFigure(
                        AscendedClass.GUARDIAN,
                        BodyBase.MALE,
                        Modifier.size(160.dp, 280.dp),
                        SilhouetteFidelity.REFINED,
                        stage = EvolutionStage.BASE,
                    )
                    ClassSilhouetteFigure(
                        AscendedClass.GUARDIAN,
                        BodyBase.MALE,
                        Modifier.size(160.dp, 280.dp),
                        SilhouetteFidelity.REFINED,
                        stage = EvolutionStage.MASTERED,
                        perceptionLevel = 10,
                    )
                }
            }
        }
        compose.onAllNodesWithContentDescription("Guardian class silhouette", substring = true).onFirst().assertExists()
    }

    @Test
    fun `the stage and Perception selectors report their choices`() {
        var stage = EvolutionStage.MASTERED
        var level = 0
        compose.setContent {
            AscendTheme(darkTheme = true) {
                ClassReviewControls(
                    selectedClass = AscendedClass.GUARDIAN,
                    bodyBase = BodyBase.MALE,
                    silhouetteOnly = false,
                    showLayers = false,
                    reducedMotion = false,
                    onSelectClass = {},
                    onBodyBase = {},
                    onSilhouetteOnly = {},
                    onShowLayers = {},
                    onReducedMotion = {},
                    refinedAvailable = true,
                    stage = stage,
                    perceptionLevel = level,
                    onStage = { stage = it },
                    onPerception = { level = it },
                )
            }
        }
        compose.onNodeWithText("Early").performClick()
        assertEquals(EvolutionStage.EARLY_GROWTH, stage)
        compose.onNodeWithText("L10").performClick()
        assertEquals(10, level)
    }
}
