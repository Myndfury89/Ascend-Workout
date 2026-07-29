package com.ascend.feature.dashboard

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Production wiring: the Status screen reads its domain state from the real,
 * repository‑backed [ProductionStatusData], and drains the persisted
 * ProgressionEventQueue (populated by live quest/workout completions). There is no
 * fake completion trigger in production, so the [StatusSimulator] is a no‑op. The
 * prototype fake ([FakeStatusData]) remains for its unit test only.
 */
@Module
@InstallIn(SingletonComponent::class)
object StatusModule {
    @Provides
    @Singleton
    fun statusDataSource(production: ProductionStatusData): StatusDataSource = production

    @Provides
    @Singleton
    fun statusSimulator(noOp: NoOpStatusSimulator): StatusSimulator = noOp
}
