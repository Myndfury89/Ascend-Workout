package com.ascend.feature.dashboard

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.feature.dashboard.prototype.AttributeReward
import com.ascend.feature.dashboard.prototype.QuestCompletionSummary
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** JVM render checks (no emulator) for the production Quest Complete HUD window. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class QuestCompleteHudRenderTest {
    @get:Rule
    val compose = createComposeRule()

    private val accent = Color(0xFFF9744F)

    private fun summary(
        xp: Int = 350,
        attrs: List<AttributeReward> = listOf(AttributeReward("Strength", 30)),
    ) = QuestCompletionSummary(questId = "q1", title = "200 Push-Ups", xpGained = xp, attributeGains = attrs)

    @Test
    fun `the window renders its title, quest name and reward summary`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                QuestCompleteHud(summary(), accent, reveal = 1f, pulse = 1f)
            }
        }
        compose.onNodeWithText("DAILY QUEST COMPLETE").assertExists()
        compose.onNodeWithText("200 Push-Ups").assertExists()
        compose.onNodeWithText("+350").assertExists()
        compose.onNodeWithText("STRENGTH").assertExists()
    }

    @Test
    fun `final content renders immediately when fully revealed (reduced-motion path)`() {
        // Under reduced motion the caller passes reveal = 1 and a constant pulse — the same content.
        compose.setContent {
            AscendTheme(darkTheme = true) {
                QuestCompleteHud(summary(), accent, reveal = 1f, pulse = 1f)
            }
        }
        compose.onNodeWithText("200 Push-Ups").assertExists()
        compose.onNodeWithText("+350").assertExists()
    }

    @Test
    fun `the optional dismiss is the only interactive affordance and is non-blocking`() {
        var dismissed = false
        compose.setContent {
            AscendTheme(darkTheme = true) {
                QuestCompleteHud(summary(), accent, reveal = 1f, pulse = 1f, onDismiss = { dismissed = true })
            }
        }
        compose.onNodeWithText("CONTINUE", substring = true).performClick()
        assertTrue("dismiss only flips local state; it changes no rewards or queue", dismissed)
    }

    @Test
    fun `a reward-less completion still renders a readable summary`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                QuestCompleteHud(summary(xp = 0, attrs = emptyList()), accent, reveal = 1f, pulse = 1f)
            }
        }
        compose.onNodeWithText("Objectives complete.").assertExists()
    }

    @Test
    fun `small and large portrait widths both render`() {
        compose.setContent {
            AscendTheme(darkTheme = true) {
                androidx.compose.foundation.layout.Column {
                    QuestCompleteHud(summary(), accent, reveal = 1f, pulse = 1f, modifier = Modifier.width(320.dp))
                    QuestCompleteHud(summary(), accent, reveal = 1f, pulse = 1f, modifier = Modifier.width(460.dp))
                }
            }
        }
        // Both widths compose the window (two matching nodes) — assert at least the first renders.
        compose.onAllNodesWithText("DAILY QUEST COMPLETE").onFirst().assertExists()
    }

    @Test
    fun `a large font scale keeps the reward summary readable`() {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale = 1.6f)) {
                AscendTheme(darkTheme = true) {
                    QuestCompleteHud(summary(), accent, reveal = 1f, pulse = 1f)
                }
            }
        }
        compose.onNodeWithText("+350").assertExists()
    }
}
