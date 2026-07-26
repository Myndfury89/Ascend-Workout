package com.ascend.core.data.di

import com.ascend.core.data.repository.PlayerRepositoryImpl
import com.ascend.core.data.repository.ProgressionRepositoryImpl
import com.ascend.core.data.repository.QuestRepositoryImpl
import com.ascend.core.data.repository.WorkoutRepositoryImpl
import com.ascend.core.domain.repository.PlayerRepository
import com.ascend.core.domain.repository.ProgressionRepository
import com.ascend.core.domain.repository.QuestRepository
import com.ascend.core.domain.repository.WorkoutRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {
    @Binds
    abstract fun bindProgressionRepository(impl: ProgressionRepositoryImpl): ProgressionRepository

    @Binds
    abstract fun bindPlayerRepository(impl: PlayerRepositoryImpl): PlayerRepository

    @Binds
    abstract fun bindQuestRepository(impl: QuestRepositoryImpl): QuestRepository

    @Binds
    abstract fun bindWorkoutRepository(impl: WorkoutRepositoryImpl): WorkoutRepository
}
