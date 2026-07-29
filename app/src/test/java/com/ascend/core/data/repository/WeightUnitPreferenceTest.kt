package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.common.WeightUnit
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.AscendMigrations
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.progression.LevelCalculator
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class WeightUnitPreferenceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val dbName = "weight-unit-pref-test.db"
    private val userId = "u1"

    private fun openDb(): AscendDatabase =
        Room.databaseBuilder(context, AscendDatabase::class.java, dbName)
            .addMigrations(*AscendMigrations.ALL)
            .allowMainThreadQueries()
            .build()

    private fun repo(db: AscendDatabase) = PlayerRepositoryImpl(db, db.playerDao(), LevelCalculator())

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
        val db = openDb()
        runBlocking { db.playerDao().upsertProfile(UserProfileEntity(id = userId, displayName = "T", createdAt = 0, updatedAt = 0)) }
        db.close()
    }

    @After
    fun tearDown() = context.deleteDatabase(dbName).let {}

    @Test
    fun `the default weight unit is kilograms`() =
        runTest {
            val db = openDb()
            assertEquals(WeightUnit.KILOGRAMS, repo(db).weightUnit(userId))
            db.close()
        }

    @Test
    fun `the selected weight unit persists across a restart`() =
        runTest {
            openDb().let { db ->
                repo(db).setWeightUnit(userId, WeightUnit.POUNDS)
                db.close()
            }
            // Reopen the (file-backed) database — simulating an app restart.
            openDb().let { db ->
                assertEquals(WeightUnit.POUNDS, repo(db).weightUnit(userId))
                db.close()
            }
        }

    @Test
    fun `changing the weight unit never rewrites a stored canonical weight`() =
        runTest {
            val db = openDb()
            // A workout set stored canonically at 100.0 kg.
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO exercise (id, name, category, primaryAttribute, measurementType, defaultUnit, " +
                    "isWeighted, tags, isBuiltIn, createdAt, updatedAt) " +
                    "VALUES ('ex1', 'Bench', 'Weights', 'STRENGTH', 'WEIGHT_AND_REPS', 'reps', 1, '', 1, 0, 0)",
            )
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO workout (id, userId, title, difficulty, status, performedAt, createdAt, updatedAt) " +
                    "VALUES ('w1', 'u1', 'S', 'MODERATE', 'IN_PROGRESS', 0, 0, 0)",
            )
            db.openHelper.writableDatabase.execSQL(
                "INSERT INTO workout_set (id, workoutId, exerciseId, orderIndex, reps, weight, volume, unit, " +
                    "completedAt, createdAt, updatedAt) VALUES ('s1', 'w1', 'ex1', 0, 5, 100.0, 5.0, 'reps', 0, 0, 0)",
            )

            repo(db).setWeightUnit(userId, WeightUnit.POUNDS)
            repo(db).setWeightUnit(userId, WeightUnit.KILOGRAMS)

            db.openHelper.writableDatabase.query("SELECT weight FROM workout_set WHERE id = 's1'").use { c ->
                c.moveToFirst()
                assertEquals("the canonical stored weight is untouched by a display-unit switch", 100.0, c.getDouble(0), 0.0)
            }
            // No progression side effect from a preference change.
            assertEquals(null, db.playerDao().getProgress(userId)?.lifetimeXp?.takeIf { it > 0 })
            db.close()
        }
}
