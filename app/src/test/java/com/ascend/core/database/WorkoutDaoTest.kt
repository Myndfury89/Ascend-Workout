package com.ascend.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.dao.ExerciseDao
import com.ascend.core.database.dao.WorkoutDao
import com.ascend.core.database.entity.ExerciseEntity
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.database.entity.WorkoutEntity
import com.ascend.core.database.entity.WorkoutSetEntity
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class WorkoutDaoTest {
    private lateinit var db: AscendDatabase
    private lateinit var workoutDao: WorkoutDao
    private lateinit var exerciseDao: ExerciseDao

    private val userId = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java)
                .allowMainThreadQueries().build()
        workoutDao = db.workoutDao()
        exerciseDao = db.exerciseDao()
        runBlocking {
            db.playerDao().upsertProfile(
                UserProfileEntity(id = userId, displayName = "Tester", createdAt = 0, updatedAt = 0),
            )
            exerciseDao.upsert(
                ExerciseEntity(
                    id = "ex-pushup",
                    name = "Push-ups",
                    category = "Bodyweight",
                    primaryAttribute = "STRENGTH",
                    measurementType = "REPETITIONS",
                    defaultUnit = "reps",
                    createdAt = 0,
                    updatedAt = 0,
                ),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `workout with sets joins the exercise and orders by index`() =
        runTest {
            workoutDao.upsertWorkout(
                WorkoutEntity(
                    id = "w1",
                    userId = userId,
                    title = "Session",
                    performedAt = 10,
                    createdAt = 0,
                    updatedAt = 0,
                ),
            )
            insertSet("s2", order = 1, volume = 20.0)
            insertSet("s1", order = 0, volume = 15.0)

            val wws = workoutDao.getWorkoutWithSets("w1")!!
            assertEquals(2, wws.sets.size)
            // Relation join resolves the exercise.
            assertEquals("Push-ups", wws.sets.first().exercise?.name)
            assertEquals(35.0, wws.sets.sumOf { it.set.volume }, 0.0001)
        }

    @Test
    fun `max order index tracks appended sets`() =
        runTest {
            workoutDao.upsertWorkout(
                WorkoutEntity(
                    id = "w1",
                    userId = userId,
                    title = "Session",
                    performedAt = 10,
                    createdAt = 0,
                    updatedAt = 0,
                ),
            )
            assertEquals(-1, workoutDao.maxOrderIndex("w1"))
            insertSet("s1", order = 0, volume = 10.0)
            insertSet("s2", order = 1, volume = 10.0)
            assertEquals(1, workoutDao.maxOrderIndex("w1"))
        }

    @Test
    fun `deleting a set removes only that set`() =
        runTest {
            workoutDao.upsertWorkout(
                WorkoutEntity(
                    id = "w1",
                    userId = userId,
                    title = "Session",
                    performedAt = 10,
                    createdAt = 0,
                    updatedAt = 0,
                ),
            )
            insertSet("s1", order = 0, volume = 10.0)
            insertSet("s2", order = 1, volume = 10.0)

            workoutDao.deleteSet("s1")
            assertNull(workoutDao.getSet("s1"))
            assertEquals(1, workoutDao.getSets("w1").size)
        }

    private suspend fun insertSet(
        id: String,
        order: Int,
        volume: Double,
    ) {
        workoutDao.upsertSet(
            WorkoutSetEntity(
                id = id, workoutId = "w1", exerciseId = "ex-pushup", orderIndex = order,
                reps = volume.toInt(), volume = volume, unit = "reps",
                completedAt = order.toLong(), createdAt = 0, updatedAt = 0,
            ),
        )
    }
}
