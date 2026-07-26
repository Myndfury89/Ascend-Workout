package com.ascend.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.RankCalculator
import com.ascend.core.domain.repository.XpAwardResult
import com.ascend.core.model.AttributeType
import com.ascend.core.model.XpSourceType
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
class ProgressionRepositoryTest {

    private lateinit var db: AscendDatabase
    private lateinit var repo: ProgressionRepositoryImpl
    private val userId = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java)
            .allowMainThreadQueries().build()
        repo = ProgressionRepositoryImpl(
            db = db,
            playerDao = db.playerDao(),
            xpDao = db.xpDao(),
            attributeDao = db.attributeDao(),
            levelCalculator = LevelCalculator(),
            rankCalculator = RankCalculator(),
        )
        runBlocking {
            db.playerDao().upsertProfile(
                UserProfileEntity(id = userId, displayName = "Tester", createdAt = 0, updatedAt = 0),
            )
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `award xp exactly once and recompute level`() = runTest {
        val first = repo.awardXp(userId, 350, XpSourceType.QUEST_COMPLETION, "quest-1", "200 Push-Ups")
        assertTrue(first is XpAwardResult.Awarded)
        // 350 XP -> level 2 (needs 200 for L2), 150 into the level.
        val progress = repo.getProgress(userId)!!
        assertEquals(2, progress.level)
        assertEquals(350L, progress.lifetimeXp)
        assertTrue((first as XpAwardResult.Awarded).leveledUp)

        // Re-completing the same quest must not grant XP again.
        val duplicate = repo.awardXp(userId, 350, XpSourceType.QUEST_COMPLETION, "quest-1", "200 Push-Ups")
        assertEquals(XpAwardResult.Duplicate, duplicate)
        assertEquals(350L, repo.getProgress(userId)!!.lifetimeXp)
    }

    @Test
    fun `reversing xp recomputes progression downward`() = runTest {
        repo.awardXp(userId, 350, XpSourceType.QUEST_COMPLETION, "quest-1", "done")
        repo.reverseXp(userId, XpSourceType.QUEST_COMPLETION, "quest-1")

        val progress = repo.getProgress(userId)!!
        assertEquals(0L, progress.lifetimeXp)
        assertEquals(1, progress.level)

        // Reversing twice is a no-op (idempotent).
        val second = repo.reverseXp(userId, XpSourceType.QUEST_COMPLETION, "quest-1")
        assertEquals(XpAwardResult.Duplicate, second)
        assertEquals(0L, repo.getProgress(userId)!!.lifetimeXp)
    }

    @Test
    fun `attributes are granted once per source`() = runTest {
        val deltas = mapOf(AttributeType.STRENGTH to 30L, AttributeType.DISCIPLINE to 10L)
        repo.awardAttributes(userId, deltas, XpSourceType.QUEST_COMPLETION, "quest-1")
        repo.awardAttributes(userId, deltas, XpSourceType.QUEST_COMPLETION, "quest-1") // duplicate

        val stats = db.playerDao().getStats(userId)!!
        assertEquals(30L, stats.strength)
        assertEquals(10L, stats.discipline)
    }

    @Test
    fun `multiple level ups are handled in a single award`() = runTest {
        val result = repo.awardXp(userId, 5_000, XpSourceType.WORKOUT_COMPLETION, "w-1", "big session")
        val progress = repo.getProgress(userId)!!
        val expected = LevelCalculator().resolve(5_000)
        assertEquals(expected.level, progress.level)
        assertTrue(progress.level > 2)
        assertTrue((result as XpAwardResult.Awarded).leveledUp)
    }
}
