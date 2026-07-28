package com.ascend.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Validates the v1 -> v2 migration against the exported schemas on the JVM
 * (Robolectric, no emulator). Proves the workout tables are created correctly and
 * that pre-existing v1 data survives the upgrade.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class AscendMigrationTest {
    @get:Rule
    val helper =
        MigrationTestHelper(
            InstrumentationRegistry.getInstrumentation(),
            AscendDatabase::class.java,
            emptyList(),
            FrameworkSQLiteOpenHelperFactory(),
        )

    @Test
    fun `migrate 1 to 2 creates workout tables and preserves data`() {
        val dbName = "migration-test.db"

        // Seed a v1 database with a profile + quest.
        helper.createDatabase(dbName, 1).use { db ->
            db.execSQL(
                "INSERT INTO user_profile (id, displayName, createdAt, updatedAt, onboardingCompleted, " +
                    "measurementSystem, localOnly, cloudSyncEnabled) " +
                    "VALUES ('u1', 'Tester', 0, 0, 0, 'METRIC', 1, 0)",
            )
            db.execSQL(
                "INSERT INTO quest (id, userId, title, questType, difficulty, status, " +
                    "baseRewardXp, partialRewardEnabled, overCompletionEnabled, createdAt, updatedAt) " +
                    "VALUES ('q1', 'u1', '200 Push-Ups', 'ACCUMULATION', 'MODERATE', 'ACTIVE', 350, 1, 1, 0, 0)",
            )
        }

        // Run the migration; Room validates the resulting schema against 2.json.
        val db = helper.runMigrationsAndValidate(dbName, 2, true, AscendMigrations.MIGRATION_1_2)

        // The v1 row survived.
        db.query("SELECT title FROM quest WHERE id = 'q1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("200 Push-Ups", c.getString(0))
        }

        // The new tables are usable, including the exercise -> workout_set FK chain.
        db.execSQL(
            "INSERT INTO exercise (id, name, category, primaryAttribute, measurementType, defaultUnit, " +
                "isWeighted, isBuiltIn, createdAt, updatedAt) " +
                "VALUES ('ex-pushup', 'Push-ups', 'Bodyweight', 'STRENGTH', 'REPETITIONS', 'reps', 0, 1, 0, 0)",
        )
        db.execSQL(
            "INSERT INTO workout (id, userId, title, difficulty, status, performedAt, createdAt, updatedAt) " +
                "VALUES ('w1', 'u1', 'Session', 'MODERATE', 'IN_PROGRESS', 100, 0, 0)",
        )
        db.execSQL(
            "INSERT INTO workout_set (id, workoutId, exerciseId, orderIndex, reps, volume, unit, " +
                "completedAt, createdAt, updatedAt) " +
                "VALUES ('s1', 'w1', 'ex-pushup', 0, 20, 20.0, 'reps', 100, 0, 0)",
        )
        db.query("SELECT SUM(volume) FROM workout_set WHERE workoutId = 'w1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(20.0, c.getDouble(0), 0.0001)
        }
        db.close()
    }

    @Test
    fun `migrate 2 to 3 creates the progression event queue and preserves data`() {
        val dbName = "migration-test-2-3.db"

        helper.createDatabase(dbName, 2).use { db ->
            db.execSQL(
                "INSERT INTO user_profile (id, displayName, createdAt, updatedAt, onboardingCompleted, " +
                    "measurementSystem, localOnly, cloudSyncEnabled) " +
                    "VALUES ('u1', 'Tester', 0, 0, 0, 'METRIC', 1, 0)",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 3, true, AscendMigrations.MIGRATION_2_3)

        // The new queue table is usable and the FK to user_profile holds.
        db.execSQL(
            "INSERT INTO progression_event (id, userId, batchId, sequence, type, sourceType, sourceId, " +
                "fromValue, toValue, createdAt) " +
                "VALUES ('e1', 'u1', 'batch-1', 0, 'XP_GAINED', 'QUEST_COMPLETION', 'q1', 0, 350, 10)",
        )
        // The exactly‑once (batchId, sequence) guard is a unique index — a duplicate throws.
        var duplicateRejected = false
        try {
            db.execSQL(
                "INSERT INTO progression_event (id, userId, batchId, sequence, type, sourceType, sourceId, " +
                    "fromValue, toValue, createdAt) " +
                    "VALUES ('e2', 'u1', 'batch-1', 0, 'XP_GAINED', 'QUEST_COMPLETION', 'q1', 0, 350, 11)",
            )
        } catch (expected: android.database.sqlite.SQLiteConstraintException) {
            duplicateRejected = true
        }
        assertTrue("duplicate (batchId, sequence) is rejected", duplicateRejected)

        db.query("SELECT toValue FROM progression_event WHERE id = 'e1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(350L, c.getLong(0))
        }
        db.close()
    }

    @Test
    fun `migrate 3 to 4 adds tags and the class tables and backfills built-in tags`() {
        val dbName = "migration-test-3-4.db"

        helper.createDatabase(dbName, 3).use { db ->
            db.execSQL(
                "INSERT INTO user_profile (id, displayName, createdAt, updatedAt, onboardingCompleted, " +
                    "measurementSystem, localOnly, cloudSyncEnabled) " +
                    "VALUES ('u1', 'Tester', 0, 0, 0, 'METRIC', 1, 0)",
            )
            // A v3 exercise row that the migration should tag.
            db.execSQL(
                "INSERT INTO exercise (id, name, category, primaryAttribute, measurementType, defaultUnit, " +
                    "isWeighted, isBuiltIn, createdAt, updatedAt) " +
                    "VALUES ('ex-pushup', 'Push-ups', 'Bodyweight', 'STRENGTH', 'REPETITIONS', 'reps', 0, 1, 0, 0)",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 4, true, AscendMigrations.MIGRATION_3_4)

        // Existing exercise row was backfilled with tags.
        db.query("SELECT tags FROM exercise WHERE id = 'ex-pushup'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("BODYWEIGHT,MUSCULAR_ENDURANCE", c.getString(0))
        }

        // Class selection + ledgers are usable, and the exactly-once guard holds.
        db.execSQL("INSERT INTO player_class (userId, primaryClassId, updatedAt) VALUES ('u1', 'monk', 0)")
        db.execSQL(
            "INSERT INTO class_xp_transaction (id, userId, classId, amount, transactionType, sourceType, sourceId, createdAt) " +
                "VALUES ('c1', 'u1', 'monk', 240, 'AWARD', 'WORKOUT_COMPLETION', 'w1', 0)",
        )
        var rejected = false
        try {
            db.execSQL(
                "INSERT INTO class_xp_transaction (id, userId, classId, amount, transactionType, sourceType, sourceId, createdAt) " +
                    "VALUES ('c2', 'u1', 'monk', 240, 'AWARD', 'WORKOUT_COMPLETION', 'w1', 1)",
            )
        } catch (expected: android.database.sqlite.SQLiteConstraintException) {
            rejected = true
        }
        assertTrue("duplicate class XP for the same source is rejected", rejected)
        db.close()
    }

    @Test
    fun `migrate 4 to 5 adds the progression event subject key`() {
        val dbName = "migration-test-4-5.db"

        helper.createDatabase(dbName, 4).use { db ->
            db.execSQL(
                "INSERT INTO user_profile (id, displayName, createdAt, updatedAt, onboardingCompleted, " +
                    "measurementSystem, localOnly, cloudSyncEnabled) " +
                    "VALUES ('u1', 'Tester', 0, 0, 0, 'METRIC', 1, 0)",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 5, true, AscendMigrations.MIGRATION_4_5)

        // The new nullable subjectKey column carries a class event's class id.
        db.execSQL(
            "INSERT INTO progression_event (id, userId, batchId, sequence, type, sourceType, sourceId, " +
                "subjectKey, fromValue, toValue, createdAt) " +
                "VALUES ('e1', 'u1', 'b1', 0, 'CLASS_XP_GAINED', 'WORKOUT_COMPLETION', 'w1', 'monk', 0, 240, 0)",
        )
        db.query("SELECT subjectKey FROM progression_event WHERE id = 'e1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("monk", c.getString(0))
        }
        db.close()
    }

    @Test
    fun `migrate 5 to 6 adds class definitions, history, and the reward-type guard`() {
        val dbName = "migration-test-5-6.db"

        helper.createDatabase(dbName, 5).use { db ->
            db.execSQL(
                "INSERT INTO user_profile (id, displayName, createdAt, updatedAt, onboardingCompleted, " +
                    "measurementSystem, localOnly, cloudSyncEnabled) " +
                    "VALUES ('u1', 'Tester', 0, 0, 0, 'METRIC', 1, 0)",
            )
            // A v5 class-XP row (pre reward-type column).
            db.execSQL(
                "INSERT INTO class_xp_transaction (id, userId, classId, amount, transactionType, sourceType, sourceId, createdAt) " +
                    "VALUES ('c1', 'u1', 'monk', 240, 'AWARD', 'WORKOUT_COMPLETION', 'w1', 0)",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 6, true, AscendMigrations.MIGRATION_5_6)

        // Pre-existing ledger row survived and was backfilled with the default reward type.
        db.query("SELECT rewardType FROM class_xp_transaction WHERE id = 'c1'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals("CLASS_XP", c.getString(0))
        }

        // New tables are usable + the widened exactly-once guard holds.
        db.execSQL("INSERT INTO player_class (userId, primaryClassId, changeSource, updatedAt) VALUES ('u1', 'monk', 'MANUAL', 0)")
        db.execSQL(
            "INSERT INTO class_history (id, userId, classId, slot, startedAt, createdAt) " +
                "VALUES ('h1', 'u1', 'monk', 'PRIMARY', 0, 0)",
        )
        db.execSQL(
            "INSERT INTO class_definition (id, name, classTitle, description, fitnessIdentity, favoredWorkoutCategories, " +
                "favoredTags, primaryAttributes, secondaryAttributes, attributeMultipliers, uniqueProficiencyKey, " +
                "uniqueProficiencyName, favoredClassXpMultiplier, nonFavoredClassXpMultiplier, statusThemeKey, frameVariantKey, " +
                "accentTokenKey, proficiencyIconKey, idleEffectKey, progressionEffectKey, classQuestTemplateIds, " +
                "expeditionTemplateIds, achievementPathIds, titleIds, enabled, createdAt, updatedAt) " +
                "VALUES ('monk', 'Monk', 't', 'd', 'fi', '', 'BODYWEIGHT', 'DISCIPLINE', '', 'STRENGTH:1.25', 'BODY_MASTERY', " +
                "'Body Mastery', 1.3, 0.75, 'monk', 'balanced', 'aqua', 'body_mastery', 'i', 'p', '', '', '', '', 1, 0, 0)",
        )

        var rejected = false
        try {
            db.execSQL(
                "INSERT INTO class_xp_transaction (id, userId, classId, amount, transactionType, sourceType, " +
                    "sourceId, rewardType, createdAt) " +
                    "VALUES ('c2', 'u1', 'monk', 240, 'AWARD', 'WORKOUT_COMPLETION', 'w1', 'CLASS_XP', 1)",
            )
        } catch (expected: android.database.sqlite.SQLiteConstraintException) {
            rejected = true
        }
        assertTrue("duplicate (classId, ..., rewardType) is rejected", rejected)
        db.close()
    }

    @Test
    fun `migrate 6 to 7 adds the quest template table`() {
        val dbName = "migration-test-6-7.db"
        helper.createDatabase(dbName, 6).close()

        val db = helper.runMigrationsAndValidate(dbName, 7, true, AscendMigrations.MIGRATION_6_7)

        db.execSQL(
            "INSERT INTO quest_template (id, name, objectiveType, unit, minimumTarget, maximumTarget, defaultTarget, " +
                "targetStep, defaultQuickAddValues, defaultPreferredSetSize, minimumAllowedSetSize, maximumAllowedSetSize, " +
                "supportsAutomaticProgress, supportsManualProgress, supportedVariations, safetyWarningThreshold, baseRewardXp, " +
                "isBuiltIn, createdAt, updatedAt) " +
                "VALUES ('tmpl-pushups', 'Push-ups', 'REPETITIONS', 'reps', 25, 500, 100, 5, '10,25,50', 25, 10, 50, " +
                "0, 1, '', 300, 350, 1, 0, 0)",
        )
        db.query("SELECT maximumTarget FROM quest_template WHERE id = 'tmpl-pushups'").use { c ->
            assertTrue(c.moveToFirst())
            assertEquals(500L, c.getLong(0))
        }
        db.close()
    }

    @Test
    fun `migrate 7 to 8 adds the interval scheduling tables with the import guard`() {
        val dbName = "migration-test-7-8.db"
        helper.createDatabase(dbName, 7).use { db ->
            db.execSQL(
                "INSERT INTO user_profile (id, displayName, createdAt, updatedAt, onboardingCompleted, " +
                    "measurementSystem, localOnly, cloudSyncEnabled) VALUES ('u1', 'T', 0, 0, 0, 'METRIC', 1, 0)",
            )
            db.execSQL(
                "INSERT INTO quest (id, userId, title, questType, difficulty, status, baseRewardXp, " +
                    "partialRewardEnabled, overCompletionEnabled, createdAt, updatedAt) " +
                    "VALUES ('q1', 'u1', 'Q', 'ACCUMULATION', 'MODERATE', 'ACTIVE', 0, 1, 1, 0, 0)",
            )
        }

        val db = helper.runMigrationsAndValidate(dbName, 8, true, AscendMigrations.MIGRATION_7_8)

        db.execSQL(
            "INSERT INTO quest_interval (id, questId, title, scheduledStart, scheduledEnd, targetValue, currentValue, " +
                "isCumulative, status, orderIndex, reminderEnabled, createdAt, updatedAt) " +
                "VALUES ('i1', 'q1', 'Noon', 10, 20, 50.0, 0.0, 0, 'PENDING', 0, 1, 0, 0)",
        )
        db.execSQL(
            "INSERT INTO quest_interval_progress_entry (id, questIntervalId, questId, value, source, sourceApplication, " +
                "externalRecordId, completedAt, createdAt, updatedAt) " +
                "VALUES ('e1', 'i1', 'q1', 2000.0, 'HEALTH_CONNECT', 'com.health', 'rec-1', 0, 0, 0)",
        )
        var rejected = false
        try {
            db.execSQL(
                "INSERT INTO quest_interval_progress_entry (id, questIntervalId, questId, value, source, sourceApplication, " +
                    "externalRecordId, completedAt, createdAt, updatedAt) " +
                    "VALUES ('e2', 'i1', 'q1', 2000.0, 'HEALTH_CONNECT', 'com.health', 'rec-1', 1, 0, 0)",
            )
        } catch (expected: android.database.sqlite.SQLiteConstraintException) {
            rejected = true
        }
        assertTrue("duplicate imported interval record is rejected", rejected)
        db.close()
    }
}
