package com.ascend.feature.dashboard.prototype

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import com.ascend.core.designsystem.theme.AscendTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** CP4: each Skill has a distinct unlock reveal personality, all resolving into the same menu. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class SkillUnlockTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the four skills each have a distinct reveal personality`() {
        val styles = PrototypeSkill.entries.map { it.revealStyle }
        assertEquals("all four reveal styles are distinct", styles.size, styles.toSet().size)
        assertEquals(SkillRevealStyle.SCAN, PrototypeSkill.PERCEPTION.revealStyle)
        assertEquals(SkillRevealStyle.COMPRESS_SNAP, PrototypeSkill.STRENGTH_BOOST.revealStyle)
        assertEquals(SkillRevealStyle.BREATH_CYCLES, PrototypeSkill.BREATH_CONTROL.revealStyle)
        assertEquals(SkillRevealStyle.POINTS_ALIGN, PrototypeSkill.BODY_AWARENESS.revealStyle)
    }

    @Test
    fun `every skill carries menu content — effect, unlock condition, and affinity`() {
        PrototypeSkill.entries.forEach {
            assertTrue("${it.name} effect", it.effect.isNotBlank())
            assertTrue("${it.name} unlock condition", it.unlockCondition.isNotBlank())
            assertTrue("${it.name} affinity", it.affinity.isNotBlank())
        }
    }

    @Test
    fun `the unlock menu renders each skill's identity on the shared HUD panel`() {
        val accent = StatusSigilVariant.of(StatusClassVariant.MAGICIAN).core
        compose.setContent {
            AscendTheme(darkTheme = true) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    PrototypeSkill.entries.forEach { skill ->
                        HudReviewWindow(HudWindowKind.SKILL_UNLOCK, accent, reveal = 1f, pulse = 0.5f, skill = skill)
                    }
                }
            }
        }
        // Shared menu chrome + each skill's own identity + unlock evidence.
        compose.onAllNodesWithText("SKILL UNLOCKED").onFirst().assertExists()
        PrototypeSkill.entries.forEach { skill ->
            compose.onAllNodesWithText(skill.displayName).onFirst().assertExists()
        }
        compose.onAllNodesWithText("15 min sustained cardio").onFirst().assertExists()
        compose.onAllNodesWithText("CONTINUE", substring = true).onFirst().assertExists()
    }
}
