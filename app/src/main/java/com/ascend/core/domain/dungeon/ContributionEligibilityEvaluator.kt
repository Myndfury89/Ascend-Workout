package com.ascend.core.domain.dungeon

import com.ascend.core.model.DungeonPartyMember
import com.ascend.core.model.MemberActivityState
import javax.inject.Inject

/** Whether a member may contribute right now, with the resolved state and neutral messaging. */
data class ContributionEligibility(
    val resolvedState: MemberActivityState,
    val canContribute: Boolean,
    val message: String,
)

/**
 * Decides whether a member may contribute. A member contributes **only** while ACTIVE: with an
 * eligible workout live, qualifying recent activity within the timeout, not paused, no safety stop,
 * and still in the encounter. Going inactive stops future contribution but never removes what was
 * already earned. Messaging is deliberately neutral — inactivity and pauses are never shamed, and
 * the system never nudges anyone to push through a safety concern.
 */
class ContributionEligibilityEvaluator
    @Inject
    constructor(private val config: DungeonConfig) {
        constructor() : this(DungeonConfig())

        fun evaluate(
            member: DungeonPartyMember,
            workoutActive: Boolean,
            now: Long,
        ): ContributionEligibility {
            if (member.hasLeft) {
                return ContributionEligibility(MemberActivityState.INACTIVE, false, "Party member left the encounter.")
            }
            when (member.activityState) {
                MemberActivityState.SAFETY_PAUSED ->
                    return ContributionEligibility(MemberActivityState.SAFETY_PAUSED, false, "Party member contribution paused.")
                MemberActivityState.PAUSED ->
                    return ContributionEligibility(MemberActivityState.PAUSED, false, "Party member contribution paused.")
                MemberActivityState.DISCONNECTED ->
                    return ContributionEligibility(MemberActivityState.DISCONNECTED, false, "Party member reconnecting.")
                else -> Unit
            }
            if (!workoutActive) {
                return ContributionEligibility(MemberActivityState.INACTIVE, false, "Party member contribution paused.")
            }
            val elapsed = now - member.lastActivityAt
            val state =
                when {
                    elapsed <= config.idleWarningMillis -> MemberActivityState.ACTIVE
                    elapsed <= config.inactivityTimeoutMillis -> MemberActivityState.IDLE_WARNING
                    else -> MemberActivityState.INACTIVE
                }
            val message =
                when (state) {
                    MemberActivityState.ACTIVE -> "Contributing."
                    MemberActivityState.IDLE_WARNING -> "Waiting for the next set."
                    else -> "Party member contribution paused."
                }
            return ContributionEligibility(state, state == MemberActivityState.ACTIVE, message)
        }
    }
