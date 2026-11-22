package com.ascend.core.domain.usecase

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.ExerciseDao
import com.ascend.core.database.entity.ExerciseEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Existing installs keep edited and user-created exercises when the catalog is
 * seeded again. Only catalog ids that are not already stored are inserted.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ExerciseSeedInsertMissingTest {
    private lateinit var db: AscendDatabase
    private lateinit var exerciseDao: ExerciseDao

    @Before
    fun setUp() {
        db = openDatabase()
        exerciseDao = db.exerciseDao()
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `fresh catalog seed is idempotent and does not duplicate rows`() =
        runTest {
            val catalog = readCatalog()
            seed()
            val first = exerciseDao.getAll()

            assertEquals(catalog.keys, first.map { it.id }.toSet())
            assertEquals(catalog.size, exerciseDao.count())
            assertNoDuplicateIds(first)

            seed()

            assertEquals(first, exerciseDao.getAll())
        }

    @Test
    fun `missing catalog rows are inserted and edits plus user exercises stay`() =
        runTest {
            val catalog = readCatalog()
            val edited =
                catalog.getValue("ex-pushup").copy(
                    name = "My Push-ups",
                    tags = "CUSTOM_TAG",
                    isBuiltIn = false,
                    preferredSetSize = 7,
                    createdAt = 42L,
                    updatedAt = 43L,
                )
            val kept = catalog.getValue("ex-squat")
            val user = userExercise()
            assertFalse(catalog.containsKey(user.id))
            exerciseDao.upsertAll(listOf(edited, kept, user))

            seed()

            assertEquals(edited, exerciseDao.getById("ex-pushup"))
            assertEquals(kept, exerciseDao.getById("ex-squat"))
            assertEquals(user, exerciseDao.getById(user.id))
            assertEquals(catalog.keys + user.id, exerciseDao.ids().toSet())
            val insertedIds = catalog.keys - setOf("ex-pushup", "ex-squat")
            for (id in insertedIds) {
                val row = exerciseDao.getById(id)!!
                assertEquals(catalog.getValue(id).copy(createdAt = row.createdAt, updatedAt = row.updatedAt), row)
                assertEquals(row.createdAt, row.updatedAt)
                assertTrue(row.isBuiltIn)
                assertEquals(catalog.getValue(id).name, row.name)
                assertEquals(catalog.getValue(id).tags, row.tags)
            }
            assertEquals(catalog.size + 1, exerciseDao.count())
            assertNoDuplicateIds(exerciseDao.getAll())

            val snapshot = exerciseDao.getAll()
            seed()

            assertEquals(snapshot, exerciseDao.getAll())
            assertEquals(user, exerciseDao.getById(user.id))
            assertEquals(edited, exerciseDao.getById("ex-pushup"))
        }

    @Test
    fun `fully seeded catalog keeps edits and user created exercises`() =
        runTest {
            seed()
            val edited =
                exerciseDao.getById("ex-plank")!!.copy(
                    name = "Long Plank",
                    tags = "EDITED",
                    isBuiltIn = false,
                    createdAt = 1L,
                    updatedAt = 2L,
                )
            val user = userExercise()
            exerciseDao.upsert(edited)
            exerciseDao.upsert(user)
            val before = exerciseDao.getAll()
            val count = exerciseDao.count()

            seed()
            seed()

            assertEquals(before, exerciseDao.getAll())
            assertEquals(count, exerciseDao.count())
            assertEquals("Long Plank", exerciseDao.getById("ex-plank")!!.name)
            assertEquals(false, exerciseDao.getById("ex-plank")!!.isBuiltIn)
            assertEquals(user, exerciseDao.getById(user.id))
            assertNoDuplicateIds(exerciseDao.getAll())
        }

    @Test
    fun `user created exercises alone do not block catalog insertion`() =
        runTest {
            val catalog = readCatalog()
            val user = userExercise()
            assertFalse(catalog.containsKey(user.id))
            exerciseDao.upsert(user)
            assertEquals(1, exerciseDao.count())

            seed()

            assertEquals(user, exerciseDao.getById(user.id))
            assertEquals(false, exerciseDao.getById(user.id)!!.isBuiltIn)
            assertEquals(catalog.size + 1, exerciseDao.count())
            for (id in catalog.keys) {
                val row = exerciseDao.getById(id)!!
                assertEquals(catalog.getValue(id).copy(createdAt = row.createdAt, updatedAt = row.updatedAt), row)
                assertTrue(row.isBuiltIn)
            }
            assertNoDuplicateIds(exerciseDao.getAll())

            val snapshot = exerciseDao.getAll()
            seed()

            assertEquals(snapshot, exerciseDao.getAll())
        }

    private suspend fun seed() = SeedExerciseCatalogUseCase(exerciseDao)()

    private suspend fun readCatalog(): Map<String, ExerciseEntity> {
        val oracle = openDatabase()
        return try {
            SeedExerciseCatalogUseCase(oracle.exerciseDao())()
            val rows = oracle.exerciseDao().getAll().associateBy { it.id }
            assertTrue(rows.keys.containsAll(REQUIRED_CATALOG_IDS))
            rows
        } finally {
            oracle.close()
        }
    }

    private fun userExercise() =
        ExerciseEntity(
            id = "ex-user-handstand",
            name = "Handstand Hold",
            category = "Skill",
            primaryAttribute = "DISCIPLINE",
            measurementType = "DURATION",
            defaultUnit = "seconds",
            isWeighted = false,
            preferredSetSize = 30,
            tags = "BODYWEIGHT,BALANCE",
            isBuiltIn = false,
            createdAt = 7L,
            updatedAt = 8L,
        )

    private fun assertNoDuplicateIds(rows: List<ExerciseEntity>) {
        val ids = rows.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    private fun openDatabase(): AscendDatabase {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    private companion object {
        val REQUIRED_CATALOG_IDS = setOf("ex-pushup", "ex-squat", "ex-plank", "ex-run")
    }
}
