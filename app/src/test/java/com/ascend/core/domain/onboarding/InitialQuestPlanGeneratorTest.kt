package com.ascend.core.domain.onboarding

import com.ascend.core.model.AttributeType
import com.ascend.core.model.ObjectiveType
import com.ascend.core.model.QuestTemplate
import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.Availability
import com.ascend.core.model.onboarding.DifficultyBand
import com.ascend.core.model.onboarding.Equipment
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.Limitation
import com.ascend.core.model.onboarding.PrimaryGoal
import com.ascend.core.model.onboarding.SessionDuration
import com.ascend.core.model.onboarding.TrainingDaysPerWeek
import com.ascend.core.model.onboarding.TrainingEnvironment
import com.ascend.core.model.onboarding.TrainingFrequency
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The provisional, conservative initial-plan generator: filtering + safe targets, awards nothing. */
class InitialQuestPlanGeneratorTest {
    private val generator = InitialQuestPlanGenerator()

    private fun template(
        id: String,
        min: Int,
        max: Int,
        default: Int,
        step: Int,
        prefSet: Int,
        minSet: Int,
        warn: Int,
        unit: String = "reps",
    ) = QuestTemplate(
        id = id,
        name = id.removePrefix("tmpl-").replaceFirstChar { it.uppercase() },
        objectiveType = ObjectiveType.REPETITIONS,
        unit = unit,
        primaryAttribute = AttributeType.STRENGTH,
        exerciseId = null,
        minimumTarget = min,
        maximumTarget = max,
        defaultTarget = default,
        targetStep = step,
        defaultQuickAddValues = listOf(5, 10),
        defaultPreferredSetSize = prefSet,
        minimumAllowedSetSize = minSet,
        maximumAllowedSetSize = max,
        supportsAutomaticProgress = false,
        supportsManualProgress = true,
        supportedVariations = emptyList(),
        safetyWarningThreshold = warn,
        baseRewardXp = 100,
    )

    private fun candidate(id: String) =
        QuestPlanCandidate(
            template =
                when (id) {
                    "tmpl-pushups" -> template(id, 25, 500, 100, 5, 25, 10, 300)
                    "tmpl-pullups" -> template(id, 10, 50, 20, 1, 8, 1, 40)
                    "tmpl-squats" -> template(id, 10, 200, 30, 5, 20, 10, 150)
                    "tmpl-crunches" -> template(id, 25, 200, 75, 5, 25, 10, 150)
                    else -> template(id, 6000, 10000, 8000, 500, 0, 0, 12000, unit = "steps")
                },
            profile = QuestActivityProfileCatalog.forTemplate(id)!!,
        )

    private fun assessment(
        frequency: TrainingFrequency = TrainingFrequency.THREE_TO_FOUR_WEEKLY,
        equipment: Set<Equipment> = emptySet(),
        environment: TrainingEnvironment = TrainingEnvironment.HOME,
        availability: Availability = Availability(trainingDays = TrainingDaysPerWeek.THREE),
        limitations: Set<Limitation> = emptySet(),
        age: AgeSafetyCategory = AgeSafetyCategory.ADULT,
        goal: PrimaryGoal = PrimaryGoal.GENERAL_HEALTH,
    ) = InitialAssessment(
        userId = "u1",
        primaryGoal = goal,
        trainingFrequency = frequency,
        equipment = equipment,
        environment = environment,
        availability = availability,
        limitations = limitations,
        ageSafetyCategory = age,
    )

    @Test
    fun `pull-ups are excluded without a pull-up bar and included with one`() {
        val without = generator.generate(assessment(), listOf(candidate("tmpl-pullups")))
        assertTrue("no bar, no substitute -> excluded", without.questDefinitions.none { it.templateId == "tmpl-pullups" })

        val with = generator.generate(assessment(equipment = setOf(Equipment.PULL_UP_BAR)), listOf(candidate("tmpl-pullups")))
        assertTrue("bar present -> included", with.questDefinitions.any { it.templateId == "tmpl-pullups" })
    }

    @Test
    fun `environment filtering excludes an incompatible activity`() {
        // A synthetic gym-only, pool-required candidate must be dropped for an outdoors-only user.
        val poolProfile =
            QuestActivityProfile(
                templateId = "tmpl-poolswim",
                domain = com.ascend.core.model.onboarding.ActivityDomain.SWIMMING,
                tags = emptySet(),
                requiredEquipment = setOf(Equipment.POOL),
                allowedEnvironments = setOf(TrainingEnvironment.GYM),
                limitationConflicts = emptySet(),
                substitutable = false,
            )
        val poolCandidate = QuestPlanCandidate(template("tmpl-poolswim", 5, 30, 10, 1, 0, 0, 40), poolProfile)
        val plan =
            generator.generate(
                assessment(environment = TrainingEnvironment.OUTDOORS, equipment = emptySet()),
                listOf(poolCandidate, candidate("tmpl-steps")),
            )
        assertTrue("pool swim excluded outdoors", plan.questDefinitions.none { it.templateId == "tmpl-poolswim" })
        assertTrue("steps still offered", plan.questDefinitions.any { it.templateId == "tmpl-steps" })
    }

    @Test
    fun `training days cap the number of quests`() {
        val four = listOf("tmpl-pushups", "tmpl-squats", "tmpl-crunches", "tmpl-steps").map { candidate(it) }
        val one = generator.generate(assessment(availability = Availability(trainingDays = TrainingDaysPerWeek.ONE)), four)
        assertEquals(1, one.questDefinitions.size)
        val six = generator.generate(assessment(availability = Availability(trainingDays = TrainingDaysPerWeek.SIX)), four)
        assertEquals("capped at MAX_QUESTS", 3, six.questDefinitions.size)
    }

    @Test
    fun `shorter sessions produce lower or equal targets`() {
        val short =
            generator.generate(
                assessment(
                    availability = Availability(trainingDays = TrainingDaysPerWeek.THREE, sessionDuration = SessionDuration.UNDER_20),
                ),
                listOf(candidate("tmpl-pushups")),
            ).questDefinitions.first().target
        val long =
            generator.generate(
                assessment(
                    availability = Availability(trainingDays = TrainingDaysPerWeek.THREE, sessionDuration = SessionDuration.MIN_45_60),
                ),
                listOf(candidate("tmpl-pushups")),
            ).questDefinitions.first().target
        assertTrue("short session target <= long", short <= long)
    }

    @Test
    fun `new or returning users get a foundation plan at the minimum target`() {
        val plan = generator.generate(assessment(frequency = TrainingFrequency.NEW_OR_RETURNING), listOf(candidate("tmpl-pushups")))
        assertEquals(DifficultyBand.FOUNDATION, plan.difficultyBand)
        assertEquals("foundation starts at the template minimum", 25, plan.questDefinitions.first().target)
        assertTrue(plan.safetyAdjustments.any { it.contains("habit", ignoreCase = true) })
    }

    @Test
    fun `minors receive more conservative targets and a capped band`() {
        val adult = generator.generate(assessment(frequency = TrainingFrequency.FIVE_PLUS_WEEKLY), listOf(candidate("tmpl-pushups")))
        val minor =
            generator.generate(
                assessment(frequency = TrainingFrequency.FIVE_PLUS_WEEKLY, age = AgeSafetyCategory.MINOR_YOUNGER),
                listOf(candidate("tmpl-pushups")),
            )
        assertEquals(DifficultyBand.STEADY, adult.difficultyBand)
        assertEquals("minor band capped below STEADY", DifficultyBand.DEVELOPING, minor.difficultyBand)
        assertTrue("minor target <= adult target", minor.questDefinitions.first().target <= adult.questDefinitions.first().target)
        assertTrue(minor.safetyAdjustments.any { it.contains("under-18", ignoreCase = true) })
    }

    @Test
    fun `a limitation drops a non-substitutable conflict but keeps a substitutable one with adjustment`() {
        // Crunches conflict with back sensitivity and are NOT substitutable -> dropped.
        val backPlan =
            generator.generate(
                assessment(limitations = setOf(Limitation.BACK_SENSITIVITY)),
                listOf(candidate("tmpl-crunches"), candidate("tmpl-squats")),
            )
        assertTrue("crunches dropped for back sensitivity", backPlan.questDefinitions.none { it.templateId == "tmpl-crunches" })

        // Squats conflict with lower-body but ARE substitutable -> kept, flagged safety-adjusted.
        val legPlan = generator.generate(assessment(limitations = setOf(Limitation.LOWER_BODY)), listOf(candidate("tmpl-squats")))
        val squat = legPlan.questDefinitions.firstOrNull { it.templateId == "tmpl-squats" }
        assertTrue("squats kept via substitution", squat != null)
        assertTrue("squats flagged safety-adjusted", squat!!.safetyAdjusted)
    }

    @Test
    fun `every plan and quest is provisional and stays below the safety threshold`() {
        val plan = generator.generate(assessment(), listOf(candidate("tmpl-pushups"), candidate("tmpl-squats")))
        assertTrue(plan.provisional)
        assertFalse(plan.questDefinitions.isEmpty())
        plan.questDefinitions.forEach {
            assertTrue("provisional", it.provisional)
            assertTrue("below high-target threshold", it.target < 300)
        }
    }
}
