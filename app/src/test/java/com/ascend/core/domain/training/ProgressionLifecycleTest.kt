package com.ascend.core.domain.training

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.common.newId
import com.ascend.core.data.repository.ClassRepositoryImpl
import com.ascend.core.data.repository.ProgressionRecommendationRepositoryImpl
import com.ascend.core.data.repository.ProgressionRepositoryImpl
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.classes.ClassRewardApplier
import com.ascend.core.domain.classes.MulticlassRewardCalculator
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.RankCalculator
import com.ascend.core.domain.repository.ApplyRecommendationResult
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ExercisePrescription
import com.ascend.core.model.ProgressionMilestone
import com.ascend.core.model.ProgressionMilestoneType
import com.ascend.core.model.ProgressionRecommendation
import com.ascend.core.model.ProgressionRecommendationType
import com.ascend.core.model.ProgressionSafetyState
import com.ascend.core.model.ProgressionStrategy
import com.ascend.core.model.RecommendationStatus
import com.ascend.core.model.TrainingReadinessState
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ProgressionLifecycleTest {
    private lateinit var db: AscendDatabase
    private lateinit var recommendations: ProgressionRecommendationRepositoryImpl
    private lateinit var rewards: ProgressionRewardService
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var classes: ClassRepositoryImpl

    private val userId = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        val level = LevelCalculator()
        progression = ProgressionRepositoryImpl(db, db.playerDao(), db.xpDao(), db.attributeDao(), level, RankCalculator())
        classes = ClassRepositoryImpl(db, db.classDao(), level)
        recommendations = ProgressionRecommendationRepositoryImpl(db, db.adaptiveTrainingDao())
        rewards =
            ProgressionRewardService(
                db, db.adaptiveTrainingDao(), progression,
                ClassRewardApplier(classes, MulticlassRewardCalculator()), ProgressionRewardCalculator(),
            )
        runBlocking {
            db.playerDao().upsertProfile(UserProfileEntity(id = userId, displayName = "T", createdAt = 0, updatedAt = 0))
        }
    }

    @After
    fun tearDown() = db.close()

    private fun prescription(
        id: String,
        weight: Double,
        status: String = "ACTIVE",
    ) = ExercisePrescription(
        id = id, userId = userId, exerciseId = "ex-bench", progressionStrategy = ProgressionStrategy.DOUBLE_PROGRESSION,
        targetSets = 3, minimumReps = 8, maximumReps = 10, targetWeight = weight, targetRestSeconds = 120, status = status,
    )

    private fun loadRecommendation(): ProgressionRecommendation =
        ProgressionRecommendation(
            id = newId(), userId = userId, recommendationType = ProgressionRecommendationType.INCREASE_WEIGHT,
            exerciseId = "ex-bench", questTemplateId = null, currentPrescriptionId = "p-current",
            proposed = prescription("p-proposed", 140.0, status = "PROPOSED"), proposedTarget = null,
            readinessState = TrainingReadinessState.READY_FOR_LOAD_PROGRESSION, reason = "top range reached",
            evidence = listOf("10,10,10"), confidence = 1.0, safetyState = ProgressionSafetyState.OK,
            requiresConfirmation = true, status = RecommendationStatus.PENDING, generatedAt = 0, expiresAt = Long.MAX_VALUE,
        )

    @Test
    fun `recommendation persists, applies once, and supersedes the old prescription`() =
        runTest {
            recommendations.savePrescription(prescription("p-current", 135.0))
            val rec = loadRecommendation()
            recommendations.save(rec)

            // Survives (re-read) with its proposed prescription.
            val loaded = recommendations.get(rec.id)!!
            assertEquals(140.0, loaded.proposed!!.targetWeight!!, 1e-9)

            assertTrue(recommendations.accept(rec.id))
            val applied = recommendations.apply(rec.id) as ApplyRecommendationResult.Applied
            assertEquals("p-proposed", applied.newPrescriptionId)

            // Applied exactly once.
            assertEquals(ApplyRecommendationResult.AlreadyApplied, recommendations.apply(rec.id))

            assertEquals("ACTIVE", db.adaptiveTrainingDao().getPrescription("p-proposed")!!.status)
            assertEquals("SUPERSEDED", db.adaptiveTrainingDao().getPrescription("p-current")!!.status)
        }

    @Test
    fun `rejecting a recommendation changes nothing else`() =
        runTest {
            val rec = loadRecommendation()
            recommendations.save(rec)
            assertTrue(recommendations.reject(rec.id))

            assertEquals(RecommendationStatus.REJECTED.name, db.adaptiveTrainingDao().getRecommendation(rec.id)!!.status)
            // No XP awarded from a rejection.
            assertNull(progression.getProgress(userId))
        }

    @Test
    fun `reward is granted once and duplicate milestones do not double-award`() =
        runTest {
            val milestone =
                ProgressionMilestone(
                    id = newId(), userId = userId, milestoneType = ProgressionMilestoneType.LOAD_INCREASE_COMPLETED,
                    exerciseId = "ex-bench", questTemplateId = null, sourceRecommendationId = "r1",
                    previousValue = 135.0, newValue = 140.0, createdAt = 0,
                )
            val first = rewards.award(milestone, AttributeType.STRENGTH)
            assertTrue(first.playerXp > 0)
            assertEquals(first.playerXp, progression.getProgress(userId)!!.lifetimeXp)

            // Same milestone identity (new row id, same key) -> no second award.
            val second = rewards.award(milestone.copy(id = newId()), AttributeType.STRENGTH)
            assertTrue(second.alreadyAwarded)
            assertEquals(0L, second.playerXp)
            assertEquals(first.playerXp, progression.getProgress(userId)!!.lifetimeXp)
        }

    @Test
    fun `progression reward player xp stays class-neutral`() =
        runTest {
            classes.setClasses(userId, primaryClassId = "monk", secondaryClassId = null)
            val milestone =
                ProgressionMilestone(
                    id = newId(), userId = userId, milestoneType = ProgressionMilestoneType.LOAD_INCREASE_COMPLETED,
                    exerciseId = "ex-bench", questTemplateId = null, sourceRecommendationId = "r2",
                    previousValue = 135.0, newValue = 140.0, createdAt = 0,
                )
            val breakdown = rewards.award(milestone, AttributeType.STRENGTH, activityTags = setOf("HEAVY_STRENGTH"))

            // Player XP is the class-neutral base; class XP is tracked separately.
            assertEquals(120L, breakdown.playerXp)
            assertEquals(120L, progression.getProgress(userId)!!.lifetimeXp)
            assertEquals("monk", breakdown.primaryClass!!.classId)
            assertTrue(classes.totalClassXp(userId, "monk") > 0)
        }
}
