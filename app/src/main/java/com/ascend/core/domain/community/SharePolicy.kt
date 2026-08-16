package com.ascend.core.domain.community

/**
 * The ONLY fields that can ever be exposed to another user. This is a closed allowlist: it contains
 * exclusively fantasy/progression-facing, derived presentation data. Health/raw telemetry (weight,
 * body composition, sleep, HRV, resting/live heart rate, active calories, raw Health Connect / Samsung
 * Health records, precise location, detailed raw workout evidence, recovery telemetry, and the Build
 * Characteristic evidence behind an affinity) have NO constant here — so they cannot be named as
 * exposable, and any new field is non-shareable until deliberately added. Fail closed by construction.
 */
enum class ShareableField {
    DISPLAY_NAME,
    HANDLE,
    AVATAR,
    SELECTED_CLASS,
    RANK,
    CLASS_LEVEL,
    OVERALL_LEVEL,
    BUILD_IDENTITY,
    CLASS_AFFINITY_SUMMARY,
}

/**
 * Everything [SharePolicy] needs to decide exposability for a (viewer → target) pair. Carries the
 * resolved [RelationshipState] (which already encodes block precedence), the target's consent
 * [ShareSettings], and both parties' social eligibility (minors are never eligible).
 */
data class ShareContext(
    val relationship: RelationshipState,
    val targetSettings: ShareSettings,
    val targetSocialEligible: Boolean,
    val viewerSocialEligible: Boolean,
)

/**
 * Single source of truth for what one user may see of another — security-relevant domain logic, not UI
 * filtering. The UI and the data layer must both consult this; neither may invent its own rules.
 *
 * Order of enforcement (all fail closed):
 *  1. A profile is visible at all only when the pair are [RelationshipState.Friends] (so any block,
 *     which resolves to Blocked/BlockedBy, denies everything), BOTH users are socially eligible, and
 *     the target's visibility is FRIENDS.
 *  2. Given visibility, each field is exposable only if it is on the allowlist AND its consent toggle
 *     is on. Identity (name/handle/avatar) is the baseline friend view; class progress and build
 *     identity are gated by their respective toggles.
 */
object SharePolicy {
    /** Can the viewer see the target's profile at all? Any block, non-friend, ineligibility, or non-FRIENDS visibility → false. */
    fun canViewProfile(ctx: ShareContext): Boolean =
        ctx.relationship == RelationshipState.Friends &&
            ctx.targetSocialEligible &&
            ctx.viewerSocialEligible &&
            ctx.targetSettings.visibility == ShareVisibility.FRIENDS

    /** Is a specific allowlisted field exposable in this context? */
    fun isExposable(
        field: ShareableField,
        ctx: ShareContext,
    ): Boolean {
        if (!canViewProfile(ctx)) return false
        return when (field) {
            ShareableField.DISPLAY_NAME, ShareableField.HANDLE, ShareableField.AVATAR -> true
            ShareableField.SELECTED_CLASS,
            ShareableField.RANK,
            ShareableField.CLASS_LEVEL,
            ShareableField.OVERALL_LEVEL,
            -> ctx.targetSettings.shareClassProgress
            ShareableField.BUILD_IDENTITY, ShareableField.CLASS_AFFINITY_SUMMARY -> ctx.targetSettings.shareBuildIdentity
        }
    }

    /**
     * Redact a fully-populated snapshot down to only what [ctx] permits. Returns null when the profile
     * is not viewable at all (block / non-friend / ineligible / PRIVATE), so callers can't accidentally
     * render a stale or unauthorized profile.
     */
    fun redact(
        full: SharedProfile,
        ctx: ShareContext,
    ): SharedProfile? {
        if (!canViewProfile(ctx)) return null
        val classProgress = isExposable(ShareableField.CLASS_LEVEL, ctx)
        val buildIdentity = isExposable(ShareableField.BUILD_IDENTITY, ctx)
        return SharedProfile(
            userId = full.userId,
            selectedClass = full.selectedClass.takeIf { classProgress },
            classLevel = full.classLevel.takeIf { classProgress },
            rank = full.rank.takeIf { classProgress },
            overallLevel = full.overallLevel.takeIf { classProgress },
            buildIdentity = full.buildIdentity.takeIf { buildIdentity },
            topAffinities = if (buildIdentity) full.topAffinities else emptyList(),
            // Avatar is the cosmetic baseline of the friend view (no separate toggle), safe among friends.
            avatarBodyBase = full.avatarBodyBase,
        )
    }
}
