package com.ascend.feature.dashboard

import com.ascend.core.domain.repository.PlayerRepository
import com.ascend.core.model.PlayerProgress
import com.ascend.core.model.PlayerStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The **production** Status domain source: real player progression and attributes from the
 * [PlayerRepository]. The animation/presentation layers are unchanged — the ViewModel observes the
 * same [StatusDataSource] contract it did against the prototype fake, so swapping to real data is a
 * pure data-source change. The persisted ProgressionEventQueue (already populated by live quest and
 * workout completions) is drained by the ViewModel exactly as before.
 */
@Singleton
class ProductionStatusData
    @Inject
    constructor(
        private val playerRepository: PlayerRepository,
    ) : StatusDataSource {
        private val mutex = Mutex()
        private val userIdFlow = MutableStateFlow<String?>(null)

        @Volatile private var cachedUserId: String? = null

        @Volatile private var cachedName: String = DEFAULT_NAME
        override val hunterName: String get() = cachedName

        override suspend fun userId(): String =
            cachedUserId ?: mutex.withLock {
                cachedUserId ?: run {
                    val uid = playerRepository.ensureLocalPlayer()
                    cachedName = playerRepository.displayName(uid)
                    cachedUserId = uid
                    userIdFlow.value = uid
                    uid
                }
            }

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun observeProgress(): Flow<PlayerProgress> =
            userIdFlow.filterNotNull().flatMapLatest { playerRepository.observeProgress(it) }.filterNotNull()

        @OptIn(ExperimentalCoroutinesApi::class)
        override fun observeStats(): Flow<PlayerStats> =
            userIdFlow.filterNotNull().flatMapLatest { playerRepository.observeStats(it) }.filterNotNull()

        private companion object {
            const val DEFAULT_NAME = "Hunter"
        }
    }

/**
 * The production stand-in for the prototype's fake completion trigger. Production earns progression
 * from real quest/workout completions — there is nothing to simulate — so this is a no-op that keeps
 * the ViewModel contract intact.
 */
@Singleton
class NoOpStatusSimulator
    @Inject
    constructor() : StatusSimulator {
        override suspend fun simulate(kind: SimulatedCompletion) = Unit

        override suspend fun reset() = Unit
    }
