package com.ascend.core.model

/**
 * Original Ascend rank ladder. Ordinal is the progression order. Rank thresholds
 * are computed by the progression engine (Phase 3) from a configurable blend of
 * level, lifetime XP, consistency, and attributes — never from a single metric.
 */
enum class Rank(val displayName: String) {
    INITIATE("Initiate"),
    IRON("Iron"),
    BRONZE("Bronze"),
    SILVER("Silver"),
    GOLD("Gold"),
    VANGUARD("Vanguard"),
    ASCENDANT("Ascendant"),
    APEX("Apex"),
    MYTHIC("Mythic"),
}
