package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.common.BuildFlags
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.classes.AwakeningSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
 * Unit tests run against a debug test build, where `BuildConfig.DEBUG` is true. The guard is the
 * injected [BuildFlags.isDebug], so these tests pass `false` to stand in for a release build.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ClassAwakeningDebugGuardTest {
    private lateinit var db: AscendDatabase

    private val userId = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        runBlocking {
            db.playerDao().upsertProfile(
                UserProfileEntity(id = userId, displayName = "Tester", createdAt = 0, updatedAt = 0),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `debug override writes nothing when the debug flag is false`() =
        runTest {
            val repo = repo(isDebug = false)
            val wrote =
                repo.awaken(
                    userId,
                    "assassin",
                    AwakeningSource.DEBUG_OVERRIDE,
                    evidenceJson = "x",
                    awakenedAt = 5,
                )
            assertFalse(wrote)
            assertTrue(db.classAwakeningDao().ids(userId).isEmpty())
            assertFalse(repo.isAwake(userId, "assassin"))

            assertTrue(repo.awaken(userId, "assassin", AwakeningSource.BUILD_AFFINITY, awakenedAt = 8))
            val row = db.classAwakeningDao().observe(userId).first().single()
            assertEquals(AwakeningSource.BUILD_AFFINITY.name, row.source)
            assertEquals(8L, row.awakenedAt)
        }

    @Test
    fun `debug override writes only when the debug flag is true and stays idempotent`() =
        runTest {
            val repo = repo(isDebug = true)
            assertTrue(
                repo.awaken(userId, "assassin", AwakeningSource.DEBUG_OVERRIDE, evidenceJson = "lab", awakenedAt = 5),
            )
            assertFalse(
                repo.awaken(userId, "assassin", AwakeningSource.DEBUG_OVERRIDE, evidenceJson = "again", awakenedAt = 9),
            )
            val row = db.classAwakeningDao().observe(userId).first().single()
            assertEquals(AwakeningSource.DEBUG_OVERRIDE.name, row.source)
            assertEquals("lab", row.evidenceJson)
            assertEquals(5L, row.awakenedAt)
        }

    private fun repo(isDebug: Boolean): ClassAwakeningRepositoryImpl {
        return ClassAwakeningRepositoryImpl(
            db.classAwakeningDao(),
            db.classDao(),
            BuildFlags(isDebug = isDebug),
        )
    }
}
