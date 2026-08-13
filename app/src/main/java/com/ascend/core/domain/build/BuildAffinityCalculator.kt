package com.ascend.core.domain.build

import javax.inject.Inject

private const val SCORE_MAX = 100.0

/** One class's read-only resemblance to the player's current build. */
data class ClassAffinity(
    val buildClass: BuildClass,
    /** Resemblance among the characteristics that have evidence, renormalized so missing data can't deflate it (0..1). */
    val affinity: Double,
    /** How much of the class's full signature is actually backed by evidence (0..1). */
    val coverage: Double,
    /** How confidently this resemblance is asserted — a function of coverage and per-characteristic confidence (0..1). */
    val confidence: Double,
    /** True only when the class may be presented as a dominant/strongest affinity (coverage gate + defining characteristic OK). */
    val dominantEligible: Boolean,
) {
    val active: Boolean get() = buildClass.active
}

/**
 * The full read-only affinity result: every class ranked by resemblance, plus the single [dominant]
 * class (the strongest that is actually eligible — never a class propped up by one logged session).
 */
data class BuildAffinityResult(
    val ranked: List<ClassAffinity>,
    val dominant: ClassAffinity?,
)

/**
 * Turns a [BuildProfile] into per-class affinity + coverage. Read-only and pure — it grants nothing.
 *
 * Affinity and Coverage are computed separately (revised Decision 3): Affinity renormalizes among the
 * characteristics that carry evidence (states OK or ZERO), so missing data neither inflates nor
 * deflates it; Coverage measures how much of the class's signature is evidenced at all. A class can
 * only be [ClassAffinity.dominantEligible] if its Coverage clears the gate AND its defining
 * (top-weighted) characteristic is OK — so a strong renormalized score over sparse evidence can never
 * crown a class.
 */
class BuildAffinityCalculator
    @Inject
    constructor() {
        fun calculate(
            profile: BuildProfile,
            tuning: BuildTuning = BuildTuning(),
        ): BuildAffinityResult {
            val ranked =
                ClassBuildSignatures.ALL
                    .map { affinityFor(it, profile, tuning) }
                    .sortedByDescending { it.affinity }
            return BuildAffinityResult(ranked = ranked, dominant = ranked.firstOrNull { it.dominantEligible })
        }

        private fun affinityFor(
            signature: ClassBuildSignature,
            profile: BuildProfile,
            tuning: BuildTuning,
        ): ClassAffinity {
            var evidencedWeight = 0.0
            var weightedScore = 0.0
            var weightedConfidence = 0.0
            signature.weights.forEach { (characteristic, sig) ->
                val resolved = profile[characteristic] ?: return@forEach
                if (resolved.state == EvidenceState.OK || resolved.state == EvidenceState.ZERO) {
                    evidencedWeight += sig.weight
                    weightedScore += sig.weight * (resolved.score / SCORE_MAX)
                    weightedConfidence += sig.weight * resolved.confidence
                }
            }
            val affinity = if (evidencedWeight > 0.0) weightedScore / evidencedWeight else 0.0
            val coverage = if (signature.totalWeight > 0.0) evidencedWeight / signature.totalWeight else 0.0
            val meanConfidence = if (evidencedWeight > 0.0) weightedConfidence / evidencedWeight else 0.0
            val topOk = signature.topCharacteristic?.let { profile[it]?.state == EvidenceState.OK } ?: false
            return ClassAffinity(
                buildClass = signature.buildClass,
                affinity = affinity,
                coverage = coverage,
                confidence = coverage * meanConfidence,
                dominantEligible = coverage >= tuning.dominantCoverageGate && topOk,
            )
        }
    }
