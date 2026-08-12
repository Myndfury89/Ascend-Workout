package com.ascend.feature.ascended

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.feature.ascended.model.AscendedClass
import com.ascend.feature.ascended.model.BodyBase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** JVM render checks for the production Your Ascended surface (stateless content). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class YourAscendedContentRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `first open with no body base shows the chooser`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                YourAscendedContent(
                    state = YourAscendedUiState(loading = false, needsBodyBaseChoice = true),
                    onChooseBodyBase = {},
                )
            }
        }
        compose.onNodeWithText("Choose your base form.").assertIsDisplayed()
        compose.onNodeWithText("Male").assertIsDisplayed()
        compose.onNodeWithText("Female").assertIsDisplayed()
        compose.onNodeWithText("This only changes your Ascended's visual form. You can change it later.").assertIsDisplayed()
    }

    @Test
    fun `choosing a base form reports the selection`() {
        var chosen: BodyBase? = null
        compose.setContent {
            AscendTheme(darkTheme = true) {
                YourAscendedContent(
                    state = YourAscendedUiState(loading = false, needsBodyBaseChoice = true),
                    onChooseBodyBase = { chosen = it },
                )
            }
        }
        compose.onNodeWithText("Female").performClick()
        assertEquals(BodyBase.FEMALE, chosen)
    }

    @Test
    fun `a bound class renders its figure with a change-base affordance`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                YourAscendedContent(
                    state =
                        YourAscendedUiState(
                            loading = false,
                            figureClass = AscendedClass.BERSERKER,
                            bodyBase = BodyBase.MALE,
                            needsBodyBaseChoice = false,
                        ),
                    onChooseBodyBase = {},
                )
            }
        }
        compose.onNodeWithText("Berserker").assertIsDisplayed()
        compose.onNodeWithContentDescription("Berserker Male figure").assertExists()
        compose.onNodeWithText("Change base form").assertExists()
    }

    @Test
    fun `an unbound player renders the neutral base figure`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                YourAscendedContent(
                    state =
                        YourAscendedUiState(
                            loading = false,
                            figureClass = null,
                            bodyBase = BodyBase.FEMALE,
                            needsBodyBaseChoice = false,
                        ),
                    onChooseBodyBase = {},
                )
            }
        }
        compose.onNodeWithText("Unbound").assertIsDisplayed()
        compose.onNodeWithContentDescription("Base Female figure").assertExists()
    }

    @Test
    fun `reduced motion is presentation-only and still renders the figure`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                YourAscendedContent(
                    state =
                        YourAscendedUiState(
                            loading = false,
                            figureClass = AscendedClass.MAGE,
                            bodyBase = BodyBase.MALE,
                            needsBodyBaseChoice = false,
                            reducedMotion = true,
                        ),
                    onChooseBodyBase = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Mage Male figure").assertExists()
    }
}
