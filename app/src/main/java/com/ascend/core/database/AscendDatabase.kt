package com.ascend.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ascend.core.database.dao.AttributeDao
import com.ascend.core.database.dao.ClassDao
import com.ascend.core.database.dao.ExerciseDao
import com.ascend.core.database.dao.PlayerDao
import com.ascend.core.database.dao.ProgressionEventDao
import com.ascend.core.database.dao.QuestDao
import com.ascend.core.database.dao.WorkoutDao
import com.ascend.core.database.dao.XpDao
import com.ascend.core.database.entity.AttributeTransactionEntity
import com.ascend.core.database.entity.ClassProficiencyTransactionEntity
import com.ascend.core.database.entity.ClassXpTransactionEntity
import com.ascend.core.database.entity.ExerciseEntity
import com.ascend.core.database.entity.PlayerClassEntity
import com.ascend.core.database.entity.PlayerProgressEntity
import com.ascend.core.database.entity.PlayerStatsEntity
import com.ascend.core.database.entity.ProgressionEventEntity
import com.ascend.core.database.entity.QuestEntity
import com.ascend.core.database.entity.QuestObjectiveEntity
import com.ascend.core.database.entity.QuestProgressEntryEntity
import com.ascend.core.database.entity.UserProfileEntity
import com.ascend.core.database.entity.WorkoutEntity
import com.ascend.core.database.entity.WorkoutSetEntity
import com.ascend.core.database.entity.XpTransactionEntity

@Database(
    entities = [
        UserProfileEntity::class,
        PlayerProgressEntity::class,
        PlayerStatsEntity::class,
        XpTransactionEntity::class,
        AttributeTransactionEntity::class,
        QuestEntity::class,
        QuestObjectiveEntity::class,
        QuestProgressEntryEntity::class,
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutSetEntity::class,
        ProgressionEventEntity::class,
        PlayerClassEntity::class,
        ClassXpTransactionEntity::class,
        ClassProficiencyTransactionEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao

    abstract fun xpDao(): XpDao

    abstract fun attributeDao(): AttributeDao

    abstract fun questDao(): QuestDao

    abstract fun exerciseDao(): ExerciseDao

    abstract fun workoutDao(): WorkoutDao

    abstract fun progressionEventDao(): ProgressionEventDao

    abstract fun classDao(): ClassDao

    companion object {
        const val NAME = "ascend.db"
    }
}
