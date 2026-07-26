package com.ascend.core.database.di

import android.content.Context
import androidx.room.Room
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.dao.AttributeDao
import com.ascend.core.database.dao.PlayerDao
import com.ascend.core.database.dao.QuestDao
import com.ascend.core.database.dao.XpDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AscendDatabase =
        Room.databaseBuilder(context, AscendDatabase::class.java, AscendDatabase.NAME)
            // Real, versioned migrations are added per phase; no destructive fallback.
            .build()

    @Provides
    fun providePlayerDao(db: AscendDatabase): PlayerDao = db.playerDao()

    @Provides
    fun provideXpDao(db: AscendDatabase): XpDao = db.xpDao()

    @Provides
    fun provideAttributeDao(db: AscendDatabase): AttributeDao = db.attributeDao()

    @Provides
    fun provideQuestDao(db: AscendDatabase): QuestDao = db.questDao()
}
