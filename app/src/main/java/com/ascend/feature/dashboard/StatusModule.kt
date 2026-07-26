package com.ascend.feature.dashboard

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Prototype wiring: the Status screen reads its domain state and its simulate
 * trigger from the same [FakeStatusData] singleton. Swapping to production is a
 * one‑module change — bind [StatusDataSource] to a real repository‑backed source
 * and drop the simulator.
 */
@Module
@InstallIn(SingletonComponent::class)
object StatusModule {
    @Provides
    @Singleton
    fun statusDataSource(fake: FakeStatusData): StatusDataSource = fake

    @Provides
    @Singleton
    fun statusSimulator(fake: FakeStatusData): StatusSimulator = fake
}
