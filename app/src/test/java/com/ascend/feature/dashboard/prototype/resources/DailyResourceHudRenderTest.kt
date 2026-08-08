package com.ascend.feature.dashboard.prototype.resources

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.ascend.core.designsystem.theme.AscendTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** JVM render checks for the HP/MP/XP HUD across the device-availability scenarios. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class DailyResourceHudRenderTest {
    @get:Rule
    val compose = createComposeRule()

    private fun render(scenario: DailyResourceScenario) {
        val i = DailyResourceFixtures.inputs(scenario)
        val hp = DailyHpResolver.resolve(i.movement)
        val mp = MpEvidenceResolver.resolve(i.evidence, DailyTrainingTargetResolver.resolve(i.targetInput))
        compose.setContent { AscendTheme(darkTheme = true) { DailyResourceHud(hp, mp, i.xp) } }
    }

    @Test
    fun `full state shows banded HP, MP and the existing XP`() {
        render(DailyResourceScenario.FULL)
        compose.onNodeWithText("6,842 / 8,000").assertExists()
        compose.onNodeWithText("Strong").assertExists() // 85% HP band
        compose.onNodeWithText("45 / 45").assertExists()
        compose.onNodeWithText("Full").assertExists() // 100% MP band
        compose.onNodeWithText("4,148 / 6,200").assertExists() // XP unchanged
    }

    @Test
    fun `sparse state shows movement unavailable but a full MP`() {
        render(DailyResourceScenario.SPARSE)
        compose.onNodeWithText("Movement data unavailable").assertExists()
        compose.onNodeWithText("Full").assertExists()
    }

    @Test
    fun `rest day shows a recovery state, not zero over zero`() {
        render(DailyResourceScenario.REST_DAY)
        compose.onNodeWithText("Recovery day").assertExists()
    }

    @Test
    fun `no-activity shows zero percent, distinct from unavailable`() {
        render(DailyResourceScenario.NO_ACTIVITY)
        compose.onNodeWithText("0 / 8,000").assertExists()
        compose.onAllNodesWithText("Low").onFirst().assertExists() // HP (and MP) both at 0% show Low
        // Not the unavailable text.
        compose.onNodeWithText("Movement data unavailable").assertDoesNotExist()
    }

    @Test
    fun `partial state renders the reduced MP`() {
        render(DailyResourceScenario.PARTIAL)
        compose.onNodeWithText("38 / 45").assertExists()
    }

    @Test
    fun `phone-only state renders a full MP without a wearable`() {
        render(DailyResourceScenario.PHONE_ONLY)
        compose.onNodeWithText("45 / 45").assertExists()
    }

    @Test
    fun `each resource exposes its meaning and value to accessibility`() {
        render(DailyResourceScenario.FULL)
        compose.onNodeWithContentDescription("HP Vitality", substring = true).assertExists()
        compose.onNodeWithContentDescription("MP Energy", substring = true).assertExists()
        compose.onNodeWithContentDescription("XP Growth", substring = true).assertExists()
    }
}
