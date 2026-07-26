package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.common.LOCAL_USER_ID
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.progression.AttributeProgressCalculator
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.RankCalculator
import com.ascend.core.domain.progression.XpCalculator
import com.ascend.core.domain.repository.CompleteWorkoutResult
import com.ascend.core.domain.repository.NewSetSpec
import com.ascend.core.domain.repository.NewWorkoutSpec
import com.ascend.core.domain.usecase.SeedExerciseCatalogUseCase
import com.ascend.core.model.AttributeType
import com.ascend.core.model.Difficulty
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
 * End-to-end proof of the Milestone 2 workout flow: seed catalog -> log sets across
 * two exercises -> finish -> XP awarded exactly once -> level up -> the right
 * attributes grow (Strength from push-up volume; Discipline from plank volume +
 * the flat adherence bonus). Completing again must not double-award.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class WorkoutFlowIntegrationTest {
    private lateinit var db: AscendDatabase
    private lateinit var progression: ProgressionRepositoryImpl
    private lateinit var workouts: WorkoutRepositoryImpl

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java)
                .allowMainThreadQueries().build()
        progression =
            ProgressionRepositoryImpl(
                db, db.playerDao(), db.xpDao(), db.attributeDao(), LevelCalculator(), RankCalculator(),
            )
        workouts =
            WorkoutRepositoryImpl(
                db, db.workoutDao(), db.exerciseDao(), progression, XpCalculator(), AttributeProgressCalculator(),
            )
        runBlocking {
            db.playerDao().upsertProfile(
                UserProfileEntity(id = LOCAL_USER_ID, displayName = "Tester", createdAt = 0, updatedAt = 0),
            )
            SeedExerciseCatalogUseCase(db.exerciseDao())()
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `seeded catalog is populated once and is idempotent`() =
        runTest {
            val first = db.exerciseDao().count()
            assertTrue(first > 0)
            SeedExerciseCatalogUseCase(db.exerciseDao())()
            assertEquals("re-seeding does not duplicate rows", first, db.exerciseDao().count())
        }

    @Test
    fun `full workout flow awards xp once, levels up, and grows the right attributes`() =
        runTest {
            val workoutId =
                workouts.createWorkout(
                    NewWorkoutSpec(userId = LOCAL_USER_ID, title = "Upper Body", difficulty = Difficulty.MODERATE),
                )

            // 190 reps of push-ups (STRENGTH) + 60s plank (DISCIPLINE) = 250 total volume.
            workouts.addSet(workoutId, NewSetSpec(exerciseId = "ex-pushup", volume = 100.0, unit = "reps", reps = 100))
            workouts.addSet(workoutId, NewSetSpec(exerciseId = "ex-pushup", volume = 90.0, unit = "reps", reps = 90))
            val plankSet =
                workouts.addSet(
                    workoutId,
                    NewSetSpec(exerciseId = "ex-plank", volume = 90.0, unit = "seconds", durationSeconds = 90),
                )

            var workout = workouts.getWorkout(workoutId)!!
            assertEquals(3, workout.setCount)
            assertEquals(280.0, workout.totalVolume, 0.0001)

            // Trim the plank down to 60s by deleting and re-adding -> 250 total.
            assertTrue(workouts.deleteSet(plankSet))
            workouts.addSet(
                workoutId,
                NewSetSpec(exerciseId = "ex-plank", volume = 60.0, unit = "seconds", durationSeconds = 60),
            )
            workout = workouts.getWorkout(workoutId)!!
            assertEquals(250.0, workout.totalVolume, 0.0001)

            // Finish: workoutBase 100 + duration 0 + intensity(MODERATE) 30 + volume(250 -> full) 100 = 230 XP.
            val result = workouts.completeWorkout(workoutId) as CompleteWorkoutResult.Completed
            assertEquals(230L, result.xpAwarded)
            assertTrue(result.leveledUp)
            // Strength from 190 reps: round(190 * 0.15) = 29.
            assertEquals(29L, result.attributeDeltas[AttributeType.STRENGTH])
            // Discipline from 60s plank volume (round(60*0.15)=9) + flat adherence (10) = 19.
            assertEquals(19L, result.attributeDeltas[AttributeType.DISCIPLINE])

            val progress = progression.getProgress(LOCAL_USER_ID)!!
            assertEquals(230L, progress.lifetimeXp)
            assertEquals(2, progress.level)
            val stats = db.playerDao().getStats(LOCAL_USER_ID)!!
            assertEquals(29L, stats.strength)
            assertEquals(19L, stats.discipline)

            // Completing again must not double-award XP or attributes.
            assertEquals(CompleteWorkoutResult.AlreadyCompleted, workouts.completeWorkout(workoutId))
            assertEquals(230L, progression.getProgress(LOCAL_USER_ID)!!.lifetimeXp)
            assertEquals(29L, db.playerDao().getStats(LOCAL_USER_ID)!!.strength)
        }

    @Test
    fun `completing an empty workout awards nothing`() =
        runTest {
            val workoutId =
                workouts.createWorkout(NewWorkoutSpec(userId = LOCAL_USER_ID, title = "Empty"))
            assertEquals(CompleteWorkoutResult.Empty, workouts.completeWorkout(workoutId))
            assertEquals(0L, progression.getProgress(LOCAL_USER_ID)?.lifetimeXp ?: 0L)
        }

    @Test
    fun `completing an unknown workout returns NotFound`() =
        runTest {
            assertEquals(CompleteWorkoutResult.NotFound, workouts.completeWorkout("nope"))
        }
}
