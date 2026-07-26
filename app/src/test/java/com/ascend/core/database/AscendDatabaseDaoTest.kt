package com.ascend.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.dao.QuestDao
import com.ascend.core.database.dao.XpDao
import com.ascend.core.database.entity.QuestEntity
import com.ascend.core.database.entity.QuestObjectiveEntity
import com.ascend.core.database.entity.QuestProgressEntryEntity
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.database.entity.XpTransactionEntity
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
class AscendDatabaseDaoTest {
    private lateinit var db: AscendDatabase
    private lateinit var xpDao: XpDao
    private lateinit var questDao: QuestDao

    private val userId = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        xpDao = db.xpDao()
        questDao = db.questDao()
        runBlocking {
            db.playerDao().upsertProfile(
                UserProfileEntity(id = userId, displayName = "Tester", createdAt = 0, updatedAt = 0),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `duplicate xp award for same source is blocked but reversal is allowed`() =
        runTest {
            val award =
                XpTransactionEntity(
                    id = "x1",
                    userId = userId,
                    amount = 350,
                    transactionType = "AWARD",
                    sourceType = "QUEST_COMPLETION",
                    sourceId = "q1",
                    description = "Quest done",
                    createdAt = 1,
                )
            val firstRow = xpDao.insertIgnoringDuplicates(award)
            val dupRow = xpDao.insertIgnoringDuplicates(award.copy(id = "x2"))

            assertTrue("first award inserts", firstRow > 0)
            assertEquals("duplicate award is ignored", -1L, dupRow)
            assertEquals("only one award counted", 350L, xpDao.totalXp(userId))

            // A reversal for the same source is a different transactionType, so it is allowed.
            val reversal =
                XpTransactionEntity(
                    id = "x3", userId = userId, amount = -350, transactionType = "REVERSAL",
                    sourceType = "QUEST_COMPLETION", sourceId = "q1", description = "Reversed",
                    reversedTransactionId = "x1", createdAt = 2,
                )
            val reversalRow = xpDao.insertIgnoringDuplicates(reversal)
            assertTrue("reversal inserts", reversalRow > 0)
            assertEquals("award + reversal nets to zero", 0L, xpDao.totalXp(userId))
        }

    @Test
    fun `accumulation sums uneven sets and recomputes after delete`() =
        runTest {
            seedPushUpQuest()

            insertSet("s1", value = 25.0)
            insertSet("s2", value = 25.0)
            insertSet("s3", value = 40.0)
            insertSet("s4", value = 30.0)

            assertEquals(120.0, questDao.sumProgress("obj1"), 0.0001)

            questDao.deleteProgressEntry("s3") // remove the 40
            assertEquals(80.0, questDao.sumProgress("obj1"), 0.0001)
        }

    @Test
    fun `duplicate imported record is blocked while manual entries are not`() =
        runTest {
            seedPushUpQuest()

            val imported =
                QuestProgressEntryEntity(
                    id = "i1", questId = "quest1", objectiveId = "obj1", value = 50.0,
                    source = "HEALTH_CONNECT", sourceApplication = "com.example.health",
                    externalRecordId = "rec-123", completedAt = 10, createdAt = 10, updatedAt = 10,
                )
            val firstRow = questDao.insertProgressEntry(imported)
            val dupRow = questDao.insertProgressEntry(imported.copy(id = "i2"))

            assertTrue(firstRow > 0)
            assertEquals("re-import of same external record blocked", -1L, dupRow)

            // Two manual entries (null source app / record id) are both allowed.
            val m1 =
                questDao.insertProgressEntry(
                    QuestProgressEntryEntity(
                        id = "m1",
                        questId = "quest1",
                        objectiveId = "obj1",
                        value = 10.0,
                        source = "MANUAL",
                        completedAt = 11,
                        createdAt = 11,
                        updatedAt = 11,
                    ),
                )
            val m2 =
                questDao.insertProgressEntry(
                    QuestProgressEntryEntity(
                        id = "m2",
                        questId = "quest1",
                        objectiveId = "obj1",
                        value = 10.0,
                        source = "MANUAL",
                        completedAt = 12,
                        createdAt = 12,
                        updatedAt = 12,
                    ),
                )
            assertTrue(m1 > 0 && m2 > 0)
            assertEquals(70.0, questDao.sumProgress("obj1"), 0.0001)
        }

    private suspend fun seedPushUpQuest() {
        questDao.upsertQuest(
            QuestEntity(
                id = "quest1",
                userId = userId,
                title = "200 Push-Ups",
                questType = "ACCUMULATION",
                baseRewardXp = 350,
                createdAt = 0,
                updatedAt = 0,
            ),
        )
        questDao.upsertObjective(
            QuestObjectiveEntity(
                id = "obj1",
                questId = "quest1",
                title = "Push-ups",
                objectiveType = "REPETITIONS",
                targetValue = 200.0,
                unit = "reps",
                preferredSetSize = 25,
            ),
        )
    }

    private suspend fun insertSet(
        id: String,
        value: Double,
    ) {
        questDao.insertProgressEntry(
            QuestProgressEntryEntity(
                id = id,
                questId = "quest1",
                objectiveId = "obj1",
                value = value,
                source = "MANUAL",
                completedAt = value.toLong(),
                createdAt = 0,
                updatedAt = 0,
            ),
        )
    }
}
