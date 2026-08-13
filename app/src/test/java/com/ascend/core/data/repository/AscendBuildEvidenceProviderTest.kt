package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.ExerciseEntity
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.database.entity.WorkoutEntity
import com.ascend.core.database.entity.WorkoutSetEntity
import com.ascend.core.domain.build.ActivityFamily
import com.ascend.core.domain.build.ActivityModality
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Verifies the concrete provider maps real completed workouts + sets into Build evidence shapes. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AscendBuildEvidenceProviderTest {
    private lateinit var db: AscendDatabase
    private lateinit var provider: AscendBuildEvidenceProvider
    private val now = 1_700_000_000_000L

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        provider = AscendBuildEvidenceProvider(db.workoutDao(), db.adaptiveTrainingDao())
        kotlinx.coroutines.runBlocking {
            db.playerDao().upsertProfile(UserProfileEntity(id = "u1", displayName = "U", createdAt = 0, updatedAt = 0))
        }
    }

    @After
    fun tearDown() = db.close()

    private suspend fun seedExercise(
        id: String,
        category: String,
        primaryAttribute: String,
        measurementType: String,
        weighted: Boolean,
        tags: String,
    ) {
        db.exerciseDao().upsert(
            ExerciseEntity(
                id = id,
                name = id,
                category = category,
                primaryAttribute = primaryAttribute,
                measurementType = measurementType,
                defaultUnit = "u",
                isWeighted = weighted,
                tags = tags,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
    }

    private suspend fun seedWorkout(
        id: String,
        status: String,
        performedAt: Long,
    ) {
        db.workoutDao().upsertWorkout(
            WorkoutEntity(
                id = id,
                userId = "u1",
                title = id,
                status = status,
                performedAt = performedAt,
                durationSeconds = 40 * 60,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
    }

    private suspend fun seedSet(
        id: String,
        workoutId: String,
        exerciseId: String,
        weight: Double?,
        distance: Double?,
        durationSeconds: Long?,
        volume: Double,
        reps: Int? = null,
    ) {
        db.workoutDao().upsertSet(
            WorkoutSetEntity(
                id = id,
                workoutId = workoutId,
                exerciseId = exerciseId,
                weight = weight,
                reps = reps,
                distance = distance,
                durationSeconds = durationSeconds,
                volume = volume,
                unit = "u",
                completedAt = 0,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
    }

    @Test
    fun `completed lifting and running workouts map to the right evidence shapes`() =
        runTest {
            seedExercise("ex-bench", "Weights", "STRENGTH", "WEIGHT_AND_REPS", weighted = true, tags = "HEAVY_STRENGTH")
            seedExercise("ex-run", "Cardio", "ENDURANCE", "DISTANCE", weighted = false, tags = "STEADY_STATE_CARDIO")

            seedWorkout("w-lift", "COMPLETED", now - 2 * 86_400_000L)
            seedSet("s1", "w-lift", "ex-bench", weight = 100.0, distance = null, durationSeconds = null, volume = 800.0)

            seedWorkout("w-run", "COMPLETED", now - 1 * 86_400_000L)
            seedSet("s2", "w-run", "ex-run", weight = null, distance = 5_000.0, durationSeconds = 1_500L, volume = 5_000.0)

            val evidence = provider.gather("u1")

            assertEquals(2, evidence.sessions.size)
            assertTrue("strength set captured", evidence.strengthSets.any { it.volume == 800.0 })
            assertTrue("run distance captured", evidence.distances.any { it.meters == 5_000.0 && it.modality == ActivityModality.RUN })
            assertTrue("pace derived from distance and duration", evidence.paces.isNotEmpty())
            val fams = evidence.families.map { it.family }.toSet()
            assertTrue("lifting -> traditional strength family", ActivityFamily.TRADITIONAL_STRENGTH in fams)
            assertTrue("running -> run/walk family", ActivityFamily.RUN_WALK in fams)
        }

    @Test
    fun `personal records are detected from improving lift history, baseline and regressions are not`() =
        runTest {
            seedExercise("ex-bench", "Weights", "STRENGTH", "WEIGHT_AND_REPS", weighted = true, tags = "HEAVY_STRENGTH")
            // Three sessions of the same lift: baseline, an improvement (PR), then a regression (not a PR).
            seedWorkout("w1", "COMPLETED", now - 20 * 86_400_000L)
            seedSet("s1", "w1", "ex-bench", weight = 100.0, reps = 5, distance = null, durationSeconds = null, volume = 500.0)
            seedWorkout("w2", "COMPLETED", now - 10 * 86_400_000L)
            seedSet("s2", "w2", "ex-bench", weight = 110.0, reps = 5, distance = null, durationSeconds = null, volume = 550.0)
            seedWorkout("w3", "COMPLETED", now - 2 * 86_400_000L)
            seedSet("s3", "w3", "ex-bench", weight = 105.0, reps = 5, distance = null, durationSeconds = null, volume = 525.0)

            val evidence = provider.gather("u1")
            val prCount = evidence.strengthSets.count { it.isPersonalRecord }

            assertEquals("only the improvement over baseline is a PR", 1, prCount)
        }

    @Test
    fun `abandoned or in-progress workouts are ignored`() =
        runTest {
            seedExercise("ex-bench", "Weights", "STRENGTH", "WEIGHT_AND_REPS", weighted = true, tags = "HEAVY_STRENGTH")
            seedWorkout("w-open", "IN_PROGRESS", now - 86_400_000L)
            seedSet("s1", "w-open", "ex-bench", weight = 100.0, distance = null, durationSeconds = null, volume = 800.0)

            val evidence = provider.gather("u1")

            assertTrue(evidence.sessions.isEmpty())
            assertTrue(evidence.strengthSets.isEmpty())
        }
}
