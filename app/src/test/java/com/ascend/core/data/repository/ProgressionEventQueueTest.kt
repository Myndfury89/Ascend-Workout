package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.model.AttributeType
import com.ascend.core.model.ProgressionEvent
import com.ascend.core.model.ProgressionEventType
import com.ascend.core.model.XpSourceType
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Proves the ProgressionEventQueue's contract: events persist, enqueue is
 * exactly‑once per (batchId, sequence), pending drains in play order, and marking a
 * batch consumed removes it — so a reward survives app death and replays once.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ProgressionEventQueueTest {
    private lateinit var db: AscendDatabase
    private lateinit var repo: ProgressionEventRepositoryImpl

    private val userId = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        repo = ProgressionEventRepositoryImpl(db, db.progressionEventDao())
        runBlocking {
            db.playerDao().upsertProfile(UserProfileEntity(id = userId, displayName = "Tester", createdAt = 0, updatedAt = 0))
        }
    }

    @After
    fun tearDown() = db.close()

    private fun event(
        batchId: String,
        sequence: Int,
        type: ProgressionEventType,
        attribute: AttributeType? = null,
        from: Long = 0,
        to: Long = 0,
    ) = ProgressionEvent(
        id = "$batchId-$sequence",
        userId = userId,
        batchId = batchId,
        sequence = sequence,
        type = type,
        sourceType = XpSourceType.QUEST_COMPLETION,
        sourceId = batchId,
        attribute = attribute,
        fromValue = from,
        toValue = to,
        createdAt = sequence.toLong(),
    )

    @Test
    fun `enqueue persists a batch and is exactly-once on re-enqueue`() =
        runTest {
            val batch =
                listOf(
                    event("b1", 0, ProgressionEventType.XP_GAINED, from = 100, to = 450),
                    event("b1", 1, ProgressionEventType.ATTRIBUTE_CHANGED, AttributeType.STRENGTH, 10, 22),
                )
            repo.enqueue(batch)
            // Re‑running the same earning transaction must not duplicate the queue.
            repo.enqueue(batch.map { it.copy(id = it.id + "-dupe") })

            assertEquals(2, db.progressionEventDao().count(userId))
            assertEquals(2, repo.getPending(userId).size)
        }

    @Test
    fun `pending drains in play order and consuming removes it`() =
        runTest {
            repo.enqueue(listOf(event("b1", 0, ProgressionEventType.XP_GAINED, from = 0, to = 100)))
            repo.enqueue(
                listOf(
                    event("b2", 0, ProgressionEventType.XP_GAINED, from = 100, to = 300),
                    event("b2", 1, ProgressionEventType.LEVEL_UP, from = 1, to = 2),
                ),
            )

            val pending = repo.getPending(userId)
            assertEquals(listOf("b1", "b2", "b2"), pending.map { it.batchId })

            // Consume the first batch; only the second remains.
            repo.markConsumed(listOf("b1-0"))
            val afterConsume = repo.getPending(userId)
            assertEquals(listOf("b2", "b2"), afterConsume.map { it.batchId })
            assertEquals(listOf(0, 1), afterConsume.map { it.sequence })
        }
}
