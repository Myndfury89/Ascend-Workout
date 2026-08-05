package com.ascend.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.ascend.core.common.HeightUnits
import com.ascend.core.common.WeightUnit
import com.ascend.core.common.WeightUnits
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.core.model.MeasurementSystem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Render + interaction tests for the unit-aware height/weight pickers. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class OnboardingPickerRenderTest {
    @get:Rule
    val compose = createComposeRule()

    // ---- weight ----

    @Test
    fun `weight field accessibility label reflects the selected unit`() {
        compose.setContent {
            AscendTheme { WeightPickerField(canonicalKg = 112.5, unit = WeightUnit.KILOGRAMS, onCommit = {}) }
        }
        compose.onNodeWithContentDescription("Weight, 112.5 kilograms").assertIsDisplayed()
    }

    @Test
    fun `weight field in pounds describes the value in pounds`() {
        compose.setContent {
            AscendTheme { WeightPickerField(canonicalKg = WeightUnits.lbToKg(248.0), unit = WeightUnit.POUNDS, onCommit = {}) }
        }
        compose.onNodeWithContentDescription("Weight, 248 pounds").assertIsDisplayed()
    }

    @Test
    fun `tapping the weight field opens the picker centred on the current value`() {
        compose.setContent {
            AscendTheme { WeightPickerField(canonicalKg = 100.0, unit = WeightUnit.KILOGRAMS, onCommit = {}) }
        }
        compose.onNodeWithContentDescription("Weight, 100 kilograms").performClick()
        compose.onNodeWithText("Weight").assertIsDisplayed()
        // The entry initialises at the current value, so the picker opens near it.
        compose.onNodeWithTag("weightPickerEntry").assertTextContains("100")
    }

    @Test
    fun `partial weight input is not silently rewritten`() {
        compose.setContent {
            AscendTheme { WeightPickerField(canonicalKg = 100.0, unit = WeightUnit.KILOGRAMS, onCommit = {}) }
        }
        compose.onNodeWithContentDescription("Weight, 100 kilograms").performClick()
        compose.onNodeWithTag("weightPickerEntry").performTextClearance()
        compose.onNodeWithTag("weightPickerEntry").performTextInput("11")
        // Still exactly "11" — never reformatted to "11.0" or a nearest option while typing.
        compose.onNodeWithTag("weightPickerEntry").assertTextContains("11")
    }

    @Test
    fun `typed weight jumps to and commits that value as canonical kilograms`() {
        var committed: Double? = null
        compose.setContent {
            AscendTheme { WeightPickerField(canonicalKg = 100.0, unit = WeightUnit.KILOGRAMS, onCommit = { committed = it }) }
        }
        compose.onNodeWithContentDescription("Weight, 100 kilograms").performClick()
        compose.onNodeWithTag("weightPickerEntry").performTextClearance()
        compose.onNodeWithTag("weightPickerEntry").performTextInput("82.5")
        compose.onNodeWithText("Confirm").performClick()
        assertEquals(82.5, committed!!, 1e-6)
    }

    @Test
    fun `typed pounds commit canonical kilograms`() {
        var committed: Double? = null
        compose.setContent {
            AscendTheme {
                WeightPickerField(
                    canonicalKg = WeightUnits.lbToKg(200.0),
                    unit = WeightUnit.POUNDS,
                    onCommit = { committed = it },
                )
            }
        }
        compose.onNodeWithContentDescription("Weight, 200 pounds").performClick()
        compose.onNodeWithTag("weightPickerEntry").performTextClearance()
        compose.onNodeWithTag("weightPickerEntry").performTextInput("180")
        compose.onNodeWithText("Confirm").performClick()
        assertEquals(WeightUnits.lbToKg(180.0), committed!!, 1e-6)
    }

    @Test
    fun `an out-of-range weight receives neutral validation and does not commit`() {
        var committed: Double? = 999.0
        var called = false
        compose.setContent {
            AscendTheme {
                WeightPickerField(canonicalKg = 100.0, unit = WeightUnit.KILOGRAMS, onCommit = {
                    called = true
                    committed = it
                })
            }
        }
        compose.onNodeWithContentDescription("Weight, 100 kilograms").performClick()
        compose.onNodeWithTag("weightPickerEntry").performTextClearance()
        compose.onNodeWithTag("weightPickerEntry").performTextInput("5")
        compose.onNodeWithText("Confirm").performClick()
        compose.onNodeWithText("Enter a weight between", substring = true).assertIsDisplayed()
        assertTrue("must not commit an out-of-range value", !called)
    }

    @Test
    fun `clearing the weight commits null`() {
        var committed: Double? = 100.0
        var called = false
        compose.setContent {
            AscendTheme {
                WeightPickerField(canonicalKg = 100.0, unit = WeightUnit.KILOGRAMS, onCommit = {
                    called = true
                    committed = it
                })
            }
        }
        compose.onNodeWithContentDescription("Weight, 100 kilograms").performClick()
        compose.onNodeWithText("Clear").performClick()
        assertTrue(called)
        assertNull(committed)
    }

    // ---- height ----

    @Test
    fun `imperial height field describes feet and inches`() {
        compose.setContent {
            AscendTheme { HeightPickerField(canonicalCm = 187.96, system = MeasurementSystem.IMPERIAL, onCommit = {}) }
        }
        compose.onNodeWithContentDescription("Height, 6 feet 2 inches").assertIsDisplayed()
    }

    @Test
    fun `imperial height picker shows the combined reading and commits canonical centimetres`() {
        var committed: Double? = null
        compose.setContent {
            AscendTheme { HeightPickerField(canonicalCm = 187.96, system = MeasurementSystem.IMPERIAL, onCommit = { committed = it }) }
        }
        compose.onNodeWithContentDescription("Height, 6 feet 2 inches").performClick()
        compose.onNodeWithContentDescription("Selected height, 6 feet 2 inches").assertIsDisplayed()
        compose.onNodeWithText("Confirm").performClick()
        assertEquals(HeightUnits.feetInchesToCm(6, 2), committed!!, 1e-6)
    }

    @Test
    fun `metric height field describes centimetres and commits canonical centimetres`() {
        var committed: Double? = null
        compose.setContent {
            AscendTheme { HeightPickerField(canonicalCm = 188.0, system = MeasurementSystem.METRIC, onCommit = { committed = it }) }
        }
        compose.onNodeWithContentDescription("Height, 188 centimetres").performClick()
        compose.onNodeWithTag("heightPickerEntry").assertTextContains("188")
        compose.onNodeWithTag("heightPickerEntry").performTextClearance()
        compose.onNodeWithTag("heightPickerEntry").performTextInput("175")
        compose.onNodeWithText("Confirm").performClick()
        assertEquals(175.0, committed!!, 1e-6)
    }
}
