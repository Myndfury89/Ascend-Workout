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

    /** All migrations, wired into the Room builder. */
    val ALL: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
}
