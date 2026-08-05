package com.ascend.feature.onboarding

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.core.model.onboarding.PrimaryGoal
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** JVM render + accessibility smoke tests for the onboarding UI (no emulator, no animation clock). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class OnboardingRenderTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `the eligibility screen is neutral and non-shaming`() {
        compose.setContent { AscendTheme { AgeIneligibleScreen() } }
        compose.onNodeWithText("Not available yet").assertIsDisplayed()
        compose.onNodeWithText("Nothing has been saved.", substring = true).assertIsDisplayed()
    }

    @Test
    fun `the scaffold shows an accessible step indicator and advances on next`() {
        var advanced = false
        compose.setContent {
            AscendTheme {
                OnboardingScaffold(
                    title = "Your goals",
                    subtitle = null,
                    progressIndex = 3,
                    progressTotal = 10,
                    onBack = {},
                    onNext = { advanced = true },
                ) { OnboardingHint("hint") }
            }
        }
        compose.onNodeWithContentDescription("Step 3 of 10").assertIsDisplayed()
        compose.onNodeWithText("Your goals").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        assertTrue(advanced)
    }

    @Test
    fun `selection is exposed to accessibility as selected, not colour alone`() {
        compose.setContent {
            AscendTheme {
                SingleChoice(
                    options = PrimaryGoal.entries.toList(),
                    selected = PrimaryGoal.STRENGTH,
                    label = { it.displayName },
                    onSelect = {},
                )
            }
        }
        compose.onNodeWithContentDescription("Strength, selected").assertIsDisplayed()
        compose.onNodeWithContentDescription("Endurance, not selected").assertExists()
    }

    @Test
    fun `the stepper renders under large font scaling`() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale = 1.8f)) {
                AscendTheme {
                    OnboardingScaffold(
                        title = "Equipment & environment",
                        subtitle = "Optional",
                        progressIndex = 5,
                        progressTotal = 10,
                        onBack = {},
                        onNext = {},
                    ) { OnboardingHint("Your plan only suggests what you can do.") }
                }
            }
        }
        compose.onNodeWithText("Equipment & environment").assertIsDisplayed()
    }

    @Test
    fun `the centered message renders welcome-style content`() {
        compose.setContent {
            AscendTheme { CenteredMessage("Your Ascension Begins", "Begin your assessment.") { OnboardingHint("Reduced motion") } }
        }
        compose.onNodeWithText("Your Ascension Begins").assertIsDisplayed()
    }
}
