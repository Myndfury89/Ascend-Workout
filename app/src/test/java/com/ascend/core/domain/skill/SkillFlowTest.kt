package com.ascend.core.domain.skill

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.ascend.core.data.repository.SkillRepositoryImpl
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.model.SkillEvidence
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

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SkillFlowTest {
    private lateinit var db: AscendDatabase
    private lateinit var repo: SkillRepositoryImpl
    private val userId = "u1"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AscendDatabase::class.java).allowMainThreadQueries().build()
        val config = SkillConfig()
        repo =
            SkillRepositoryImpl(
                db, db.skillDao(), SkillLevelCalculator(config), SkillProgressCalculator(config),
                SkillEligibilityEngine(config), SkillClassAffinityResolver(), TransferableSkillResolver(),
            )
        runBlocking {
            db.playerDao().upsertProfile(UserProfileEntity(id = userId, displayName = "T", createdAt = 0, updatedAt = 0))
        }
    }

    @After
    fun tearDown() = db.close()

    @Test
    fun `unlocking a skill is idempotent`() =
        runTest {
            assertTrue(repo.unlock(userId, SkillCatalog.PERCEPTION, listOf("15 min cardio"), "WORKOUT", "w1"))
            // A second unlock (even from a different source) does nothing.
            assertFalse(repo.unlock(userId, SkillCatalog.PERCEPTION, listOf("more cardio"), "WORKOUT", "w2"))
            val ps = repo.playerSkill(userId, SkillCatalog.PERCEPTION)!!
            assertTrue(ps.unlocked)
            assertEquals(1, db.skillDao().getUnlockEvents(userId).size)
        }

    @Test
    fun `duplicate skill XP from the same source is prevented`() =
        runTest {
            val first = repo.awardXp(userId, SkillCatalog.PERCEPTION, evidenceUnits = 1, "CARDIO", "s1", classAffinity = 0.0)
            assertTrue(first.awarded)
            assertTrue(first.amount > 0)

            val duplicate = repo.awardXp(userId, SkillCatalog.PERCEPTION, evidenceUnits = 1, "CARDIO", "s1", classAffinity = 0.0)
            assertFalse(duplicate.awarded)
            assertTrue(duplicate.alreadyAwarded)

            // XP was not double-counted.
            assertEquals(first.amount, repo.playerSkill(userId, SkillCatalog.PERCEPTION)!!.skillXp)
        }

    @Test
    fun `accumulating skill XP raises the skill level`() =
        runTest {
            repo.awardXp(userId, SkillCatalog.PERCEPTION, evidenceUnits = 1, "CARDIO", "s1", 0.0) // 40 xp -> level 1
            assertEquals(1, repo.playerSkill(userId, SkillCatalog.PERCEPTION)!!.level)
            val big = repo.awardXp(userId, SkillCatalog.PERCEPTION, evidenceUnits = 3, "CARDIO", "s2", 0.0) // +120 -> 160 xp
            assertTrue("crossing 100 XP should level up", big.leveledUp)
            assertEquals(2, repo.playerSkill(userId, SkillCatalog.PERCEPTION)!!.level)
        }

    @Test
    fun `evaluation reflects unlocked state and never blocks on class`() =
        runTest {
            repo.unlock(userId, SkillCatalog.STRENGTH_BOOST, listOf("PR"), "PR", "p1")
            // A Magician (cardio class) still sees Strength Boost as unlocked/eligible from the PR evidence.
            val results =
                repo.evaluate(
                    userId,
                    SkillEvidence(strengthPersonalRecords = 1),
                    com.ascend.core.domain.classes.ClassCatalog.MAGICIAN,
                )
            val strength = results.first { it.skillId == SkillCatalog.STRENGTH_BOOST }
            assertTrue(strength.unlocked)
            assertTrue(strength.eligible)
        }

    @Test
    fun `a class switch preserves transferable skills`() =
        runTest {
            repo.unlock(userId, SkillCatalog.BODY_AWARENESS, listOf("variation advance"), "AT", "v1")
            val removed = repo.applyClassSwitch(userId, newClassId = "magician")
            assertEquals("no transferable skill is removed", 0, removed)
            assertTrue(repo.playerSkill(userId, SkillCatalog.BODY_AWARENESS)!!.unlocked)
        }
}
