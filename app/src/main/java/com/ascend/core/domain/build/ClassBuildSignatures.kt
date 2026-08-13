package com.ascend.core.domain.build

/** The seven classes an Ascend build can resemble. Only the first three are currently playable. */
enum class BuildClass(
    val id: String,
    val displayName: String,
    val active: Boolean,
) {
    BERSERKER("berserker", "Berserker", active = true),
    MONK("monk", "Monk", active = true),
    MAGE("mage", "Mage", active = true),
    ASSASSIN("assassin", "Assassin", active = false),
    FIGHTER("fighter", "Fighter", active = false),
    RANGER("ranger", "Ranger", active = false),
    GUARDIAN("guardian", "Guardian", active = false),
    ;

    companion object {
        fun fromId(id: String): BuildClass? = entries.firstOrNull { it.id == id }
    }
}

/** Where a signature weight came from — kept so tuning never loses why a weight exists (Artifact 4). */
enum class WeightSource {
    /** Derived from the class's legacy favoredTags. */
    TAG,

    /** Derived from a quantitative metric (pace, distance) that legacy tags cannot express. */
    METRIC,

    /** A deliberate product-level addition — an authorized delta. */
    PRODUCT,
}

/** One characteristic's weight within a class signature, with its provenance. */
data class SignatureWeight(
    val weight: Double,
    val sources: Set<WeightSource>,
)

/**
 * A class's signature over the six signature-eligible [BuildCharacteristic]s. Activity is
 * deliberately absent (it differentiates no class and would manufacture false coverage). A
 * characteristic a class does not care about is simply omitted (weight 0), so it never enters that
 * class's coverage.
 */
data class ClassBuildSignature(
    val buildClass: BuildClass,
    val weights: Map<BuildCharacteristic, SignatureWeight>,
) {
    val totalWeight: Double get() = weights.values.sumOf { it.weight }

    /** The single highest-weighted characteristic — the class's defining axis (deterministic on ties by insertion order). */
    val topCharacteristic: BuildCharacteristic? get() = weights.maxByOrNull { it.value.weight }?.key
}

/**
 * The approved P1 starting-hypothesis signatures (Activity removed, per the P1 greenlight). These are
 * tunable balance numbers, not final — the [SignatureWeight.sources] record why each weight exists so
 * they can be retuned without losing intent, and so this matrix never silently diverges from the
 * class-XP system it was reconciled against.
 */
object ClassBuildSignatures {
    private const val LOW = 0.2
    private const val MOD = 0.5
    private const val HIGH = 0.8
    private const val V_HIGH = 1.0

    private fun w(
        weight: Double,
        vararg sources: WeightSource,
    ): SignatureWeight = SignatureWeight(weight, sources.toSet())

    val ALL: List<ClassBuildSignature> =
        listOf(
            ClassBuildSignature(
                BuildClass.BERSERKER,
                mapOf(
                    BuildCharacteristic.STRENGTH to w(V_HIGH, WeightSource.TAG),
                    BuildCharacteristic.VERSATILITY to w(LOW, WeightSource.PRODUCT),
                    BuildCharacteristic.ENDURANCE to w(LOW, WeightSource.PRODUCT),
                    BuildCharacteristic.SPEED to w(LOW, WeightSource.METRIC),
                    BuildCharacteristic.RECOVERY to w(LOW, WeightSource.PRODUCT),
                ),
            ),
            ClassBuildSignature(
                BuildClass.MONK,
                mapOf(
                    BuildCharacteristic.STRENGTH to w(MOD, WeightSource.TAG),
                    BuildCharacteristic.VERSATILITY to w(V_HIGH, WeightSource.TAG),
                    BuildCharacteristic.ENDURANCE to w(MOD, WeightSource.TAG),
                    BuildCharacteristic.SPEED to w(MOD, WeightSource.METRIC),
                    BuildCharacteristic.DISTANCE to w(LOW, WeightSource.METRIC),
                    BuildCharacteristic.RECOVERY to w(HIGH, WeightSource.PRODUCT),
                ),
            ),
            ClassBuildSignature(
                BuildClass.MAGE,
                mapOf(
                    BuildCharacteristic.STRENGTH to w(LOW, WeightSource.TAG),
                    BuildCharacteristic.VERSATILITY to w(MOD, WeightSource.TAG),
                    BuildCharacteristic.ENDURANCE to w(V_HIGH, WeightSource.TAG),
                    BuildCharacteristic.SPEED to w(MOD, WeightSource.METRIC),
                    BuildCharacteristic.DISTANCE to w(HIGH, WeightSource.METRIC, WeightSource.PRODUCT),
                    BuildCharacteristic.RECOVERY to w(MOD, WeightSource.TAG),
                ),
            ),
            ClassBuildSignature(
                BuildClass.ASSASSIN,
                mapOf(
                    BuildCharacteristic.STRENGTH to w(MOD, WeightSource.TAG),
                    BuildCharacteristic.VERSATILITY to w(HIGH, WeightSource.TAG),
                    BuildCharacteristic.ENDURANCE to w(MOD, WeightSource.TAG),
                    BuildCharacteristic.SPEED to w(V_HIGH, WeightSource.METRIC, WeightSource.PRODUCT),
                    BuildCharacteristic.DISTANCE to w(MOD, WeightSource.METRIC),
                    BuildCharacteristic.RECOVERY to w(LOW, WeightSource.PRODUCT),
                ),
            ),
            ClassBuildSignature(
                BuildClass.FIGHTER,
                mapOf(
                    BuildCharacteristic.STRENGTH to w(HIGH, WeightSource.TAG),
                    BuildCharacteristic.VERSATILITY to w(V_HIGH, WeightSource.TAG),
                    BuildCharacteristic.ENDURANCE to w(HIGH, WeightSource.TAG),
                    BuildCharacteristic.SPEED to w(MOD, WeightSource.METRIC),
                    BuildCharacteristic.DISTANCE to w(LOW, WeightSource.METRIC),
                    BuildCharacteristic.RECOVERY to w(MOD, WeightSource.PRODUCT),
                ),
            ),
            ClassBuildSignature(
                BuildClass.RANGER,
                mapOf(
                    BuildCharacteristic.STRENGTH to w(LOW, WeightSource.TAG),
                    BuildCharacteristic.VERSATILITY to w(HIGH, WeightSource.TAG, WeightSource.PRODUCT),
                    BuildCharacteristic.ENDURANCE to w(V_HIGH, WeightSource.TAG),
                    BuildCharacteristic.SPEED to w(MOD, WeightSource.METRIC),
                    BuildCharacteristic.DISTANCE to w(V_HIGH, WeightSource.METRIC, WeightSource.PRODUCT),
                    BuildCharacteristic.RECOVERY to w(MOD, WeightSource.PRODUCT),
                ),
            ),
            ClassBuildSignature(
                BuildClass.GUARDIAN,
                mapOf(
                    BuildCharacteristic.STRENGTH to w(V_HIGH, WeightSource.TAG),
                    BuildCharacteristic.VERSATILITY to w(MOD, WeightSource.TAG),
                    BuildCharacteristic.ENDURANCE to w(MOD, WeightSource.TAG),
                    BuildCharacteristic.SPEED to w(LOW, WeightSource.METRIC),
                    BuildCharacteristic.DISTANCE to w(LOW, WeightSource.METRIC),
                    BuildCharacteristic.RECOVERY to w(HIGH, WeightSource.TAG),
                ),
            ),
        )

    fun of(buildClass: BuildClass): ClassBuildSignature = ALL.first { it.buildClass == buildClass }
}
