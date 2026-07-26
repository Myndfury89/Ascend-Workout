package com.ascend.feature.quests

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.core.domain.progression.SetSuggestionEngine
import com.ascend.core.domain.progression.SetSuggestionInput
import com.ascend.core.model.AttributeType
import com.ascend.core.model.Difficulty
import com.ascend.core.model.ObjectiveType
import com.ascend.core.model.ProgressSource
import com.ascend.core.model.Quest
import com.ascend.core.model.QuestObjective
import com.ascend.core.model.QuestProgressEntry
import com.ascend.core.model.QuestStatus
import com.ascend.core.model.QuestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class ActiveQuestScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private fun state(): ActiveQuestUiState {
        val objective = QuestObjective(
            id = "o1", questId = "q1", exerciseId = null, title = "Push-ups",
            type = ObjectiveType.REPETITIONS, target = 200.0, current = 75.0, unit = "reps",
            preferredSetSize = 25, minimumSetSize = 10, maximumSetSize = 50,
            primaryAttribute = AttributeType.STRENGTH, status = QuestStatus.IN_PROGRESS, orderIndex = 0,
            entries = listOf(
                QuestProgressEntry("e1", "o1", 25.0, ProgressSource.MANUAL, null, 1),
                QuestProgressEntry("e2", "o1", 50.0, ProgressSource.MANUAL, null, 2),
            ),
        )
        val quest = Quest(
            id = "q1", userId = "u", title = "Upper-Body Trial", description = null,
            type = QuestType.ACCUMULATION, status = QuestStatus.IN_PROGRESS, scheduledDate = null,
            deadline = null, difficulty = Difficulty.MODERATE, baseRewardXp = 350,
            partialRewardEnabled = true, overCompletionEnabled = true, objectives = listOf(objective),
        )
        val suggestion = SetSuggestionEngine().suggest(
            SetSuggestionInput(remaining = 125, preferredSetSize = 25, minimumSetSize = 10, maximumSetSize = 50),
        )
        return ActiveQuestUiState(isLoading = false, quest = quest, suggestion = suggestion)
    }

    @Test
    fun `renders progress and quick-add fires with the tapped amount`() {
        var added = -1
        compose.setContent {
            AscendTheme {
                ActiveQuestContent(
                    state = state(), onBack = {}, onAdd = { added = it },
                    onDelete = {}, onComplete = {}, onDismissCompletion = {},
                )
            }
        }

        compose.onNodeWithText("75 / 200 reps").assertExists()
        compose.onNodeWithText("Remaining", substring = true).assertExists()
        compose.onNodeWithText("+25").assertIsEnabled().performClick()
        assertEquals(25, added)
    }

    @Test
    fun `complete button invokes completion`() {
        var completed = false
        compose.setContent {
            AscendTheme {
                ActiveQuestContent(
                    state = state(), onBack = {}, onAdd = {},
                    onDelete = {}, onComplete = { completed = true }, onDismissCompletion = {},
                )
            }
        }
        compose.onNodeWithText("Complete Quest").performScrollTo().performClick()
        assertTrue(completed)
    }

    @Test
    fun `deleting a set invokes delete with the entry id`() {
        var deletedId: String? = null
        compose.setContent {
            AscendTheme {
                ActiveQuestContent(
                    state = state(), onBack = {}, onAdd = {},
                    onDelete = { deletedId = it }, onComplete = {}, onDismissCompletion = {},
                )
            }
        }
        compose.onAllNodesWithContentDescription("Delete set")[0].performClick()
        assertEquals("e1", deletedId)
    }
}
