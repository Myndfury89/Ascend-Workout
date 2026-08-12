package com.ascend.feature.ascended.prototype

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.feature.ascended.model.AscendedClass
import com.ascend.feature.ascended.model.BodyBase
import com.ascend.feature.ascended.model.EvolutionStage
import com.ascend.feature.ascended.presentation.AscendedFigure
import com.ascend.feature.ascended.prototype.controls.AscendedControls
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM render checks for the image-driven figure. With no artwork supplied, the figure degrades to a
 * placeholder that names the exact drawable to add; the controls report the reviewer's choices.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class AscendedFigureRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `a class without its own art falls back to the imported base body`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                AscendedFigure(AscendedClass.GUARDIAN, BodyBase.MALE, Modifier.size(280.dp, 460.dp))
            }
        }
        // The base body art (ascended_base_male) resolves, so the figure renders — not the placeholder.
        compose.onNodeWithContentDescription("Guardian Male figure").assertExists()
        compose.onNodeWithText("ascended_guardian_male.png", substring = true).assertDoesNotExist()
    }

    @Test
    fun `a class with its own art renders that figure`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                AscendedFigure(AscendedClass.BERSERKER, BodyBase.FEMALE, Modifier.size(280.dp, 460.dp))
            }
        }
        // ascended_berserker_female is supplied, so the class art resolves and renders.
        compose.onNodeWithContentDescription("Berserker Female figure").assertExists()
    }

    @Test
    fun `the controls report class, body base, stage, and Perception choices`() {
        var cls = AscendedClass.GUARDIAN
        var base = BodyBase.MALE
        var stage = EvolutionStage.BASE
        var level = 0
        compose.setContent {
            AscendTheme(darkTheme = true) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    AscendedControls(
                        ascendedClass = cls,
                        bodyBase = base,
                        stage = stage,
                        perceptionLevel = level,
                        onClass = { cls = it },
                        onBodyBase = { base = it },
                        onStage = { stage = it },
                        onPerception = { level = it },
                    )
                }
            }
        }
        compose.onNodeWithText("Berserker").performScrollTo().performClick()
        assertEquals(AscendedClass.BERSERKER, cls)
        compose.onNodeWithText("Female").performScrollTo().performClick()
        assertEquals(BodyBase.FEMALE, base)
        compose.onNodeWithText("Early").performScrollTo().performClick()
        assertEquals(EvolutionStage.EARLY_GROWTH, stage)
        compose.onNodeWithText("L10").performScrollTo().performClick()
        assertEquals(10, level)
    }
}
