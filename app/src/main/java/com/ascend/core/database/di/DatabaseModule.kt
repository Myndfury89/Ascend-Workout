package com.ascend.core.database.di

import android.content.Context
import androidx.room.Room
import com.ascend.core.database.AscendDatabase
import com.ascend.core.database.AscendMigrations
import com.ascend.core.database.dao.AdaptiveTrainingDao
import com.ascend.core.database.dao.AttributeDao
import com.ascend.core.database.dao.ClassDao
import com.ascend.core.database.dao.ExerciseDao
import com.ascend.core.database.dao.ExerciseVariationDao
import com.ascend.core.database.dao.PlayerDao
import com.ascend.core.database.dao.ProgressionEventDao
import com.ascend.core.database.dao.QuestDao
import com.ascend.core.database.dao.QuestIntervalDao
import com.ascend.core.database.dao.QuestTemplateDao
import com.ascend.core.database.dao.WorkoutDao
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
            .addMigrations(*AscendMigrations.ALL)
            .build()

    @Provides
    fun providePlayerDao(db: AscendDatabase): PlayerDao = db.playerDao()

    @Provides
    fun provideXpDao(db: AscendDatabase): XpDao = db.xpDao()

    @Provides
    fun provideAttributeDao(db: AscendDatabase): AttributeDao = db.attributeDao()

    @Provides
    fun provideQuestDao(db: AscendDatabase): QuestDao = db.questDao()

    @Provides
    fun provideExerciseDao(db: AscendDatabase): ExerciseDao = db.exerciseDao()

    @Provides
    fun provideWorkoutDao(db: AscendDatabase): WorkoutDao = db.workoutDao()

    @Provides
    fun provideProgressionEventDao(db: AscendDatabase): ProgressionEventDao = db.progressionEventDao()

    @Provides
    fun provideClassDao(db: AscendDatabase): ClassDao = db.classDao()

    @Provides
    fun provideQuestTemplateDao(db: AscendDatabase): QuestTemplateDao = db.questTemplateDao()

    @Provides
    fun provideQuestIntervalDao(db: AscendDatabase): QuestIntervalDao = db.questIntervalDao()

    @Provides
    fun provideAdaptiveTrainingDao(db: AscendDatabase): AdaptiveTrainingDao = db.adaptiveTrainingDao()

    @Provides
    fun provideExerciseVariationDao(db: AscendDatabase): ExerciseVariationDao = db.exerciseVariationDao()
}
