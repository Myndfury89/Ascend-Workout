package com.ascend.feature.dungeon.prototype

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.random.Random

/*
 * A FAKE, debug-only nearby-presence source. There is NO Bluetooth, no advertising/scanning, no
 * Nearby-Devices permission, no location, no precise distance, and no real account identity — every
 * presence is simulated with a rotating anonymous handle and a coarse signal category. This exists
 * only to review whether workout-driven party combat feels good; real proximity is a separate,
 * later product/legal/safety phase.
 */

/** A coarse, non-distance signal category. Exact distance is deliberately never exposed. */
enum class NearbySignal(val label: String) {
    VERY_NEAR("Very near"),
    NEARBY("Nearby"),
    UNSTABLE("Unstable"),
    UNKNOWN("Unknown"),
}

/**
 * A simulated nearby Ascended. [handle] is a rotating anonymous identifier (not a name, account id,
 * Bluetooth id, or location). [classId] is optional cosmetic flavor. No health or distance data.
 */
data class FakeNearbyPresence(
    val handle: String,
    val signal: NearbySignal,
    val classId: String?,
)

/** Source of simulated nearby presences. Debug-only; never backed by real discovery. */
interface NearbyAscendedProvider {
    val nearby: StateFlow<List<FakeNearbyPresence>>

    /** Rotate the anonymous handles and jitter the signal categories (simulated churn). */
    fun rotate()
}

/**
 * A deterministic fake provider. Seeds a handful of simulated presences and, on [rotate], swaps
 * their anonymous handles and coarse signals. Deterministic (seeded) so tests are stable.
 */
class FakeNearbyAscendedProvider(
    private val seed: Int = 1,
    initialCount: Int = 4,
) : NearbyAscendedProvider {
    private val rng = Random(seed)
    private val classes = listOf("berserker", "monk", "magician", null)
    private val _nearby = MutableStateFlow(List(initialCount) { generate(it) })
    override val nearby: StateFlow<List<FakeNearbyPresence>> = _nearby.asStateFlow()

    override fun rotate() {
        _nearby.value = _nearby.value.mapIndexed { i, _ -> generate(i) }
    }

    private fun generate(index: Int): FakeNearbyPresence =
        FakeNearbyPresence(
            handle = "Ascended-" + randomHandle(),
            signal = NearbySignal.entries[rng.nextInt(NearbySignal.entries.size)],
            classId = classes[index % classes.size],
        )

    private fun randomHandle(): String {
        val chars = "0123456789ABCDEF"
        return (0 until HANDLE_LEN).map { chars[rng.nextInt(chars.length)] }.joinToString("")
    }

    private companion object {
        const val HANDLE_LEN = 4
    }
}
