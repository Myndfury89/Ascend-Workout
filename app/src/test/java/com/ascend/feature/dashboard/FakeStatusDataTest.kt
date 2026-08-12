package com.ascend.feature.dashboard

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.data.repository.ProgressionEventRepositoryImpl
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.domain.progression.LevelCalculator
import com.ascend.core.domain.progression.ProgressionEventFactory
import com.ascend.core.domain.progression.RankCalculator
import com.ascend.core.domain.repository.PlayerRepository
import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import com.ascend.core.model.ProgressionEventType
import com.ascend.core.model.Rank
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
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
 * Proves the prototype's "earning transaction" behaves like production would: a
 * single [FakeStatusData.simulate] advances the domain **and** enqueues the full,
 * ordered event chain into the real persisted queue — so the ViewModel can drive
 * the animation purely from (domain + queue).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class FakeStatusDataTest {
    private lateinit var db: AscendDatabase
    private lateinit var eventRepo: ProgressionEventRepositoryImpl
    private lateinit var fake: FakeStatusData

    private val userId = "u1"

    private val fixedPlayer =
        object : PlayerRepository {
            override suspend fun ensureLocalPlayer(displayName: String): String = userId

            override suspend fun displayName(userId: String): String = "Tester"

            override fun observeProgress(userId: String): Flow<PlayerProgress?> = emptyFlow()

            override fun observeStats(userId: String): Flow<PlayerStats?> = emptyFlow()

            override suspend fun weightUnit(userId: String) = com.ascend.core.common.WeightUnit.KILOGRAMS

            override fun observeWeightUnit(userId: String) = emptyFlow<com.ascend.core.common.WeightUnit>()

            override suspend fun setWeightUnit(
                userId: String,
                unit: com.ascend.core.common.WeightUnit,
            ) = Unit

            override fun observeAvatarBodyBase(userId: String): Flow<String?> = emptyFlow()

            override suspend fun setAvatarBodyBase(
                userId: String,
                value: String?,
            ) = Unit
        }

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        eventRepo = ProgressionEventRepositoryImpl(db, db.progressionEventDao())
        fake = FakeStatusData(fixedPlayer, eventRepo, ProgressionEventFactory(), LevelCalculator(), RankCalculator())
        runBlocking {
            db.playerDao().upsertProfile(UserProfileEntity(id = userId, displayName = "Tester", createdAt = 0, updatedAt = 0))
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `baseline hunter starts at level 5 Iron`() =
        runTest {
            val progress = fake.observeProgress().first()
            assertEquals(5, progress.level)
            assertEquals(Rank.IRON, progress.rank)
            assertEquals(48L, fake.observeStats().first().strength)
        }

    @Test
    fun `a heavy completion advances the domain and enqueues the full chain`() =
        runTest {
            fake.simulate(SimulatedCompletion.HEAVY_QUEST)

            // Domain advanced: level‑up 5 -> 6, rank‑up Iron -> Bronze, Strength 48 -> 93.
            val progress = fake.observeProgress().first()
            assertEquals(6, progress.level)
            assertEquals(Rank.BRONZE, progress.rank)
            assertEquals(93L, fake.observeStats().first().strength)

            // The persisted queue holds the ordered animation script for that earning.
            val pending = eventRepo.getPending(userId)
            assertEquals(
                listOf(
                    ProgressionEventType.XP_GAINED,
                    ProgressionEventType.ATTRIBUTE_CHANGED,
                    ProgressionEventType.ATTRIBUTE_CHANGED,
                    ProgressionEventType.ATTRIBUTE_CHANGED,
                    ProgressionEventType.LEVEL_UP,
                    ProgressionEventType.RANK_UP,
                ),
                pending.map { it.type },
            )
            assertEquals(4_148L, pending.first().fromValue)
            assertEquals(5_048L, pending.first().toValue)
            assertEquals(Rank.BRONZE.ordinal.toLong(), pending.last().toValue)
        }

    @Test
    fun `a light completion stays in level with no level or rank beat`() =
        runTest {
            fake.simulate(SimulatedCompletion.LIGHT_QUEST)

            val progress = fake.observeProgress().first()
            assertEquals(5, progress.level)
            assertEquals(Rank.IRON, progress.rank)

            val types = eventRepo.getPending(userId).map { it.type }
            assertEquals(
                listOf(
                    ProgressionEventType.XP_GAINED,
                    ProgressionEventType.ATTRIBUTE_CHANGED,
                    ProgressionEventType.ATTRIBUTE_CHANGED,
                ),
                types,
            )
        }

    @Test
    fun `reset restores the baseline and drains pending events`() =
        runTest {
            fake.simulate(SimulatedCompletion.HEAVY_QUEST)
            fake.reset()

            assertEquals(5, fake.observeProgress().first().level)
            assertEquals(48L, fake.observeStats().first().strength)
            assertEquals(0, eventRepo.getPending(userId).size)
        }
}
