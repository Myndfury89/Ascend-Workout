package com.ascend.feature.dashboard.prototype

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.ascend.core.designsystem.theme.AscendTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * JVM smoke tests (no emulator) that the revised prototype composes for every state/class and
 * that the fake-data model is internally consistent. Renders the static panel to avoid relying
 * on the entrance animation clock.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class StatusPrototypeRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the standard panel renders identity and attributes`() {
        val data = FakeStatusPrototype.dataFor(StatusPrototypeStateId.STANDARD, StatusClassVariant.BERSERKER, reducedMotion = false)
        compose.setContent { AscendTheme(darkTheme = true) { StaticStatusPanel(data) } }
        compose.onNodeWithText(data.hunterName).assertExists()
        compose.onNodeWithText("Strength").assertExists()
    }

    @Test
    fun `every state and class variant builds valid fake data`() {
        for (variant in StatusClassVariant.entries) {
            for (state in StatusPrototypeStateId.entries) {
                val data = FakeStatusPrototype.dataFor(state, variant, reducedMotion = false)
                assertEquals(5, data.attributes.size)
                assertTrue(data.playerXpFraction in 0f..1f)
                assertTrue(data.classXpFraction in 0f..1f)
                assertTrue(data.questFraction in 0f..1f)
            }
        }
    }

    @Test
    fun `the recommendation state renders its pending recommendation`() {
        val data = FakeStatusPrototype.dataFor(StatusPrototypeStateId.RECOMMENDATION, StatusClassVariant.BERSERKER, reducedMotion = false)
        compose.setContent { AscendTheme(darkTheme = true) { StaticStatusPanel(data) } }
        compose.onNodeWithText("Recommendation").assertExists()
    }

    @Test
    fun `reduced-motion state forces reduced motion`() {
        val data = FakeStatusPrototype.dataFor(StatusPrototypeStateId.REDUCED_MOTION, StatusClassVariant.MONK, reducedMotion = false)
        assertEquals(true, data.reducedMotion)
    }

    @Test
    fun `quest completion state reaches its target`() {
        val data = FakeStatusPrototype.dataFor(StatusPrototypeStateId.QUEST_COMPLETE, StatusClassVariant.MAGICIAN, reducedMotion = false)
        assertEquals(1f, data.questFraction, 1e-6f)
    }
}
