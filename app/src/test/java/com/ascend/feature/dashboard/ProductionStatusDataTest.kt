package com.ascend.feature.dashboard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.data.repository.PlayerRepositoryImpl
import com.ascend.core.data.repository.ProgressionRepositoryImpl
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.RankCalculator
import com.ascend.core.model.AttributeType
import com.ascend.core.model.XpSourceType
import kotlinx.coroutines.flow.first
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

/** Proves the production Status source reflects the real player repositories (not fake data). */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class ProductionStatusDataTest {
    private lateinit var db: AscendDatabase
    private lateinit var players: PlayerRepositoryImpl
    private lateinit var progression: ProgressionRepositoryImpl
    private val userId = com.ascend.core.common.LOCAL_USER_ID

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        players = PlayerRepositoryImpl(db, db.playerDao(), LevelCalculator())
        progression = ProgressionRepositoryImpl(db, db.playerDao(), db.xpDao(), db.attributeDao(), LevelCalculator(), RankCalculator())
        runBlocking {
            db.playerDao().upsertProfile(UserProfileEntity(id = userId, displayName = "Kaiden", createdAt = 0, updatedAt = 0))
            db.playerDao().upsertProgress(com.ascend.core.database.entity.PlayerProgressEntity(userId = userId, updatedAt = 0))
            db.playerDao().upsertStats(com.ascend.core.database.entity.PlayerStatsEntity(userId = userId, updatedAt = 0))
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `the production source exposes the real hunter name`() =
        runTest {
            val source = ProductionStatusData(players)
            source.userId()
            assertEquals("Kaiden", source.hunterName)
        }

    @Test
    fun `progression and stats flow from the real repositories`() =
        runTest {
            progression.awardXp(userId, amount = 500, sourceType = XpSourceType.QUEST_COMPLETION, sourceId = "q1", description = "Quest")
            progression.awardAttributes(userId, mapOf(AttributeType.STRENGTH to 12L), XpSourceType.QUEST_COMPLETION, "q1")

            val source = ProductionStatusData(players)
            source.userId()

            assertEquals(500L, source.observeProgress().first().lifetimeXp)
            assertTrue("real attribute growth is reflected", source.observeStats().first().asMap.getValue(AttributeType.STRENGTH) >= 12L)
        }
}
