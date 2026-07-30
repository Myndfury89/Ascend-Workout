package com.ascend.feature.dashboard

import com.ascend.core.model.AttributeType
import com.ascend.core.model.ProgressionEvent
import com.ascend.core.model.ProgressionEventType
import com.ascend.core.model.Rank
import com.ascend.feature.dashboard.prototype.AttributeLine
import com.ascend.feature.dashboard.prototype.RankTier
import com.ascend.feature.dashboard.prototype.StatusClassVariant
import com.ascend.feature.dashboard.prototype.StatusEventOverlay
import com.ascend.feature.dashboard.prototype.StatusOverlayKind
import com.ascend.feature.dashboard.prototype.StatusPrototypeData
import com.ascend.feature.dashboard.prototype.StatusPrototypeStateId

/**
 * Real class/specialization facts the ornate Status composition needs, resolved from the live
 * `ClassRepository` (primary + optional secondary). `variant` is [StatusClassVariant.NEUTRAL] when
 * the player has not chosen a class — in which case the composition renders no class ring, no class
 * bar, and no proficiency medallion.
 */
data class StatusClassInfo(
    val variant: StatusClassVariant,
    val classLevel: Int,
    val classXpInLevel: Int,
    val classXpForLevel: Int,
    val uniqueProficiency: Int,
    val secondaryVariant: StatusClassVariant?,
    val secondaryXpInLevel: Int?,
    val secondaryXpForLevel: Int?,
) {
    companion object {
        /** Neutral (no class chosen) — the sigil shows the unbound seal, no class layers. */
        val NEUTRAL =
            StatusClassInfo(
                variant = StatusClassVariant.NEUTRAL,
                classLevel = 0,
                classXpInLevel = 0,
                classXpForLevel = 0,
                uniqueProficiency = 0,
                secondaryVariant = null,
                secondaryXpInLevel = null,
                secondaryXpForLevel = null,
            )
    }
}

/**
 * Pure mapping from the **real** progression domain (plus the just-earned event batch) onto the
 * data model the approved ornate Status composition renders. No fake values are introduced: fields
 * the production panel does not yet surface (adaptive-training / daily-quest copy) are left blank
 * and are not drawn by [StatusScreen]. Keeping this a pure function makes the real→visual contract
 * unit-testable without Compose or repositories.
 */
object StatusComposition {
    /** Map a class id (as seeded in `ClassCatalog`) to its sigil identity. */
    fun variantOf(classId: String?): StatusClassVariant =
        when (classId) {
            "berserker" -> StatusClassVariant.BERSERKER
            "monk" -> StatusClassVariant.MONK
            "magician" -> StatusClassVariant.MAGICIAN
            else -> StatusClassVariant.NEUTRAL
        }

    /** The nine game ranks map 1:1 onto the nine sigil rank tiers (same order). */
    fun tierOf(rank: Rank): RankTier = RankTier.entries[rank.ordinal]

    /**
     * Derive the transient overlay + whether this batch is a "major" unlock (level/rank/class-level
     * up), used to pick the cinematic entrance. Picks the single most celebratory event present.
     */
    fun overlayFor(batch: List<ProgressionEvent>): StatusEventOverlay {
        if (batch.isEmpty()) return NONE_OVERLAY
        val types = batch.mapTo(HashSet()) { it.type }
        return when {
            ProgressionEventType.RANK_UP in types ->
                StatusEventOverlay(StatusOverlayKind.RANK_PROMOTION, "Rank promotion", "A new rank seal is forged")
            ProgressionEventType.LEVEL_UP in types ->
                StatusEventOverlay(StatusOverlayKind.PLAYER_LEVEL_UP, "Level up", "Your level rises")
            ProgressionEventType.CLASS_LEVEL_UP in types ->
                StatusEventOverlay(StatusOverlayKind.CLASS_LEVEL_UP, "Class level up", "Your class advances")
            ProgressionEventType.PROFICIENCY_GAINED in types ->
                StatusEventOverlay(StatusOverlayKind.PROFICIENCY, "Proficiency", "Class proficiency deepens")
            ProgressionEventType.ATTRIBUTE_CHANGED in types -> {
                val attr = batch.firstOrNull { it.type == ProgressionEventType.ATTRIBUTE_CHANGED }?.attribute
                StatusEventOverlay(
                    StatusOverlayKind.ATTRIBUTE,
                    "Attribute increase",
                    attr?.let { "${it.displayName} grew" } ?: "An attribute grew",
                    emphasizedAttribute = attr?.displayName,
                )
            }
            ProgressionEventType.CLASS_XP_GAINED in types ->
                StatusEventOverlay(StatusOverlayKind.CLASS_XP, "Class XP", "Class experience gained")
            ProgressionEventType.XP_GAINED in types ->
                StatusEventOverlay(StatusOverlayKind.PLAYER_XP, "XP gained", "Experience gained")
            else -> NONE_OVERLAY
        }
    }

    /** True when the batch crosses a level, class level, or rank — drives the cinematic entrance. */
    fun isMajorUnlock(batch: List<ProgressionEvent>): Boolean =
        batch.any {
            it.type == ProgressionEventType.LEVEL_UP ||
                it.type == ProgressionEventType.RANK_UP ||
                it.type == ProgressionEventType.CLASS_LEVEL_UP
        }

    fun map(
        hunterName: String,
        domain: StatusDomain,
        classInfo: StatusClassInfo,
        batch: List<ProgressionEvent>,
        reducedMotion: Boolean,
    ): StatusPrototypeData {
        val overlay = overlayFor(batch)
        val hasClass = classInfo.variant != StatusClassVariant.NEUTRAL
        val attributes =
            AttributeType.entries.map { type ->
                AttributeLine(
                    name = type.displayName,
                    value = (domain.attributes[type] ?: 0L).clampToInt(),
                    emphasized = type.displayName == overlay.emphasizedAttribute,
                )
            }
        val activeMedallion = attributes.indexOfFirst { it.emphasized }
        return StatusPrototypeData(
            stateId =
                if (overlay.kind == StatusOverlayKind.RANK_PROMOTION) {
                    StatusPrototypeStateId.RANK_PROMOTION
                } else {
                    StatusPrototypeStateId.STANDARD
                },
            variant = classInfo.variant,
            hunterName = hunterName,
            level = domain.level,
            rankLabel = domain.rank.displayName,
            trainingTier = domain.rank.ordinal + 1,
            title = classInfo.variant.classTitle,
            secondaryVariant = classInfo.secondaryVariant,
            playerXpInLevel = domain.currentLevelXp.clampToInt(),
            playerXpForLevel = domain.xpToNextLevel.clampToInt(),
            classLevel = classInfo.classLevel,
            classXpInLevel = classInfo.classXpInLevel,
            classXpForLevel = classInfo.classXpForLevel,
            secondaryClassXpInLevel = classInfo.secondaryXpInLevel,
            secondaryClassXpForLevel = classInfo.secondaryXpForLevel,
            uniqueProficiency = classInfo.uniqueProficiency,
            attributes = attributes,
            // Adaptive-training + daily-quest copy is not wired to production yet; left blank and
            // not drawn by the production panel (see StatusScreen).
            trainingFocus = "",
            pendingRecommendation = null,
            recentProgressionEvent = "",
            readinessState = "",
            recentPersonalRecord = null,
            questName = "",
            questProgress = 0,
            questTarget = 0,
            questIntervalState = null,
            overlay = overlay,
            reducedMotion = reducedMotion,
            majorUnlock = isMajorUnlock(batch),
            rankTier = tierOf(domain.rank),
            activeMedallionIndex = activeMedallion,
            showProficiencyMedallion = hasClass,
        )
    }

    private val NONE_OVERLAY = StatusEventOverlay(StatusOverlayKind.NONE, "", "")
}

/** Clamp a non-negative Long total to the Int range the visual model uses. */
internal fun Long.clampToInt(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
