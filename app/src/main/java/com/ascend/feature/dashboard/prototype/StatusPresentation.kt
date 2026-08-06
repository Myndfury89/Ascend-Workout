package com.ascend.feature.dashboard.prototype

/*
 * The ordered presentation for one drained ProgressionEventQueue batch. This is the seam that lets a
 * single quest-completion batch present as more than one beat WITHOUT a parallel event channel, a
 * second queue, or any reward mutation: the same persisted batch is drained once, mapped to an
 * ordered plan, played, and marked consumed. A non-quest batch is a single step — byte-identical to
 * the pre-existing single-overlay behaviour. A quest-sourced completion is Quest Complete first,
 * then the resulting Ascension beat (rank / player level / class level) if the same batch crossed
 * one — so the quest identity is preserved AND the Ascension is never suppressed.
 *
 * Everything here is derived only from data already on the events (type, sourceType, sourceId,
 * label, from/to deltas). No new schema, no recomputed rewards.
 */

/** One reward the quest awarded, read straight from an event's [from,to] delta. */
data class AttributeReward(
    val name: String,
    val amount: Int,
)

/**
 * The earned-progression summary for a completed quest, built only from the drained batch's deltas
 * (player XP gained + attribute gains). Identity comes from the event metadata: [questId] from
 * `sourceId`, [title] from the event `label`.
 */
data class QuestCompletionSummary(
    val questId: String,
    val title: String,
    val xpGained: Int,
    val attributeGains: List<AttributeReward>,
)

/** One beat in a batch's presentation. A quest step also carries the completion summary. */
data class StatusPresentationStep(
    val overlay: StatusEventOverlay,
    val questComplete: QuestCompletionSummary? = null,
)

/**
 * The ordered plan for one batch. [steps] are in play order (Quest Complete before any Ascension).
 */
data class StatusPresentationPlan(
    val steps: List<StatusPresentationStep>,
) {
    /** The quest completion in this batch, if any — drives the Quest Complete HUD window. */
    val questComplete: QuestCompletionSummary? get() = steps.firstNotNullOfOrNull { it.questComplete }

    /** The Ascension beat (rank / player level / class level) present in this batch, if any. */
    val ascensionOverlay: StatusEventOverlay? get() = steps.map { it.overlay }.firstOrNull { it.kind.isAscensionKind() }

    /**
     * The single overlay the panel renders (chip / flash / burst). The Ascension beat wins when
     * present so its cinematic is never lost; otherwise the batch's own overlay (Quest Complete for
     * a quest-only batch, or the pre-existing single overlay for a non-quest batch).
     */
    val panelOverlay: StatusEventOverlay get() = ascensionOverlay ?: steps.first().overlay
}

/** Ascension-tier overlays: the celebratory beats that must never be suppressed by a quest window. */
internal fun StatusOverlayKind.isAscensionKind(): Boolean =
    this == StatusOverlayKind.RANK_PROMOTION ||
        this == StatusOverlayKind.PLAYER_LEVEL_UP ||
        this == StatusOverlayKind.CLASS_LEVEL_UP
