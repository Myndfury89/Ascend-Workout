package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.classes.ClassRewardApplier
import com.ascend.core.domain.classes.MulticlassRewardCalculator
import com.ascend.core.domain.progression.AttributeProgressCalculator
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.ProgressionEventFactory
import com.ascend.core.domain.progression.ProgressionEventPublisher
import com.ascend.core.domain.progression.RankCalculator
import com.ascend.core.domain.progression.XpCalculator
import com.ascend.core.domain.repository.CompleteWorkoutResult
import com.ascend.core.domain.repository.NewSetSpec
import com.ascend.core.domain.repository.NewWorkoutSpec
import com.ascend.core.domain.usecase.SeedExerciseCatalogUseCase
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ProgressionEventType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Proves the class system's core guarantees on a real completion: **Player XP is
 * class‑neutral**, while attribute proficiency, Class XP, and unique proficiency are
 * class‑shaped and tag‑driven — and it's all idempotent.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ClassRewardFlowTest {
    private lateinit var db: AscendDatabase
    private lateinit var workouts: WorkoutRepositoryImpl
    private lateinit var classes: ClassRepositoryImpl
    private lateinit var events: ProgressionEventRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        val progression =
            ProgressionRepositoryImpl(db, db.playerDao(), db.xpDao(), db.attributeDao(), LevelCalculator(), RankCalculator())
        val levelCalc = LevelCalculator()
        classes = ClassRepositoryImpl(db, db.classDao(), levelCalc)
        events = ProgressionEventRepositoryImpl(db, db.progressionEventDao())
        val applier = ClassRewardApplier(classes, MulticlassRewardCalculator())
        val publisher = ProgressionEventPublisher(events, ProgressionEventFactory(), classes, levelCalc)
        workouts =
            WorkoutRepositoryImpl(
                db, db.workoutDao(), db.exerciseDao(), progression, XpCalculator(),
                AttributeProgressCalculator(), applier, publisher,
            )
        runBlocking {
            SeedExerciseCatalogUseCase(db.exerciseDao())()
            listOf("u_monk", "u_zerk", "u_multi").forEach { id ->
                db.playerDao().upsertProfile(UserProfileEntity(id = id, displayName = id, createdAt = 0, updatedAt = 0))
            }
        }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun completePushUpWorkout(userId: String): CompleteWorkoutResult.Completed {
        val id = workouts.createWorkout(NewWorkoutSpec(userId = userId, title = "Push Day"))
        workouts.addSet(id, NewSetSpec(exerciseId = "ex-pushup", volume = 100.0, unit = "reps", reps = 100))
        workouts.addSet(id, NewSetSpec(exerciseId = "ex-pushup", volume = 90.0, unit = "reps", reps = 90))
        return workouts.completeWorkout(id) as CompleteWorkoutResult.Completed
    }

    @Test
    fun `player xp is class-neutral while everything else is class-shaped`() =
        runTest {
            classes.setClasses("u_monk", primaryClassId = "monk", secondaryClassId = null)
            classes.setClasses("u_zerk", primaryClassId = "berserker", secondaryClassId = null)

            val monk = completePushUpWorkout("u_monk")
            val zerk = completePushUpWorkout("u_zerk")

            // Player XP identical across classes -> no class is a faster way to level the account.
            assertEquals(monk.xpAwarded, zerk.xpAwarded)
            assertEquals(monk.xpAwarded, monk.rewardBreakdown.basePlayerXp)

            // Attribute proficiency reshaped by the primary class (base STR 29, DISC 10).
            assertEquals(36L, monk.attributeDeltas[AttributeType.STRENGTH]) // 29 * 1.25 -> 36
            assertEquals(14L, monk.attributeDeltas[AttributeType.DISCIPLINE]) // 10 * 1.35 -> 14
            assertEquals(44L, zerk.attributeDeltas[AttributeType.STRENGTH]) // 29 * 1.50 -> 44
            assertEquals(10L, zerk.attributeDeltas[AttributeType.DISCIPLINE]) // 10 * 1.00

            // Class XP favors the Monk (push-ups are favored) over the Berserker (unfavored).
            val monkLine = monk.rewardBreakdown.primaryClass!!
            val zerkLine = zerk.rewardBreakdown.primaryClass!!
            assertEquals("monk", monkLine.classId)
            assertTrue("favored class XP beats unfavored", monkLine.classXp > zerkLine.classXp)
            assertEquals(monkLine.classXp, classes.totalClassXp("u_monk", "monk"))

            // Unique proficiency: earned when favored, zero when not.
            assertTrue(monkLine.uniqueProficiencyGain > 0)
            assertEquals(monkLine.uniqueProficiencyGain, classes.totalProficiency("u_monk", "BODY_MASTERY"))
            assertEquals(0L, zerkLine.uniqueProficiencyGain)
            assertEquals(0L, classes.totalProficiency("u_zerk", "FORCE"))
        }

    @Test
    fun `magician cardio activity scales endurance and earns favored class xp`() =
        runTest {
            db.playerDao().upsertProfile(UserProfileEntity(id = "u_mage", displayName = "Mage", createdAt = 0, updatedAt = 0))
            classes.setClasses("u_mage", primaryClassId = "magician", secondaryClassId = null)

            val id = workouts.createWorkout(NewWorkoutSpec(userId = "u_mage", title = "Run"))
            workouts.addSet(id, NewSetSpec(exerciseId = "ex-run", volume = 300.0, unit = "metres"))
            val result = workouts.completeWorkout(id) as CompleteWorkoutResult.Completed

            // Running trains Endurance (base 45); Magician Endurance multiplier 1.50 -> 68.
            assertEquals(68L, result.attributeDeltas[AttributeType.ENDURANCE])
            val line = result.rewardBreakdown.primaryClass!!
            assertEquals("magician", line.classId)
            // Cardio is favored by the Magician -> unique proficiency earned.
            assertTrue(line.uniqueProficiencyGain > 0)
            assertEquals(1.0, line.affinity, 1e-9)
        }

    @Test
    fun `secondary class earns its own class xp but does not re-modify attributes`() =
        runTest {
            classes.setClasses("u_multi", primaryClassId = "monk", secondaryClassId = "magician")

            val result = completePushUpWorkout("u_multi")

            // Attributes still shaped only by the primary (Monk) class.
            assertEquals(36L, result.attributeDeltas[AttributeType.STRENGTH])

            val secondary = result.rewardBreakdown.secondaryClass!!
            assertEquals("magician", secondary.classId)
            assertEquals(0.5, secondary.allocation, 1e-9)
            // Secondary Class XP is awarded and tracked at the reduced allocation.
            assertTrue(secondary.classXp > 0)
            assertEquals(secondary.classXp, classes.totalClassXp("u_multi", "magician"))
        }

    @Test
    fun `completion enqueues player and class presentation events`() =
        runTest {
            classes.setClasses("u_monk", primaryClassId = "monk", secondaryClassId = null)
            completePushUpWorkout("u_monk")

            val pending = events.getPending("u_monk")
            val types = pending.map { it.type }
            assertTrue(types.contains(ProgressionEventType.XP_GAINED))
            assertTrue(types.contains(ProgressionEventType.ATTRIBUTE_CHANGED))

            val classXp = pending.first { it.type == ProgressionEventType.CLASS_XP_GAINED }
            assertEquals("monk", classXp.subjectKey)
            assertEquals(classes.totalClassXp("u_monk", "monk"), classXp.toValue)

            val prof = pending.first { it.type == ProgressionEventType.PROFICIENCY_GAINED }
            assertEquals("BODY_MASTERY", prof.subjectKey)

            // One coherent batch, densely sequenced.
            assertEquals(1, pending.map { it.batchId }.distinct().size)
            assertEquals(pending.indices.toList(), pending.map { it.sequence })
        }

    @Test
    fun `no class means no class events`() =
        runTest {
            // u_multi has no class set here.
            db.playerDao().upsertProfile(UserProfileEntity(id = "u_none", displayName = "None", createdAt = 0, updatedAt = 0))
            completePushUpWorkout("u_none")

            val types = events.getPending("u_none").map { it.type }
            assertTrue(types.contains(ProgressionEventType.XP_GAINED))
            assertTrue(types.none { it == ProgressionEventType.CLASS_XP_GAINED || it == ProgressionEventType.PROFICIENCY_GAINED })
        }

    @Test
    fun `re-completing does not double-award class rewards`() =
        runTest {
            classes.setClasses("u_monk", primaryClassId = "monk", secondaryClassId = null)

            val id = workouts.createWorkout(NewWorkoutSpec(userId = "u_monk", title = "Push Day"))
            workouts.addSet(id, NewSetSpec(exerciseId = "ex-pushup", volume = 100.0, unit = "reps", reps = 100))
            workouts.addSet(id, NewSetSpec(exerciseId = "ex-pushup", volume = 90.0, unit = "reps", reps = 90))

            val first = workouts.completeWorkout(id) as CompleteWorkoutResult.Completed
            val monkXp = classes.totalClassXp("u_monk", "monk")
            val bodyMastery = classes.totalProficiency("u_monk", "BODY_MASTERY")
            assertTrue(first.rewardBreakdown.primaryClass!!.classXp > 0)
            assertTrue(monkXp > 0)

            // Second completion of the same workout awards nothing more.
            assertEquals(CompleteWorkoutResult.AlreadyCompleted, workouts.completeWorkout(id))
            assertEquals(monkXp, classes.totalClassXp("u_monk", "monk"))
            assertEquals(bodyMastery, classes.totalProficiency("u_monk", "BODY_MASTERY"))
        }
}
