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

    /** v5 -> v6: DB-backed class definitions, class history, richer selection, reward-type guard. */
    val MIGRATION_5_6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `class_definition` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `classTitle` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `fitnessIdentity` TEXT NOT NULL,
                        `favoredWorkoutCategories` TEXT NOT NULL,
                        `favoredTags` TEXT NOT NULL,
                        `primaryAttributes` TEXT NOT NULL,
                        `secondaryAttributes` TEXT NOT NULL,
                        `attributeMultipliers` TEXT NOT NULL,
                        `uniqueProficiencyKey` TEXT NOT NULL,
                        `uniqueProficiencyName` TEXT NOT NULL,
                        `favoredClassXpMultiplier` REAL NOT NULL,
                        `nonFavoredClassXpMultiplier` REAL NOT NULL,
                        `statusThemeKey` TEXT NOT NULL,
                        `frameVariantKey` TEXT NOT NULL,
                        `accentTokenKey` TEXT NOT NULL,
                        `proficiencyIconKey` TEXT NOT NULL,
                        `idleEffectKey` TEXT NOT NULL,
                        `progressionEffectKey` TEXT NOT NULL,
                        `classQuestTemplateIds` TEXT NOT NULL,
                        `expeditionTemplateIds` TEXT NOT NULL,
                        `achievementPathIds` TEXT NOT NULL,
                        `titleIds` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `class_history` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `classId` TEXT NOT NULL,
                        `slot` TEXT NOT NULL,
                        `startedAt` INTEGER NOT NULL,
                        `endedAt` INTEGER,
                        `selectionReason` TEXT,
                        `changeSource` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_class_history_userId` ON `class_history` (`userId`)")

                // Richer selection fields.
                db.execSQL("ALTER TABLE `player_class` ADD COLUMN `primaryStartedAt` INTEGER")
                db.execSQL("ALTER TABLE `player_class` ADD COLUMN `secondaryStartedAt` INTEGER")
                db.execSQL("ALTER TABLE `player_class` ADD COLUMN `selectionReason` TEXT")
                db.execSQL("ALTER TABLE `player_class` ADD COLUMN `changeSource` TEXT")
                db.execSQL("ALTER TABLE `player_class` ADD COLUMN `cooldownUntil` INTEGER")
                db.execSQL("ALTER TABLE `player_class` ADD COLUMN `respecQuestId` TEXT")

                // Reward-type on the class ledgers; widen the exactly-once guard to include it.
                db.execSQL("ALTER TABLE `class_xp_transaction` ADD COLUMN `rewardType` TEXT NOT NULL DEFAULT 'CLASS_XP'")
                db.execSQL("DROP INDEX IF EXISTS `index_class_xp_transaction_classId_transactionType_sourceType_sourceId`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_class_xp_transaction_classId_transactionType_sourceType_sourceId_rewardType` " +
                        "ON `class_xp_transaction` (`classId`, `transactionType`, `sourceType`, `sourceId`, `rewardType`)",
                )
                db.execSQL(
                    "ALTER TABLE `class_proficiency_transaction` ADD COLUMN `rewardType` TEXT NOT NULL DEFAULT 'UNIQUE_PROFICIENCY'",
                )
                db.execSQL("DROP INDEX IF EXISTS `index_class_proficiency_transaction_proficiencyKey_transactionType_sourceType_sourceId`")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_class_proficiency_transaction_proficiencyKey_transactionType_sourceType_sourceId_rewardType` " +
                        "ON `class_proficiency_transaction` (`proficiencyKey`, `transactionType`, `sourceType`, `sourceId`, `rewardType`)",
                )
            }
        }

    /** v6 -> v7: customizable Daily Quest templates (configurable safe ranges). */
    val MIGRATION_6_7 =
        object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quest_template` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `objectiveType` TEXT NOT NULL,
                        `unit` TEXT NOT NULL,
                        `primaryAttribute` TEXT,
                        `exerciseId` TEXT,
                        `minimumTarget` INTEGER NOT NULL,
                        `maximumTarget` INTEGER NOT NULL,
                        `defaultTarget` INTEGER NOT NULL,
                        `targetStep` INTEGER NOT NULL,
                        `defaultQuickAddValues` TEXT NOT NULL,
                        `defaultPreferredSetSize` INTEGER NOT NULL,
                        `minimumAllowedSetSize` INTEGER NOT NULL,
                        `maximumAllowedSetSize` INTEGER NOT NULL,
                        `supportsAutomaticProgress` INTEGER NOT NULL,
                        `supportsManualProgress` INTEGER NOT NULL,
                        `supportedVariations` TEXT NOT NULL,
                        `safetyWarningThreshold` INTEGER NOT NULL,
                        `baseRewardXp` INTEGER NOT NULL,
                        `isBuiltIn` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
            }
        }

    /** v7 -> v8: Daily Quest interval scheduling (schedule, intervals, checkpoints, entries). */
    val MIGRATION_7_8 =
        object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quest_interval_schedule` (
                        `id` TEXT NOT NULL,
                        `questId` TEXT NOT NULL,
                        `scheduleMode` TEXT NOT NULL,
                        `activeWindowStart` INTEGER NOT NULL,
                        `activeWindowEnd` INTEGER NOT NULL,
                        `intervalCount` INTEGER NOT NULL,
                        `distributionStrategy` TEXT NOT NULL,
                        `adaptiveRedistributionEnabled` INTEGER NOT NULL,
                        `redistributionPreference` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`questId`) REFERENCES `quest`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_quest_interval_schedule_questId` " +
                        "ON `quest_interval_schedule` (`questId`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quest_interval` (
                        `id` TEXT NOT NULL,
                        `questId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `scheduledStart` INTEGER NOT NULL,
                        `scheduledEnd` INTEGER NOT NULL,
                        `targetValue` REAL NOT NULL,
                        `currentValue` REAL NOT NULL,
                        `isCumulative` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `reminderEnabled` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`questId`) REFERENCES `quest`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_interval_questId` ON `quest_interval` (`questId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quest_checkpoint` (
                        `id` TEXT NOT NULL,
                        `questId` TEXT NOT NULL,
                        `title` TEXT NOT NULL,
                        `targetValue` REAL NOT NULL,
                        `dueAt` INTEGER NOT NULL,
                        `isCumulative` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `orderIndex` INTEGER NOT NULL,
                        `completedAt` INTEGER,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`questId`) REFERENCES `quest`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_quest_checkpoint_questId` ON `quest_checkpoint` (`questId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `quest_interval_progress_entry` (
                        `id` TEXT NOT NULL,
                        `questIntervalId` TEXT NOT NULL,
                        `questId` TEXT NOT NULL,
                        `value` REAL NOT NULL,
                        `source` TEXT NOT NULL,
                        `sourceApplication` TEXT,
                        `externalRecordId` TEXT,
                        `completedAt` INTEGER NOT NULL,
                        `note` TEXT,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`questIntervalId`) REFERENCES `quest_interval`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_quest_interval_progress_entry_questIntervalId` " +
                        "ON `quest_interval_progress_entry` (`questIntervalId`)",
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_quest_interval_progress_entry_questId` " +
                        "ON `quest_interval_progress_entry` (`questId`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_quest_interval_progress_entry_sourceApplication_externalRecordId` " +
                        "ON `quest_interval_progress_entry` (`sourceApplication`, `externalRecordId`)",
                )
            }
        }

    /** v8 -> v9: Adaptive Training — prescriptions, readiness snapshots, recommendations, milestones. */
    val MIGRATION_8_9 =
        object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercise_prescription` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `exerciseId` TEXT NOT NULL,
                        `progressionStrategy` TEXT NOT NULL,
                        `targetSets` INTEGER,
                        `minimumReps` INTEGER,
                        `maximumReps` INTEGER,
                        `targetWeight` REAL,
                        `targetRestSeconds` INTEGER,
                        `targetDurationSeconds` INTEGER,
                        `targetDistance` REAL,
                        `targetPace` REAL,
                        `tempo` TEXT,
                        `variationId` TEXT,
                        `assistanceValue` REAL,
                        `effectiveFrom` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercise_prescription_userId_exerciseId_status` " +
                        "ON `exercise_prescription` (`userId`, `exerciseId`, `status`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `training_readiness_snapshot` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `exerciseId` TEXT,
                        `questTemplateId` TEXT,
                        `readinessState` TEXT NOT NULL,
                        `score` REAL NOT NULL,
                        `confidence` REAL NOT NULL,
                        `evidence` TEXT NOT NULL,
                        `positiveSignals` TEXT NOT NULL,
                        `limitingSignals` TEXT NOT NULL,
                        `missingSignals` TEXT NOT NULL,
                        `safetyState` TEXT NOT NULL,
                        `safetyFlags` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_training_readiness_snapshot_userId` " +
                        "ON `training_readiness_snapshot` (`userId`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `progression_recommendation` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `recommendationType` TEXT NOT NULL,
                        `exerciseId` TEXT,
                        `questTemplateId` TEXT,
                        `currentPrescriptionId` TEXT,
                        `proposedPrescriptionId` TEXT,
                        `proposedTarget` INTEGER,
                        `readinessState` TEXT NOT NULL,
                        `reason` TEXT NOT NULL,
                        `evidence` TEXT NOT NULL,
                        `confidence` REAL NOT NULL,
                        `safetyState` TEXT NOT NULL,
                        `requiresConfirmation` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `generatedAt` INTEGER NOT NULL,
                        `expiresAt` INTEGER NOT NULL,
                        `acceptedAt` INTEGER,
                        `rejectedAt` INTEGER,
                        `appliedAt` INTEGER,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_progression_recommendation_userId_status` " +
                        "ON `progression_recommendation` (`userId`, `status`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `progression_milestone` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `milestoneKey` TEXT NOT NULL,
                        `milestoneType` TEXT NOT NULL,
                        `exerciseId` TEXT,
                        `questTemplateId` TEXT,
                        `sourceRecommendationId` TEXT,
                        `previousValue` REAL NOT NULL,
                        `newValue` REAL NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_progression_milestone_userId` ON `progression_milestone` (`userId`)")
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_progression_milestone_milestoneKey` " +
                        "ON `progression_milestone` (`milestoneKey`)",
                )
            }
        }

    /** v9 -> v10: the bodyweight exercise‑variation graph (variations + directed edges). */
    val MIGRATION_9_10 =
        object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercise_variation` (
                        `id` TEXT NOT NULL,
                        `exerciseId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `difficultyTier` INTEGER NOT NULL,
                        `variationTags` TEXT NOT NULL,
                        `assistanceType` TEXT NOT NULL,
                        `assistanceValue` REAL,
                        `externalLoadSupported` INTEGER NOT NULL,
                        `rangeOfMotionLevel` INTEGER NOT NULL,
                        `tempoProfile` TEXT NOT NULL,
                        `enabled` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercise_variation_exerciseId` " +
                        "ON `exercise_variation` (`exerciseId`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `exercise_variation_edge` (
                        `id` TEXT NOT NULL,
                        `sourceVariationId` TEXT NOT NULL,
                        `destinationVariationId` TEXT NOT NULL,
                        `progressionType` TEXT NOT NULL,
                        `minimumSuccessfulExposures` INTEGER NOT NULL,
                        `minimumCompletedReps` INTEGER NOT NULL,
                        `minimumCompletedSets` INTEGER NOT NULL,
                        `maximumRpe` REAL,
                        `minimumRir` INTEGER,
                        `maximumAssistanceValue` REAL,
                        `requiredRangeOfMotion` INTEGER,
                        `requiredTempoControl` INTEGER NOT NULL,
                        `classUnlockRequirement` TEXT,
                        `safetyNotes` TEXT,
                        `enabled` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_exercise_variation_edge_sourceVariationId` " +
                        "ON `exercise_variation_edge` (`sourceVariationId`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_exercise_variation_edge_sourceVariationId_destinationVariationId_progressionType` " +
                        "ON `exercise_variation_edge` (`sourceVariationId`, `destinationVariationId`, `progressionType`)",
                )
            }
        }

    /** v10 -> v11: a persisted cardio prescription (duration/distance/pace/incline/resistance/intervals). */
    val MIGRATION_10_11 =
        object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cardio_prescription` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `exerciseId` TEXT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `targetDurationSeconds` INTEGER,
                        `targetDistanceMeters` REAL,
                        `targetPaceSecondsPerKm` REAL,
                        `targetSpeed` REAL,
                        `incline` REAL,
                        `resistance` REAL,
                        `intervalCount` INTEGER,
                        `workIntervalSeconds` INTEGER,
                        `restIntervalSeconds` INTEGER,
                        `targetHrZoneSeconds` INTEGER,
                        `effectiveFrom` INTEGER NOT NULL,
                        `status` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_cardio_prescription_userId_exerciseId_status` " +
                        "ON `cardio_prescription` (`userId`, `exerciseId`, `status`)",
                )
            }
        }

    /** v11 -> v12: Skills & Techniques — player skill state + idempotent progress + unlock log. */
    val MIGRATION_11_12 =
        object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `player_skill` (
                        `userId` TEXT NOT NULL,
                        `skillId` TEXT NOT NULL,
                        `unlocked` INTEGER NOT NULL,
                        `level` INTEGER NOT NULL,
                        `skillXp` INTEGER NOT NULL,
                        `unlockedAt` INTEGER,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`, `skillId`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_player_skill_userId` ON `player_skill` (`userId`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `skill_progress_transaction` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `skillId` TEXT NOT NULL,
                        `amount` INTEGER NOT NULL,
                        `sourceType` TEXT NOT NULL,
                        `sourceId` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_skill_progress_transaction_userId_skillId` " +
                        "ON `skill_progress_transaction` (`userId`, `skillId`)",
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS " +
                        "`index_skill_progress_transaction_userId_skillId_sourceType_sourceId` " +
                        "ON `skill_progress_transaction` (`userId`, `skillId`, `sourceType`, `sourceId`)",
                )

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `skill_unlock_event` (
                        `id` TEXT NOT NULL,
                        `userId` TEXT NOT NULL,
                        `skillId` TEXT NOT NULL,
                        `unlockedAt` INTEGER NOT NULL,
                        `triggeringSourceType` TEXT NOT NULL,
                        `triggeringSourceId` TEXT NOT NULL,
                        `evidence` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`),
                        FOREIGN KEY(`userId`) REFERENCES `user_profile`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent(),
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_skill_unlock_event_userId_skillId` " +
                        "ON `skill_unlock_event` (`userId`, `skillId`)",
                )
            }
        }

    /** v12 -> v13: a display weight-unit preference on the profile (canonical storage stays kg). */
    val MIGRATION_12_13 =
        object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `user_profile` ADD COLUMN `weightUnit` TEXT NOT NULL DEFAULT 'KILOGRAMS'")
            }
        }

    /** All migrations, wired into the Room builder. */
    val ALL: Array<Migration> =
        arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_10,
            MIGRATION_10_11,
            MIGRATION_11_12,
            MIGRATION_12_13,
        )
}
