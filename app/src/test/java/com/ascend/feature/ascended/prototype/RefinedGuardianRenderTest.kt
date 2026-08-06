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
import com.ascend.feature.ascended.prototype.body.classShapeCount
import com.ascend.feature.ascended.prototype.controls.ClassReviewControls
import com.ascend.feature.ascended.prototype.model.AscendedClass
import com.ascend.feature.ascended.prototype.model.BodyBase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** JVM render checks (no emulator) for the CP3 Guardian refinement + review controls. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class RefinedGuardianRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the refined Guardian renders in normal, outline, and blockout-compare forms`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                Column {
                    ClassSilhouetteFigure(AscendedClass.GUARDIAN, BodyBase.MALE, Modifier.size(160.dp, 280.dp), SilhouetteFidelity.REFINED)
                    ClassSilhouetteFigure(
                        AscendedClass.GUARDIAN,
                        BodyBase.FEMALE,
                        Modifier.size(160.dp, 280.dp),
                        SilhouetteFidelity.REFINED,
                        outlineOnly = true,
                    )
                    ClassSilhouetteFigure(AscendedClass.GUARDIAN, BodyBase.MALE, Modifier.size(160.dp, 280.dp), SilhouetteFidelity.BLOCKOUT)
                }
            }
        }
        compose.onAllNodesWithContentDescription("Guardian class silhouette", substring = true).onFirst().assertExists()
    }

    @Test
    fun `the fidelity toggle and shape count are shown for a refined class`() {
        var fidelity = SilhouetteFidelity.BLOCKOUT
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
                    fidelity = fidelity,
                    compare = false,
                    outlineOnly = false,
                    shapeCount = classShapeCount(AscendedClass.GUARDIAN, BodyBase.MALE, SilhouetteFidelity.REFINED),
                    refinedAvailable = true,
                    onFidelity = { fidelity = it },
                    onCompare = {},
                    onOutline = {},
                )
            }
        }
        compose.onNodeWithText("FIDELITY", substring = true).assertExists()
        compose.onNodeWithText("Refined Base").performClick()
        assertEquals(SilhouetteFidelity.REFINED, fidelity)
    }

    @Test
    fun `the shape count helper reports the refined Guardian's developed shape set`() {
        val refined = classShapeCount(AscendedClass.GUARDIAN, BodyBase.MALE, SilhouetteFidelity.REFINED)
        val blockout = classShapeCount(AscendedClass.GUARDIAN, BodyBase.MALE, SilhouetteFidelity.BLOCKOUT)
        assertEquals(true, refined > blockout)
    }
}
