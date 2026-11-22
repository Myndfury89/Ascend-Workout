package com.ascend.core.designsystem.sigil

/** The four sigil identities the Status screen and the Ornate sigil render (moved out of prototype in PR 5b). */
enum class StatusClassVariant(
    val displayName: String,
    val classTitle: String,
    val proficiencyName: String,
) {
    NEUTRAL("Unbound", "The Unawakened", "Potential"),
    BERSERKER("Berserker", "Breaker of Limits", "Force"),
    MONK("Monk", "Master of the Vessel", "Body Mastery"),
    MAGE("Mage", "Channeler of Vitality", "Energy Control"),
}
