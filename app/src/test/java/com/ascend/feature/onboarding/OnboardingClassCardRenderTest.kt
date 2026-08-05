package com.ascend.feature.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.ascend.core.designsystem.theme.AscendTheme
import com.ascend.core.domain.classes.ClassCatalog
import com.ascend.core.domain.classes.ClassRecommendationEngine
import com.ascend.core.domain.onboarding.ClassAffinityAssessor
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ClassDefinition
import com.ascend.core.model.ClassPresentation
import com.ascend.core.model.onboarding.ActivityPreference
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.PrimaryGoal
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Render tests for the rich class-selection cards + the "Paths Yet to Awaken" preview. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], manifest = Config.NONE)
class OnboardingClassCardRenderTest {
    @get:Rule
    val compose = createComposeRule()

    private val assessor = ClassAffinityAssessor(ClassRecommendationEngine())
    private val strengthAssessment =
        InitialAssessment(
            userId = "u1",
            primaryGoal = PrimaryGoal.STRENGTH,
            secondaryGoals = listOf(PrimaryGoal.MUSCLE_GAIN),
            activityPreferences = setOf(ActivityPreference.WEIGHTLIFTING),
        )

    private fun content(
        selected: String?,
        definitions: List<ClassDefinition> = ClassCatalog.ALL,
        onSelect: (String) -> Unit = {},
    ) {
        val affinity = assessor.assess(strengthAssessment, ClassCatalog.ALL)
        compose.setContent {
            AscendTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    ClassAffinityStep(
                        draft = OnboardingDraft(selectedClassId = selected),
                        affinity = affinity,
                        definitions = definitions,
                        onSelectClass = onSelect,
                    )
                }
            }
        }
    }

    @Test
    fun `a class card shows summary, styles, proficiency, and the priorities-not-access footer`() {
        content(selected = null)
        compose.onNodeWithText("Berserker").assertIsDisplayed()
        compose.onNodeWithText("Trains heavy strength, muscle growth, and explosive power.").assertExists()
        compose.onNodeWithText("Force", substring = true).assertExists()
        // The priorities-not-access footer appears on every implemented card.
        compose.onAllNodesWithText(
            "You can still train cardio, mobility, and techniques from other paths.",
            substring = true,
        ).onFirst().assertExists()
    }

    @Test
    fun `the recommended card is marked and explains why from the answers`() {
        content(selected = null)
        compose.onNodeWithText("Recommended").assertExists()
        compose.onNodeWithText("Why this was recommended").assertExists()
        compose.onNodeWithText("Recommended because you", substring = true).assertExists()
    }

    @Test
    fun `any implemented class can be selected manually`() {
        val picks = mutableListOf<String>()
        content(selected = null, onSelect = { picks += it })
        compose.onNodeWithContentDescription("Monk class", substring = true).performScrollTo().performClick()
        assertTrue("monk selectable by tap", picks.contains("monk"))
    }

    @Test
    fun `selection is exposed to accessibility as selected, not colour alone`() {
        content(selected = "monk")
        compose.onNodeWithContentDescription("Monk class, selected", substring = true).assertExists()
    }

    @Test
    fun `the preview section is shown and clearly unavailable`() {
        content(selected = null)
        compose.onNodeWithText("Paths Yet to Awaken").assertExists()
        compose.onNodeWithText("Assassin").assertExists()
        compose.onAllNodesWithText("Coming later").onFirst().assertExists()
        compose.onNodeWithContentDescription("Assassin, coming later, not selectable").assertExists()
    }

    @Test
    fun `tapping a preview class never selects it`() {
        val picks = mutableListOf<String>()
        content(selected = null, onSelect = { picks += it })
        compose.onNodeWithContentDescription("Assassin, coming later, not selectable").performClick()
        assertFalse("a preview must never become a selection", picks.contains("assassin"))
    }

    @Test
    fun `a newly seeded class renders through the same cards with no id conditionals`() {
        val synthetic =
            ClassDefinition(
                id = "tidewalker",
                name = "Tidewalker",
                classTitle = "Rider of Currents",
                description = "A test class proving cards render from data alone.",
                fitnessIdentity = "Aquatic endurance",
                favoredWorkoutCategories = listOf("Swimming", "Rowing"),
                favoredTags = setOf(com.ascend.core.model.ActivityTags.AQUATIC, com.ascend.core.model.ActivityTags.STEADY_STATE_CARDIO),
                primaryAttributes = listOf(AttributeType.ENDURANCE),
                secondaryAttributes = listOf(AttributeType.RECOVERY),
                attributeMultipliers = mapOf(AttributeType.ENDURANCE to 1.4),
                uniqueProficiencyKey = "FLOW",
                uniqueProficiencyName = "Flow",
                favoredClassXpMultiplier = 1.3,
                neutralClassXpMultiplier = 0.75,
                presentation = ClassPresentation("tidewalker", "flowing", "aqua", "flow", "shimmer", "soft"),
            )
        content(selected = null, definitions = ClassCatalog.ALL + synthetic)
        // The unknown class renders with its data-driven summary and proficiency, no code change needed.
        compose.onNodeWithText("Tidewalker").assertExists()
        compose.onNodeWithText("Flow", substring = true).assertExists()
    }
}
