package com.ascend.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Versioned, non-destructive migrations. Each bump adds a [Migration]; the CREATE
 * statements must match Room's generated schema exactly (validated by the schema
 * export + MigrationTest), so no SQL DEFAULT clauses are emitted for columns whose
 * defaults live only in the Kotlin constructor.
 */
object AscendMigrations {
    /** v1 -> v2: exercise catalog + workout sessions and their sets (Milestone 2). */
    val MIGRATION_1_2 =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercise` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `primaryAttribute` TEXT NOT NULL,
                        `measurementType` TEXT NOT NULL,
                        `defaultUnit` TEXT NOT NULL,
                        `isWeighted` INTEGER NOT NULL,
                        `preferredSetSize` INTEGER,
                        `isBuiltIn` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_category` ON `exercise` (`category`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_exercise_name` ON `exercise` (`name`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `notes` TEXT,
                        `difficulty` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `performedAt` INTEGER NOT NULL,
                        `durationSeconds` INTEGER,
                        `perceivedEffort` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_userId` ON `workout` (`userId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_performedAt` ON `workout` (`performedAt`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_status` ON `workout` (`status`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `workout_set` (
                        `id` TEXT NOT NULL,
                        `workoutId` TEXT NOT NULL,
                        `exerciseId` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `reps` INTEGER,
                        `weight` REAL,
                        `durationSeconds` INTEGER,
                        `distance` REAL,
                        `volume` REAL NOT NULL,
                        `unit` TEXT NOT NULL,
                        `completedAt` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`workoutId`) REFERENCES `workout`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(`exerciseId`) REFERENCES `exercise`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_set_workoutId` ON `workout_set` (`workoutId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_workout_set_exerciseId` ON `workout_set` (`exerciseId`)")
            }
        }

    /** v2 -> v3: the persisted ProgressionEventQueue (Status motion system). */
    val MIGRATION_2_3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `progression_event` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `batchId` TEXT NOT NULL,
                        `sequence` INTEGER NOT NULL,
                        `type` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `attributeType` TEXT,
                        `fromValue` INTEGER NOT NULL,
                        `toValue` INTEGER NOT NULL,
                        `label` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `consumedAt` INTEGER,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_progression_event_userId_consumedAt` " +
                        "ON `progression_event` (`userId`, `consumedAt`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_progression_event_batchId_sequence` " +
                        "ON `progression_event` (`batchId`, `sequence`)",
                )
            }
        }

    /** v3 -> v4: activity tags on exercises + the class system (selection + ledgers). */
    val MIGRATION_3_4 =
        object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Activity tags on exercises (comma-separated). Default keeps existing rows valid.
                db.execSQL("ALTER TABLE `exercise` ADD COLUMN `tags` TEXT NOT NULL DEFAULT ''")
                BUILTIN_EXERCISE_TAGS.forEach { (id, tags) ->
                    db.execSQL("UPDATE `exercise` SET `tags` = ? WHERE `id` = ?", arrayOf(tags, id))
                }

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `player_class` (
                        `userId` TEXT NOT NULL,
                        `primaryClassId` TEXT,
                        `secondaryClassId` TEXT,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `class_xp_transaction` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `classId` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_class_xp_transaction_userId_classId` " +
                        "ON `class_xp_transaction` (`userId`, `classId`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_class_xp_transaction_classId_transactionType_sourceType_sourceId` " +
                        "ON `class_xp_transaction` (`classId`, `transactionType`, `sourceType`, `sourceId`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `class_proficiency_transaction` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `proficiencyKey` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `transactionType` TEXT NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_class_proficiency_transaction_userId_proficiencyKey` " +
                        "ON `class_proficiency_transaction` (`userId`, `proficiencyKey`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_class_proficiency_transaction_proficiencyKey_transactionType_sourceType_sourceId` " +
                        "ON `class_proficiency_transaction` (`proficiencyKey`, `transactionType`, `sourceType`, `sourceId`)",
                )
            }
        }

    /** Tags for the built-in exercise catalog, applied to rows seeded before v4. */
    private val BUILTIN_EXERCISE_TAGS =
        mapOf(
            "ex-pushup" to "BODYWEIGHT,MUSCULAR_ENDURANCE",
            "ex-pullup" to "BODYWEIGHT,MUSCULAR_ENDURANCE",
            "ex-squat" to "BODYWEIGHT,MUSCULAR_ENDURANCE",
            "ex-lunge" to "BODYWEIGHT,BALANCE,MUSCULAR_ENDURANCE",
            "ex-situp" to "BODYWEIGHT,MUSCULAR_ENDURANCE",
            "ex-plank" to "BODYWEIGHT,BALANCE",
            "ex-burpee" to "HIGH_INTENSITY_CARDIO,EXPLOSIVE,BODYWEIGHT",
            "ex-jumpingjack" to "STEADY_STATE_CARDIO,BODYWEIGHT",
            "ex-run" to "STEADY_STATE_CARDIO",
            "ex-row" to "STEADY_STATE_CARDIO,HIGH_INTENSITY_CARDIO",
            "ex-benchpress" to "HEAVY_STRENGTH,HYPERTROPHY",
            "ex-deadlift" to "HEAVY_STRENGTH,EXPLOSIVE",
            "ex-backsquat" to "HEAVY_STRENGTH,HYPERTROPHY",
            "ex-jumprope" to "HIGH_INTENSITY_CARDIO,EXPLOSIVE",
            "ex-mobility" to "MOBILITY,RECOVERY",
            "ex-stretch" to "MOBILITY,RECOVERY",
        )

    /** v4 -> v5: a subject key on progression events (class id / proficiency key). */
    val MIGRATION_4_5 =
        object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `progression_event` ADD COLUMN `subjectKey` TEXT")
            }
        }

    /** All migrations, wired into the Room builder. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
}
