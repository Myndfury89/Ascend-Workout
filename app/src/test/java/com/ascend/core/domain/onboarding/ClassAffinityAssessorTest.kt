package com.ascend.core.domain.onboarding

import com.ascend.core.domain.classes.ClassCatalog
import com.ascend.core.domain.classes.ClassRecommendationEngine
import com.ascend.core.model.onboarding.ActivityDomain
import com.ascend.core.model.onboarding.ActivityPreference
import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.BodyMetrics
import com.ascend.core.model.onboarding.ExperienceLevel
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.Limitation
import com.ascend.core.model.onboarding.OptionalSex
import com.ascend.core.model.onboarding.PhysiologyProfile
import com.ascend.core.model.onboarding.PrimaryGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Class affinity considers only goals/preferences/experience — never protected/physiological traits. */
class ClassAffinityAssessorTest {
    private val assessor = ClassAffinityAssessor(ClassRecommendationEngine())
    private val definitions = ClassCatalog.ALL

    private fun assessment(
        goal: PrimaryGoal,
        prefs: Set<ActivityPreference> = emptySet(),
        experience: Map<ActivityDomain, ExperienceLevel> = emptyMap(),
        sex: OptionalSex = OptionalSex.NOT_SET,
        age: AgeSafetyCategory = AgeSafetyCategory.ADULT,
        limitations: Set<Limitation> = emptySet(),
        body: BodyMetrics = BodyMetrics(),
    ) = InitialAssessment(
        userId = "u1",
        primaryGoal = goal,
        activityPreferences = prefs,
        activityExperience = experience,
        physiology = PhysiologyProfile(sex = sex),
        bodyMetrics = body,
        limitations = limitations,
        ageSafetyCategory = age,
    )

    @Test
    fun `strength goal and weightlifting favor the berserker`() {
        val r = assessor.assess(assessment(PrimaryGoal.STRENGTH, setOf(ActivityPreference.WEIGHTLIFTING)), definitions)
        assertEquals("berserker", r.recommendedClassId)
    }

    @Test
    fun `bodyweight and mobility favor the monk`() {
        val r =
            assessor.assess(
                assessment(PrimaryGoal.MOBILITY, setOf(ActivityPreference.BODYWEIGHT, ActivityPreference.MOBILITY)),
                definitions,
            )
        assertEquals("monk", r.recommendedClassId)
    }

    @Test
    fun `endurance running favors the magician`() {
        val r =
            assessor.assess(
                assessment(PrimaryGoal.ENDURANCE, setOf(ActivityPreference.RUNNING, ActivityPreference.CYCLING)),
                definitions,
            )
        assertEquals("magician", r.recommendedClassId)
    }

    @Test
    fun `recommendation is explainable and scores every class`() {
        val r = assessor.assess(assessment(PrimaryGoal.STRENGTH, setOf(ActivityPreference.WEIGHTLIFTING)), definitions)
        assertNotNull(r.recommendedClassId)
        assertTrue("rationale present", r.rationale.isNotBlank())
        assertTrue("evidence keys present", r.evidenceKeys.isNotEmpty())
        assertEquals("scores all classes", definitions.size, r.classScores.size)
        assertTrue("evidence is goal/preference based", r.evidenceKeys.any { it.startsWith("goal:") || it.startsWith("prefers:") })
    }

    @Test
    fun `protected and physiological traits do not change the recommendation`() {
        val base = assessment(PrimaryGoal.STRENGTH, setOf(ActivityPreference.WEIGHTLIFTING))
        val varied =
            assessment(
                PrimaryGoal.STRENGTH,
                setOf(ActivityPreference.WEIGHTLIFTING),
                sex = OptionalSex.FEMALE,
                age = AgeSafetyCategory.MINOR_YOUNGER,
                limitations = setOf(Limitation.LOWER_BODY, Limitation.PREGNANCY_POSTPARTUM),
                body = BodyMetrics(heightCm = 150.0, currentWeightKg = 95.0),
            )
        val a = assessor.assess(base, definitions)
        val b = assessor.assess(varied, definitions)
        assertEquals("sex/age/weight/limitations must not change the class", a.recommendedClassId, b.recommendedClassId)
        assertEquals("scores must be identical", a.classScores, b.classScores)
    }
}
