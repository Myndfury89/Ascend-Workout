package com.ascend.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ascend.core.database.dao.AttributeDao
import com.ascend.core.database.dao.PlayerDao
import com.ascend.core.database.dao.QuestDao
import com.ascend.core.database.dao.XpDao
import com.ascend.core.database.entity.AttributeTransactionEntity
import com.ascend.core.database.entity.PlayerProgressEntity
import com.ascend.core.database.entity.PlayerStatsEntity
import com.ascend.core.database.entity.QuestEntity
import com.ascend.core.database.entity.QuestObjectiveEntity
import com.ascend.core.database.entity.QuestProgressEntryEntity
import com.ascend.core.database.entity.UserProfileEntity
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
    ],
    version = 1,
    exportSchema = true,
)
abstract class AscendDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao

    abstract fun xpDao(): XpDao

    abstract fun attributeDao(): AttributeDao

    abstract fun questDao(): QuestDao

    companion object {
        const val NAME = "ascend.db"
    }
}
