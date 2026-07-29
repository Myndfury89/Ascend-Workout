package com.ascend.feature.workouts

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ascend.core.common.WeightUnit
import com.ascend.core.designsystem.component.WeightUnitSelector
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.core.model.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class WeightUnitRenderTest {
    @get:Rule
    val compose = createComposeRule()

    private fun set() =
        WorkoutSet(
            id = "s1", workoutId = "w1", exerciseId = "ex1", exerciseName = "Bench", primaryAttribute = null,
            orderIndex = 0, reps = 5, weight = 100.0, durationSeconds = null, distance = null, volume = 5.0, unit = "reps",
            completedAt = 0,
        )

    @Test
    fun `logged workout weight renders in the selected display unit`() {
        val state = LogWorkoutUiState(sets = listOf(set()))
        compose.setContent {
            AscendTheme(darkTheme = true) {
                LogWorkoutContent(
                    state = state, onBack = {}, onTitleChange = {}, onDifficultyChange = {},
                    onAddSet = { _, _, _ -> }, onDeleteSet = {}, onFinish = {}, onDismissCompletion = {},
                    weightUnit = WeightUnit.POUNDS,
                )
            }
        }
        // 100 kg canonical -> displayed in pounds.
        compose.onNodeWithText("220.5 lb", substring = true).assertExists()
    }

    @Test
    fun `the weight-unit selector exposes an accessible label and toggles`() {
        val selected = mutableStateOf(WeightUnit.KILOGRAMS)
        compose.setContent {
            AscendTheme(darkTheme = true) {
                WeightUnitSelector(selected = selected.value, onSelect = { selected.value = it })
                Text(selected.value.name)
            }
        }
        compose.onNodeWithContentDescription("Weight unit, kilograms selected").assertExists()
        compose.onNodeWithText("lb").performClick()
        assertEquals(WeightUnit.POUNDS, selected.value)
    }
}
