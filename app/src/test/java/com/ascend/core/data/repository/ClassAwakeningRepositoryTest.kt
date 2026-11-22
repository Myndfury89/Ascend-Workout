package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Room
import androidx.room.Update
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.common.BuildFlags
import com.ascend.core.data.mapper.toEntity
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.ClassAwakeningDao
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.classes.AwakeningSource
import com.ascend.core.domain.classes.ClassAwakeningRepository
import com.ascend.core.domain.classes.ClassCatalog
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ClassAwakeningRepositoryTest {
    private lateinit var db: AscendDatabase
    private lateinit var repo: ClassAwakeningRepositoryImpl

    private val userId = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        repo = ClassAwakeningRepositoryImpl(db.classAwakeningDao(), db.classDao(), BuildFlags(isDebug = false))
        runBlocking {
            db.playerDao().upsertProfile(
                UserProfileEntity(id = userId, displayName = "Tester", createdAt = 0, updatedAt = 0),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `starters are awake with no awakening row`() =
        runTest {
            assertTrue(db.classAwakeningDao().ids(userId).isEmpty())
            assertEquals(setOf("berserker", "monk", "mage"), repo.ids(userId))
            assertEquals(repo.ids(userId), repo.observe(userId).first())
            assertTrue(repo.isAwake(userId, "monk"))
            assertFalse(repo.isAwake(userId, "assassin"))
        }

    @Test
    fun `seeded startsAwake flags decide starters and an edited flag is kept`() =
        runTest {
            db.classDao().upsertDefinitions(ClassCatalog.ALL.map { it.toEntity(0) })
            assertTrue(db.classAwakeningDao().ids(userId).isEmpty())
            assertEquals(setOf("berserker", "monk", "mage"), repo.ids(userId))

            db.openHelper.writableDatabase.execSQL("UPDATE class_definition SET startsAwake = 0 WHERE id = 'berserker'")
            val awake = repo.ids(userId)
            assertFalse(awake.contains("berserker"))
            assertTrue(awake.contains("monk"))
            assertTrue(awake.contains("mage"))
            assertTrue(db.classAwakeningDao().ids(userId).isEmpty())
        }

    @Test
    fun `awaken is idempotent and keeps the original row`() =
        runTest {
            val wrote =
                repo.awaken(
                    userId,
                    "assassin",
                    AwakeningSource.BUILD_AFFINITY,
                    evidenceJson = "{\"coverage\":0.8}",
                    awakenedAt = 10,
                )
            assertTrue(wrote)
            val again =
                repo.awaken(
                    userId,
                    "assassin",
                    AwakeningSource.ONBOARDING_RECOMMENDATION,
                    evidenceJson = "later",
                    awakenedAt = 99,
                )
            assertFalse(again)

            val row = db.classAwakeningDao().observe(userId).first().single { it.classId == "assassin" }
            assertEquals(AwakeningSource.BUILD_AFFINITY.name, row.source)
            assertEquals("{\"coverage\":0.8}", row.evidenceJson)
            assertEquals(10L, row.awakenedAt)
            assertTrue(repo.isAwake(userId, "assassin"))
        }

    @Test
    fun `repository and dao expose no revoke update or delete`() {
        assertInsertOnly(ClassAwakeningRepository::class.java)
        assertInsertOnly(ClassAwakeningDao::class.java)
    }

    private fun assertInsertOnly(type: Class<*>) {
        val banned = listOf("delete", "remove", "revoke", "clear", "update", "forget")
        type.declaredMethods.forEach { method ->
            val name = method.name.lowercase()
            assertTrue("${type.simpleName}.${method.name}", banned.none { name.contains(it) })
            assertNull(method.getAnnotation(Delete::class.java))
            assertNull(method.getAnnotation(Update::class.java))
            val sql = method.getAnnotation(Query::class.java)?.value?.lowercase()
            if (sql != null) {
                assertFalse(sql, sql.contains("delete") || sql.contains("update") || sql.contains("drop"))
            }
        }
    }
}
