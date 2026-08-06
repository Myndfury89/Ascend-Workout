package com.ascend.feature.ascended.prototype

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.feature.ascended.prototype.body.BaseMannequin
import com.ascend.feature.ascended.prototype.controls.AscendedReviewControls
import com.ascend.feature.ascended.prototype.model.BodyBase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM render checks (no emulator) for the static CP1 pieces — the base mannequins and the review
 * controls. The animated viewport is validated on-device (its infinite transitions never idle for a
 * headless test), mirroring how the Status prototype tests render static sub-composables.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class YourAscendedRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the male base mannequin renders front-facing`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                BaseMannequin(BodyBase.MALE, Modifier.size(280.dp, 500.dp))
            }
        }
        compose.onNodeWithContentDescription("Male base mannequin", substring = true).assertExists()
        compose.onNodeWithContentDescription("front-facing", substring = true).assertExists()
    }

    @Test
    fun `the female base mannequin renders front-facing`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                BaseMannequin(BodyBase.FEMALE, Modifier.size(280.dp, 500.dp))
            }
        }
        compose.onNodeWithContentDescription("Female base mannequin", substring = true).assertExists()
    }

    @Test
    fun `the mannequin is a single decorative node with no text (excluded from a11y clutter)`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                BaseMannequin(BodyBase.MALE, Modifier.size(280.dp, 500.dp), seams = true)
            }
        }

        fun collect(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::collect)
        val all = collect(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        // The body contributes exactly one content description and zero text nodes.
        val textNodes = all.count { it.config.getOrNull(SemanticsProperties.Text) != null }
        assertEquals("the mannequin must draw no text/pseudo-script", 0, textNodes)
        compose.onNodeWithContentDescription("Male base mannequin", substring = true).assertExists()
    }

    @Test
    fun `both small and large portrait widths render the mannequin`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                Column {
                    BaseMannequin(BodyBase.MALE, Modifier.width(300.dp).size(300.dp, 520.dp))
                    BaseMannequin(BodyBase.FEMALE, Modifier.width(460.dp).size(460.dp, 800.dp))
                }
            }
        }
        compose.onNodeWithContentDescription("Male base mannequin", substring = true).assertExists()
    }

    @Test
    fun `the base toggle reports the chosen body base`() {
        var chosen: BodyBase? = null
        compose.setContent {
            AscendTheme(darkTheme = true) {
                AscendedReviewControls(
                    bodyBase = BodyBase.MALE,
                    reducedMotion = false,
                    seams = false,
                    onBodyBase = { chosen = it },
                    onReducedMotion = {},
                    onSeams = {},
                )
            }
        }
        compose.onNodeWithText("FEMALE").performClick()
        assertEquals(BodyBase.FEMALE, chosen)
    }

    @Test
    fun `a large font scale keeps the controls readable`() {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 1.6f)) {
                AscendTheme(darkTheme = true) {
                    AscendedReviewControls(
                        bodyBase = BodyBase.MALE,
                        reducedMotion = false,
                        seams = false,
                        onBodyBase = {},
                        onReducedMotion = {},
                        onSeams = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Reduced motion", substring = true).assertExists()
    }
}
