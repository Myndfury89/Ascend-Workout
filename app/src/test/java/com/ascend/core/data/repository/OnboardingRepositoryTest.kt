package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.model.onboarding.AbilitySnapshot
import com.ascend.core.model.onboarding.ActivityDomain
import com.ascend.core.model.onboarding.ActivityPreference
import com.ascend.core.model.onboarding.AgeSafetyCategory
import com.ascend.core.model.onboarding.AgeSafetyPolicy
import com.ascend.core.model.onboarding.Availability
import com.ascend.core.model.onboarding.BodyMetrics
import com.ascend.core.model.onboarding.ClassAffinityResult
import com.ascend.core.model.onboarding.DifficultyBand
import com.ascend.core.model.onboarding.Equipment
import com.ascend.core.model.onboarding.ExperienceLevel
import com.ascend.core.model.onboarding.InitialAssessment
import com.ascend.core.model.onboarding.InitialQuestDefinition
import com.ascend.core.model.onboarding.InitialQuestPlan
import com.ascend.core.model.onboarding.Limitation
import com.ascend.core.model.onboarding.ONBOARDING_VERSION
import com.ascend.core.model.onboarding.OnboardingState
import com.ascend.core.model.onboarding.OnboardingStep
import com.ascend.core.model.onboarding.PhysiologyProfile
import com.ascend.core.model.onboarding.PrimaryGoal
import com.ascend.core.model.onboarding.Provenance
import com.ascend.core.model.onboarding.PullUpCapability
import com.ascend.core.model.onboarding.SessionDuration
import com.ascend.core.model.onboarding.TrainingDaysPerWeek
import com.ascend.core.model.onboarding.TrainingEnvironment
import com.ascend.core.model.onboarding.TrainingFrequency
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Onboarding persistence, resume, provenance, and canonical-weight round-trip on an in-memory DB. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class OnboardingRepositoryTest {
    private lateinit var db: AscendDatabase
    private lateinit var repo: OnboardingRepositoryImpl
    private val userId = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        repo = OnboardingRepositoryImpl(db.onboardingDao(), db.playerDao()) { 123L }
        runBlocking {
            db.playerDao().upsertProfile(UserProfileEntity(id = userId, displayName = "Kai", createdAt = 0, updatedAt = 0))
        }
    }

    @After
    fun tearDown() = db.close()

    private fun fullAssessment() =
        InitialAssessment(
            userId = userId,
            primaryGoal = PrimaryGoal.STRENGTH,
            secondaryGoals = listOf(PrimaryGoal.MUSCLE_GAIN),
            trainingFrequency = TrainingFrequency.ONE_TO_TWO_WEEKLY,
            activityExperience = mapOf(ActivityDomain.STRENGTH_TRAINING to ExperienceLevel.SOME),
            activityPreferences = setOf(ActivityPreference.WEIGHTLIFTING, ActivityPreference.BODYWEIGHT),
            abilitySnapshot = AbilitySnapshot(comfortablePushUps = 20, pullUpCapability = PullUpCapability.FEW),
            equipment = setOf(Equipment.DUMBBELLS, Equipment.PULL_UP_BAR),
            environment = TrainingEnvironment.HOME,
            availability = Availability(trainingDays = TrainingDaysPerWeek.THREE, sessionDuration = SessionDuration.MIN_30_45),
            physiology =
                PhysiologyProfile(
                    sex = com.ascend.core.model.onboarding.OptionalSex.PREFER_NOT_TO_ANSWER,
                    waistCircumferenceCm = 80.0,
                ),
            bodyMetrics = BodyMetrics(heightCm = 178.0, currentWeightKg = 80.0, goalWeightKg = 75.0),
            limitations = setOf(Limitation.BACK_SENSITIVITY),
            ageSafetyCategory = AgeSafetyCategory.ADULT,
            selfReportedAt = 42L,
        )

    @Test
    fun `onboarding state resumes from the saved step`() =
        runTest {
            repo.saveState(userId, OnboardingState(currentStep = OnboardingStep.GOALS, completedSteps = setOf(OnboardingStep.WELCOME)))
            val resumed = repo.getState(userId)!!
            assertEquals(OnboardingStep.GOALS, resumed.currentStep)
            assertTrue(OnboardingStep.WELCOME in resumed.completedSteps)
            assertEquals(ONBOARDING_VERSION, resumed.version)
            assertEquals(OnboardingStep.GOALS, repo.observeState(userId).first()!!.currentStep)
        }

    @Test
    fun `assessment round-trips with provenance and merged height`() =
        runTest {
            repo.saveAssessment(fullAssessment())
            val loaded = repo.getAssessment(userId)!!
            assertEquals(PrimaryGoal.STRENGTH, loaded.primaryGoal)
            assertEquals(listOf(PrimaryGoal.MUSCLE_GAIN), loaded.secondaryGoals)
            assertEquals(setOf(ActivityPreference.WEIGHTLIFTING, ActivityPreference.BODYWEIGHT), loaded.activityPreferences)
            assertEquals(ExperienceLevel.SOME, loaded.activityExperience[ActivityDomain.STRENGTH_TRAINING])
            assertEquals(20, loaded.abilitySnapshot?.comfortablePushUps)
            assertEquals(PullUpCapability.FEW, loaded.abilitySnapshot?.pullUpCapability)
            assertEquals(TrainingDaysPerWeek.THREE, loaded.availability?.trainingDays)
            assertEquals(setOf(Limitation.BACK_SENSITIVITY), loaded.limitations)
            assertEquals(178.0, loaded.bodyMetrics.heightCm!!, 0.001)
            assertEquals(80.0, loaded.bodyMetrics.currentWeightKg!!, 0.001)
            assertEquals(Provenance.SELF_REPORTED, loaded.provenance)
            assertNull("replacedByEvidenceAt stays null in this phase", loaded.replacedByEvidenceAt)
        }

    @Test
    fun `switching weight unit never rewrites the canonical kilogram weight or creates progression`() =
        runTest {
            repo.saveAssessment(fullAssessment())
            // Change the display unit through the existing profile DAO (single source of truth).
            db.playerDao().updateWeightUnit(userId, "POUNDS", updatedAt = 1L)

            val loaded = repo.getAssessment(userId)!!
            assertEquals("canonical kg is unchanged by a unit switch", 80.0, loaded.bodyMetrics.currentWeightKg!!, 0.001)
            assertEquals("POUNDS", db.playerDao().getProfile(userId)!!.weightUnit)
            // Onboarding never enqueues progression / creates a PR.
            db.query(androidx.sqlite.db.SimpleSQLiteQuery("SELECT COUNT(*) FROM progression_event")).use { c ->
                c.moveToFirst()
                assertEquals(0, c.getInt(0))
            }
        }

    @Test
    fun `affinity and plan round-trip and stay provisional`() =
        runTest {
            repo.saveAffinity(
                userId,
                ClassAffinityResult("berserker", mapOf("berserker" to 4.0, "monk" to 1.0), "Trains strength", listOf("goal:STRENGTH")),
                createdAt = 5L,
            )
            val affinity = repo.getAffinity(userId)!!
            assertEquals("berserker", affinity.recommendedClassId)
            assertEquals(4.0, affinity.classScores["berserker"]!!, 0.001)
            assertEquals(listOf("goal:STRENGTH"), affinity.evidenceKeys)

            val plan =
                InitialQuestPlan(
                    userId = userId,
                    questDefinitions =
                        listOf(
                            InitialQuestDefinition("tmpl-pushups", "Push-ups", "reps", 25, 10, "Baseline", safetyAdjusted = true),
                            InitialQuestDefinition("tmpl-steps", "Walking Steps", "steps", 6000, null, "Easy walk"),
                        ),
                    assessmentSuggestions = emptyList(),
                    rationale = "Starter plan",
                    difficultyBand = DifficultyBand.FOUNDATION,
                    safetyAdjustments = listOf("Conservative, habit first"),
                    createdAt = 9L,
                )
            repo.savePlan(plan)
            val loaded = repo.getPlan(userId)!!
            assertEquals(2, loaded.questDefinitions.size)
            assertEquals("tmpl-pushups", loaded.questDefinitions[0].templateId)
            assertEquals("tmpl-steps", loaded.questDefinitions[1].templateId)
            assertTrue(loaded.questDefinitions[0].safetyAdjusted)
            assertTrue(loaded.provisional)
            assertEquals(listOf("Conservative, habit first"), loaded.safetyAdjustments)
        }

    @Test
    fun `saving a plan again replaces the previous items`() =
        runTest {
            val first =
                InitialQuestPlan(
                    userId,
                    listOf(InitialQuestDefinition("tmpl-pushups", "Push-ups", "reps", 25, 10, "a")),
                    emptyList(),
                    "r",
                    DifficultyBand.FOUNDATION,
                    emptyList(),
                )
            val second =
                InitialQuestPlan(
                    userId,
                    listOf(InitialQuestDefinition("tmpl-steps", "Steps", "steps", 6000, null, "b")),
                    emptyList(),
                    "r",
                    DifficultyBand.FOUNDATION,
                    emptyList(),
                )
            repo.savePlan(first)
            repo.savePlan(second)
            val loaded = repo.getPlan(userId)!!
            assertEquals(1, loaded.questDefinitions.size)
            assertEquals("tmpl-steps", loaded.questDefinitions.first().templateId)
        }

    @Test
    fun `safety defaults and completion are written to the profile`() =
        runTest {
            repo.saveSafetyProfile(userId, AgeSafetyCategory.MINOR_YOUNGER, AgeSafetyPolicy.defaultsFor(AgeSafetyCategory.MINOR_YOUNGER))
            val afterSafety = db.playerDao().getProfile(userId)!!
            assertEquals("MINOR_YOUNGER", afterSafety.ageSafetyCategory)
            assertEquals("PRIVATE", afterSafety.socialVisibility)
            assertFalse(afterSafety.partyPresenceEnabled)
            assertFalse(afterSafety.strangerDiscoveryEnabled)

            assertFalse(afterSafety.onboardingCompleted)
            repo.markCompleted(userId, ONBOARDING_VERSION)
            val afterComplete = db.playerDao().getProfile(userId)!!
            assertTrue(afterComplete.onboardingCompleted)
            assertEquals(ONBOARDING_VERSION, afterComplete.onboardingVersion)
        }
}
