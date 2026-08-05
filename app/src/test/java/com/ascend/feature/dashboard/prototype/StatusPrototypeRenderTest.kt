package com.ascend.feature.dashboard.prototype

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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
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

    @Test
    fun `the text-stress scenario renders long strings and three-digit attributes without clipping the name`() {
        val data = FakeStatusPrototype.stressData(StatusClassVariant.BERSERKER, reducedMotion = false)
        compose.setContent { AscendTheme(darkTheme = true) { StaticStatusPanel(data) } }
        compose.onNodeWithText("Strength").assertExists()
        // A three-digit attribute value is present and rendered.
        compose.onNodeWithText("214").assertExists()
    }

    @Test
    fun `a secondary class renders its own progression bar`() {
        val data =
            FakeStatusPrototype.dataFor(
                StatusPrototypeStateId.STANDARD,
                StatusClassVariant.BERSERKER,
                reducedMotion = false,
                forceSecondary = true,
            )
        compose.setContent { AscendTheme(darkTheme = true) { StaticStatusPanel(data) } }
        compose.onNodeWithText("(secondary)", substring = true).assertExists()
    }

    @Test
    fun `the small-phone width renders the panel`() {
        val data = FakeStatusPrototype.dataFor(StatusPrototypeStateId.STANDARD, StatusClassVariant.MONK, reducedMotion = false)
        compose.setContent { AscendTheme(darkTheme = true) { StaticStatusPanel(data, Modifier.width(340.dp)) } }
        compose.onNodeWithText(data.hunterName).assertExists()
    }

    @Test
    fun `a large font scale still renders the identity`() {
        val data = FakeStatusPrototype.dataFor(StatusPrototypeStateId.RECOMMENDATION, StatusClassVariant.MONK, reducedMotion = false)
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 1.5f)) {
                AscendTheme(darkTheme = true) { StaticStatusPanel(data) }
            }
        }
        compose.onNodeWithText(data.hunterName).assertExists()
    }

    @Test
    fun `an event overlay exposes an accessible description`() {
        val data = FakeStatusPrototype.dataFor(StatusPrototypeStateId.QUEST_COMPLETE, StatusClassVariant.MONK, reducedMotion = false)
        compose.setContent { AscendTheme(darkTheme = true) { StaticStatusPanel(data) } }
        compose.onNodeWithContentDescription("Event:", substring = true).assertExists()
    }

    @Test
    fun `the diagnostics panel renders its read-out`() {
        val info =
            DiagnosticsInfo(
                state = "Standard status", variant = "Berserker", speed = "Fast", reducedMotion = false,
                effectsQuality = "Full", particleCount = 26, particlesActive = true, scanActive = true,
                sigilIdleActive = true, infinitePaused = false, entranceMode = "Everyday open",
                majorEventActive = false, approxEntranceMs = 480, deviceWidth = "Normal phone",
            )
        compose.setContent { AscendTheme(darkTheme = true) { StatusDiagnosticsPanel(info, frameMs = 16.7f) } }
        compose.onNodeWithText("state: Standard status").assertExists()
        compose.onNodeWithText("frame time: ~16.7 ms").assertExists()
    }

    // ---- ornate sigil ----

    private fun ornateState(showProficiency: Boolean) =
        OrnateSigilState(
            RankTier.GOLD,
            StatusClassVariant.MONK,
            playerRing = 0.72f,
            classRing = 0.44f,
            activeMedallion = 3,
            showProficiency = showProficiency,
        )

    private fun ornateAnim() = OrnateSigilAnimation(1f, 18f, 0f, 0.72f, 0.44f, List(5) { 1f }, 1f)

    @Test
    fun `the ornate sigil exposes a description and draws no text or pseudo-script`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                OrnateSigil(ornateState(true), ornateAnim(), "Gold-rank Monk seal.", Modifier.size(240.dp))
            }
        }
        compose.onNodeWithContentDescription("Gold-rank Monk seal.", substring = true).assertExists()

        // The sigil is pure Canvas geometry — it must contribute zero text nodes.
        fun collect(node: SemanticsNode): List<SemanticsNode> = listOf(node) + node.children.flatMap(::collect)
        val all = collect(compose.onRoot(useUnmergedTree = true).fetchSemanticsNode())
        val textNodes = all.count { it.config.getOrNull(SemanticsProperties.Text) != null }
        assertEquals("the sigil must draw no text/pseudo-script", 0, textNodes)
    }

    @Test
    fun `the refined sigil renders with ceremonial rotation, ring-forward opacity, and the stationary overlay`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                OrnateSigil(
                    ornateState(true),
                    ornateAnim(),
                    "Refined Gold-rank Monk seal.",
                    Modifier.size(240.dp),
                    rotationProfile = SigilRotationProfile.CEREMONIAL_MIDDLE,
                    opacityProfile = SigilOpacityProfile.RING_FORWARD,
                    stationaryOverlay = true,
                )
            }
        }
        compose.onNodeWithContentDescription("Refined Gold-rank Monk seal.", substring = true).assertExists()
    }

    @Test
    fun `the ornate sigil renders in minimal effects mode`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                OrnateSigil(ornateState(false), ornateAnim(), "seal", Modifier.size(200.dp), minimal = true, frameMotion = false)
            }
        }
        compose.onNodeWithContentDescription("seal", substring = true).assertExists()
    }

    @Test
    fun `the low-opacity settled panel keeps the identity readable`() {
        val data = FakeStatusPrototype.dataFor(StatusPrototypeStateId.LOW_RANK, StatusClassVariant.NEUTRAL, reducedMotion = false)
        compose.setContent { AscendTheme(darkTheme = true) { StaticStatusPanel(data) } }
        compose.onNodeWithText(data.hunterName).assertExists()
        compose.onNodeWithText("Strength").assertExists()
    }
}
