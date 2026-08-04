package com.ascend.core.model.onboarding

/*
 * Age-safety model. PROVISIONAL, U.S.-first product policy (minimum age 13) — must be reviewed by
 * legal counsel before public release or expansion to other jurisdictions. Four real bands are kept
 * SEPARATE so future jurisdictional / parental-consent / Play Age Signals logic can be added without
 * another redesign. Age is collected as coarse RANGES (least-precise option); only the derived
 * [AgeSafetyCategory] is persisted — never a birth year or date of birth.
 *
 * Age must NEVER gate classes, transferable Skills, rank potential, attributes, exercise identity,
 * cosmetic identity, or solo progression. It may affect ONLY safety limits, conservative initial
 * prescriptions, consent requirements, social-feature availability, safety messaging, and
 * recommendation aggressiveness.
 */

/** Privacy-conscious age buckets shown on the neutral age screen (no checkbox self-attestation). */
enum class AgeRange {
    UNDER_13,
    AGE_13_15,
    AGE_16_17,
    AGE_18_24,
    AGE_25_34,
    AGE_35_49,
    AGE_50_PLUS,
    PREFER_NOT_TO_SAY,
}

/** The persisted safety classification. Bands kept separate for future consent/jurisdiction logic. */
enum class AgeSafetyCategory { BELOW_MINIMUM, MINOR_YOUNGER, MINOR_OLDER, ADULT, NOT_PROVIDED }

/** Social visibility default. Everyone defaults [PRIVATE]; only adults may later opt into more. */
enum class SocialVisibility { PRIVATE, FRIENDS, PUBLIC }

/**
 * The social/presence defaults implied by an age category. Everyone starts fully private with party
 * presence and stranger discovery OFF; the difference is *eligibility to opt in later* — minors and
 * not-provided users cannot enable these at all in this policy.
 */
data class SocialPrivacyDefaults(
    val socialVisibility: SocialVisibility,
    val partyPresenceEnabled: Boolean,
    val strangerDiscoveryEnabled: Boolean,
    val mayEnableSocialFeatures: Boolean,
)

/** Pure age-safety policy. Minimum age 13; conservative defaults for minors and not-provided. */
object AgeSafetyPolicy {
    const val MINIMUM_AGE: Int = 13

    fun categoryFor(range: AgeRange): AgeSafetyCategory =
        when (range) {
            AgeRange.UNDER_13 -> AgeSafetyCategory.BELOW_MINIMUM
            AgeRange.AGE_13_15 -> AgeSafetyCategory.MINOR_YOUNGER
            AgeRange.AGE_16_17 -> AgeSafetyCategory.MINOR_OLDER
            AgeRange.AGE_18_24, AgeRange.AGE_25_34, AgeRange.AGE_35_49, AgeRange.AGE_50_PLUS -> AgeSafetyCategory.ADULT
            AgeRange.PREFER_NOT_TO_SAY -> AgeSafetyCategory.NOT_PROVIDED
        }

    /** Below-minimum users cannot complete onboarding or create any persisted profile/state. */
    fun canOnboard(category: AgeSafetyCategory): Boolean = category != AgeSafetyCategory.BELOW_MINIMUM

    fun isMinor(category: AgeSafetyCategory): Boolean =
        category == AgeSafetyCategory.MINOR_YOUNGER || category == AgeSafetyCategory.MINOR_OLDER

    /** Minors and not-provided users get conservative starting prescriptions. */
    fun usesConservativeDefaults(category: AgeSafetyCategory): Boolean = isMinor(category) || category == AgeSafetyCategory.NOT_PROVIDED

    /**
     * Defaults are private + presence/discovery OFF for everyone. Only [ADULT] may later opt in.
     * Turning 18 changes eligibility but never auto-enables anything — explicit opt-in is still
     * required (there is no path here that returns enabled=true).
     */
    fun defaultsFor(category: AgeSafetyCategory): SocialPrivacyDefaults =
        SocialPrivacyDefaults(
            socialVisibility = SocialVisibility.PRIVATE,
            partyPresenceEnabled = false,
            strangerDiscoveryEnabled = false,
            mayEnableSocialFeatures = category == AgeSafetyCategory.ADULT,
        )
}
