package com.ascend.feature.ascended.prototype

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
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
import com.ascend.feature.ascended.prototype.body.ClassSilhouetteFigure
import com.ascend.feature.ascended.prototype.controls.ClassReviewControls
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** JVM render checks (no emulator) for the class silhouette figures + the CP2 review controls. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class ClassSilhouetteRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `every class silhouette renders with an accessible description`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                Column {
                    AscendedClass.entries.forEach { cls ->
                        ClassSilhouetteFigure(cls, BodyBase.MALE, Modifier.size(120.dp, 210.dp))
                    }
                }
            }
        }
        AscendedClass.entries.forEach { cls ->
            compose.onNodeWithContentDescription("${cls.displayName} class silhouette", substring = true).assertExists()
        }
    }

    @Test
    fun `silhouette-only and shape-layer modes both render`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                Column {
                    ClassSilhouetteFigure(AscendedClass.GUARDIAN, BodyBase.FEMALE, Modifier.size(160.dp, 280.dp), silhouetteOnly = true)
                    ClassSilhouetteFigure(AscendedClass.RANGER, BodyBase.MALE, Modifier.size(160.dp, 280.dp), showLayers = true)
                }
            }
        }
        compose.onNodeWithContentDescription("Guardian class silhouette", substring = true).assertExists()
        compose.onNodeWithContentDescription("Ranger class silhouette", substring = true).assertExists()
    }

    @Test
    fun `a class figure draws no text nodes (no faces or labels)`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                ClassSilhouetteFigure(AscendedClass.MAGICIAN, BodyBase.MALE, Modifier.size(200.dp, 340.dp))
            }
        }

        fun collect(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::collect)
        val all = collect(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        assertEquals(0, all.count { it.config.getOrNull(SemanticsProperties.Text) != null })
    }

    @Test
    fun `the class selector reports the chosen class and the base option`() {
        var chosen: AscendedClass? = AscendedClass.MAGICIAN
        compose.setContent {
            AscendTheme(darkTheme = true) {
                ClassReviewControls(
                    selectedClass = chosen,
                    bodyBase = BodyBase.MALE,
                    silhouetteOnly = false,
                    showLayers = false,
                    reducedMotion = false,
                    onSelectClass = { chosen = it },
                    onBodyBase = {},
                    onSilhouetteOnly = {},
                    onShowLayers = {},
                    onReducedMotion = {},
                )
            }
        }
        compose.onNodeWithText("Guardian").performClick()
        assertEquals(AscendedClass.GUARDIAN, chosen)
        compose.onNodeWithText("Mannequin").performClick()
        assertTrue("Mannequin selects the anatomical base (null class)", chosen == null)
    }

    @Test
    fun `the review controls stay readable at a large font scale`() {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 1.5f)) {
                AscendTheme(darkTheme = true) {
                    ClassReviewControls(
                        selectedClass = AscendedClass.BERSERKER,
                        bodyBase = BodyBase.FEMALE,
                        silhouetteOnly = true,
                        showLayers = false,
                        reducedMotion = false,
                        onSelectClass = {},
                        onBodyBase = {},
                        onSilhouetteOnly = {},
                        onShowLayers = {},
                        onReducedMotion = {},
                    )
                }
            }
        }
        compose.onNodeWithText("Silhouette only", substring = true).assertExists()
    }
}
