package com.ascend.core.model

/**
 * The five core RPG attributes. Ordering is stable and used for persistence and
 * display. Optional future attributes (Mobility, Power, Balance, Resilience) are
 * intentionally not part of this enum yet.
 */
enum class AttributeType(val displayName: String) {
    STRENGTH("Strength"),
    ENDURANCE("Endurance"),
    AGILITY("Agility"),
    DISCIPLINE("Discipline"),
    RECOVERY("Recovery"),
}
