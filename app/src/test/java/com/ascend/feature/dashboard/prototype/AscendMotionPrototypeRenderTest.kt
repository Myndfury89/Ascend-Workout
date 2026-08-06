package com.ascend.feature.dashboard.prototype

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import com.ascend.core.designsystem.theme.AscendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Confirms the corrected motion prototype renders the HTML-faithful review composition — sigil-first
 * viewport, compact identity, attribute chips, Skill dock, timing/audio labels, and review controls —
 * rather than the production Status information panel. The animation clock is paused so the ambient
 * infinite transitions never block the test.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class AscendMotionPrototypeRenderTest {
    @get:Rule
    val compose = createComposeRule()

    private fun render() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            AscendTheme(darkTheme = true) {
                AscendMotionPrototypeScreen(
                    controller =
                        PrototypeReviewController().apply {
                            reducedMotion = true
                            sigilRefined = true
                        },
                )
            }
        }
    }

    @Test
    fun `the review viewport shows the ASCEND header, the dominant sigil, and class identity`() {
        render()
        compose.onAllNodesWithText("ASCEND").onFirst().assertExists()
        compose.onAllNodesWithContentDescription("ceremonial sigil", substring = true).onFirst().assertExists()
        compose.onAllNodesWithText("Berserker").onFirst().assertExists()
    }

    @Test
    fun `the viewport shows compact attribute chips and the Skill dock`() {
        render()
        compose.onAllNodesWithText("SKILL DOCK").onFirst().assertExists()
        compose.onAllNodesWithContentDescription("Skill Perception", substring = true).onFirst().assertExists()
        // Attribute chips carry accessible name+value descriptions (not the production meter rows).
        compose.onAllNodesWithContentDescription("Strength", substring = true).onFirst().assertExists()
    }

    @Test
    fun `the review controls expose framing, chained replay, and timing labels`() {
        render()
        compose.onAllNodesWithText("Framed").onFirst().assertExists()
        compose.onAllNodesWithText("Chained replay").onFirst().assertExists()
        // The timing label reflects the selected verb.
        compose.onAllNodesWithText("LOCK", substring = true).onFirst().assertExists()
    }
}
