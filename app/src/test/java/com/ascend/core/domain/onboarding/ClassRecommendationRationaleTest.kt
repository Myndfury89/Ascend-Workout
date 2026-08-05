package com.ascend.core.domain.onboarding

import com.ascend.core.domain.classes.ClassCatalog
import com.ascend.core.model.onboarding.ActivityPreference
import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.BodyMetrics
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.Limitation
import com.ascend.core.model.onboarding.OptionalSex
import com.ascend.core.model.onboarding.PhysiologyProfile
import com.ascend.core.model.onboarding.PrimaryGoal
import com.ascend.core.model.onboarding.TrainingEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The rationale is derived from goals/preferences/environment only — never protected traits. */
class ClassRecommendationRationaleTest {
    private fun assessment(
        goal: PrimaryGoal = PrimaryGoal.STRENGTH,
        secondary: List<PrimaryGoal> = emptyList(),
        prefs: Set<ActivityPreference> = emptySet(),
        env: TrainingEnvironment? = null,
        sex: OptionalSex = OptionalSex.NOT_SET,
        age: AgeSafetyCategory = AgeSafetyCategory.ADULT,
        limitations: Set<Limitation> = emptySet(),
        body: BodyMetrics = BodyMetrics(),
    ) = InitialAssessment(
        userId = "u1",
        primaryGoal = goal,
        secondaryGoals = secondary,
        activityPreferences = prefs,
        environment = env,
        physiology = PhysiologyProfile(sex = sex),
        bodyMetrics = body,
        limitations = limitations,
        ageSafetyCategory = age,
    )

    @Test
    fun `berserker rationale cites the strength goal and gym choice`() {
        val text =
            ClassRecommendationRationale.build(
                assessment(
                    goal = PrimaryGoal.STRENGTH,
                    secondary = listOf(PrimaryGoal.MUSCLE_GAIN),
                    prefs = setOf(ActivityPreference.WEIGHTLIFTING),
                    env = TrainingEnvironment.GYM,
                ),
                ClassCatalog.BERSERKER,
            )
        assertTrue(text.startsWith("Recommended because you"))
        assertTrue(text.contains("strength"))
        assertTrue(text.contains("muscle gain"))
        assertTrue(text.contains("gym-based training"))
    }

    @Test
    fun `magician rationale cites endurance and sustained cardio preferences`() {
        val text =
            ClassRecommendationRationale.build(
                assessment(goal = PrimaryGoal.ENDURANCE, prefs = setOf(ActivityPreference.RUNNING, ActivityPreference.CYCLING)),
                ClassCatalog.MAGICIAN,
            )
        assertTrue(text.contains("endurance"))
        assertTrue(text.contains("prefer"))
    }

    @Test
    fun `rationale never mentions protected or physiological traits`() {
        val text =
            ClassRecommendationRationale.build(
                assessment(
                    goal = PrimaryGoal.STRENGTH,
                    secondary = listOf(PrimaryGoal.MUSCLE_GAIN),
                    env = TrainingEnvironment.GYM,
                    sex = OptionalSex.FEMALE,
                    age = AgeSafetyCategory.MINOR_YOUNGER,
                    limitations = setOf(Limitation.LOWER_BODY, Limitation.PREGNANCY_POSTPARTUM),
                    body = BodyMetrics(heightCm = 150.0, currentWeightKg = 95.0),
                ),
                ClassCatalog.BERSERKER,
            )
        // Injected trait values/tokens must not surface (substrings that also occur in activity names
        // like "weightlifting" are intentionally excluded — this checks physiology leakage, not spelling).
        listOf("female", "minor", "95", "150", "kilogram", "pound", "pregnan", "injur").forEach {
            assertFalse("rationale must not mention '$it'", text.lowercase().contains(it))
        }
    }

    @Test
    fun `protected and physiological traits do not change the rationale text`() {
        val base =
            ClassRecommendationRationale.build(
                assessment(goal = PrimaryGoal.STRENGTH, prefs = setOf(ActivityPreference.WEIGHTLIFTING)),
                ClassCatalog.BERSERKER,
            )
        val varied =
            ClassRecommendationRationale.build(
                assessment(
                    goal = PrimaryGoal.STRENGTH,
                    prefs = setOf(ActivityPreference.WEIGHTLIFTING),
                    sex = OptionalSex.MALE,
                    age = AgeSafetyCategory.MINOR_OLDER,
                    limitations = setOf(Limitation.UPPER_BODY),
                    body = BodyMetrics(currentWeightKg = 130.0),
                ),
                ClassCatalog.BERSERKER,
            )
        assertEquals(base, varied)
    }

    @Test
    fun `an empty assessment yields a neutral non-locking rationale`() {
        val text = ClassRecommendationRationale.build(assessment(goal = PrimaryGoal.MAINTENANCE), ClassCatalog.BERSERKER)
        assertTrue(text.contains("balanced starting path"))
    }
}
