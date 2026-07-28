package com.ascend.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.dao.QuestIntervalDao
import com.ascend.core.database.entity.QuestEntity
import com.ascend.core.database.entity.QuestIntervalEntity
import com.ascend.core.database.entity.QuestIntervalProgressEntryEntity
import com.ascend.core.database.entity.UserProfileEntity
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class QuestIntervalDaoTest {
    private lateinit var db: AscendDatabase
    private lateinit var dao: QuestIntervalDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        dao = db.questIntervalDao()
        runBlocking {
            db.playerDao().upsertProfile(UserProfileEntity(id = "u1", displayName = "T", createdAt = 0, updatedAt = 0))
            db.questDao().upsertQuest(
                QuestEntity(id = "q1", userId = "u1", title = "200 Push-Ups", questType = "ACCUMULATION", createdAt = 0, updatedAt = 0),
            )
            dao.upsertInterval(
                QuestIntervalEntity(
                    id = "i1", questId = "q1", title = "Noon", scheduledStart = 10, scheduledEnd = 20,
                    targetValue = 50.0, currentValue = 0.0, isCumulative = false, status = "PENDING",
                    orderIndex = 0, reminderEnabled = true, completedAt = null, createdAt = 0, updatedAt = 0,
                ),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    private fun entry(
        id: String,
        value: Double,
        app: String? = null,
        record: String? = null,
    ) = QuestIntervalProgressEntryEntity(
        id = id, questIntervalId = "i1", questId = "q1", value = value, source = "MANUAL",
        sourceApplication = app, externalRecordId = record, completedAt = 0, createdAt = 0, updatedAt = 0,
    )

    @Test
    fun `multiple entries in one interval sum for interval and daily totals`() =
        runTest {
            dao.insertProgressEntry(entry("e1", 20.0))
            dao.insertProgressEntry(entry("e2", 15.0))
            dao.insertProgressEntry(entry("e3", 15.0))

            assertEquals(50.0, dao.sumIntervalProgress("i1"), 0.0001)
            assertEquals(50.0, dao.sumDailyProgress("q1"), 0.0001)
        }

    @Test
    fun `duplicate imported interval record is blocked while manual entries are not`() =
        runTest {
            val imported = entry("imp1", 2000.0, app = "com.health", record = "rec-1")
            assertTrue(dao.insertProgressEntry(imported) > 0)
            assertEquals(-1L, dao.insertProgressEntry(imported.copy(id = "imp2")))

            // Manual entries (null app/record) are never blocked.
            assertTrue(dao.insertProgressEntry(entry("m1", 10.0)) > 0)
            assertTrue(dao.insertProgressEntry(entry("m2", 10.0)) > 0)
            assertEquals(2020.0, dao.sumIntervalProgress("i1"), 0.0001)
        }
}
