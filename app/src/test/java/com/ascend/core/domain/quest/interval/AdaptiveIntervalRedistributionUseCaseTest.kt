package com.ascend.core.domain.quest.interval

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.database.AscendDatabase
import com.ascend.core.model.AdaptiveIntervalAction
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
class AdaptiveIntervalRedistributionUseCaseTest {
    private lateinit var db: AscendDatabase
    private lateinit var useCase: AdaptiveIntervalRedistributionUseCase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        useCase = AdaptiveIntervalRedistributionUseCase(db.questIntervalDao(), QuestIntervalRedistributionEngine())
        val raw = db.openHelper.writableDatabase
        raw.execSQL(
            "INSERT INTO user_profile (id, displayName, createdAt, updatedAt, onboardingCompleted, " +
                "measurementSystem, localOnly, cloudSyncEnabled) VALUES ('u1', 'T', 0, 0, 0, 'METRIC', 1, 0)",
        )
        raw.execSQL(
            "INSERT INTO quest (id, userId, title, questType, difficulty, status, baseRewardXp, " +
                "partialRewardEnabled, overCompletionEnabled, createdAt, updatedAt) " +
                "VALUES ('q1', 'u1', 'Push-ups', 'ACCUMULATION', 'MODERATE', 'ACTIVE', 0, 1, 1, 0, 0)",
        )
        raw.execSQL(
            "INSERT INTO quest_interval_schedule (id, questId, scheduleMode, activeWindowStart, activeWindowEnd, " +
                "intervalCount, distributionStrategy, adaptiveRedistributionEnabled, redistributionPreference, createdAt, updatedAt) " +
                "VALUES ('s1', 'q1', 'FIXED_INTERVALS', 0, 100, 2, 'EQUAL', 1, 'EVEN', 0, 0)",
        )
        // Morning is past and only 30/50 done (20 leftover); afternoon is still ahead.
        raw.execSQL(
            "INSERT INTO quest_interval (id, questId, title, scheduledStart, scheduledEnd, targetValue, currentValue, " +
                "isCumulative, status, orderIndex, reminderEnabled, createdAt, updatedAt) " +
                "VALUES ('m', 'q1', 'Morning', 10, 20, 50.0, 30.0, 0, 'PARTIAL', 0, 1, 0, 0)",
        )
        raw.execSQL(
            "INSERT INTO quest_interval (id, questId, title, scheduledStart, scheduledEnd, targetValue, currentValue, " +
                "isCumulative, status, orderIndex, reminderEnabled, createdAt, updatedAt) " +
                "VALUES ('a', 'q1', 'Afternoon', 30, 40, 50.0, 0.0, 0, 'PENDING', 1, 1, 0, 0)",
        )
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `auto-safe adaptation redistributes the missed work and persists it`() =
        runTest {
            val params = AdaptiveIntervalParams(dailyTarget = 100, now = 25, dayEndMillis = 100)
            val rec = useCase.recommend("q1", params)

            assertEquals(AdaptiveIntervalAction.REDISTRIBUTE_EVENLY, rec.action)
            assertTrue("auto-safe adaptation should apply", rec.autoApplied)

            // The 20 leftover moved onto the afternoon interval and was written back.
            assertEquals(70.0, db.questIntervalDao().getInterval("a")!!.targetValue, 1e-9)
            // The daily objective is untouched.
            assertEquals(null, rec.proposedDailyTotal)
        }

    @Test
    fun `completing the daily total makes interval timing irrelevant`() =
        runTest {
            db.questIntervalDao().updateIntervalProgress("a", 70.0, "COMPLETED", 0)
            val params = AdaptiveIntervalParams(dailyTarget = 100, now = 25, dayEndMillis = 100)
            val rec = useCase.recommend("q1", params)
            assertEquals(AdaptiveIntervalAction.MAINTAIN_PLAN, rec.action)
        }
}
