package com.ascend.core.domain.dungeon

import com.ascend.core.model.DungeonDefinition
import com.ascend.core.model.DungeonEnemy
import com.ascend.core.model.EnemyPhase

/**
 * Original Ascend enemies and dungeons as configurable seed data. No copyrighted characters and no
 * named real-world deities — these are Ascend's own. [DungeonElements] are the element tags that
 * contributions carry and enemies are weak to (structural, not linguistic).
 */
object DungeonElements {
    const val PHYSICAL = "PHYSICAL"
    const val SUSTAINED = "SUSTAINED"
    const val CRITICAL = "CRITICAL"
    const val COMBO = "COMBO"
    const val TECHNIQUE = "TECHNIQUE"
    const val RESTORATIVE = "RESTORATIVE"
}

object DungeonCatalog {
    private fun twoPhase(enraged: Boolean = true) =
        listOf(
            EnemyPhase(0, "Opening", 1.0f, damageResistance = 0.0f),
            EnemyPhase(1, "Desperate", 0.4f, damageResistance = if (enraged) 0.15f else 0.0f, enraged = enraged),
        )

    val TUNNEL_SERPENT =
        DungeonEnemy(
            id = "enemy-tunnel-serpent",
            name = "Tunnel Serpent",
            baseHealth = 1200,
            armor = 40,
            weaknessTags = setOf(DungeonElements.PHYSICAL, DungeonElements.CRITICAL),
            phases = twoPhase(enraged = true),
            description = "A coiling burrower that recoils from raw force.",
        )

    val ORC_WARDEN =
        DungeonEnemy(
            id = "enemy-orc-warden",
            name = "Orc Warden",
            baseHealth = 2000,
            armor = 220,
            weaknessTags = setOf(DungeonElements.COMBO, DungeonElements.PHYSICAL),
            phases = twoPhase(enraged = true),
            description = "A heavily armored sentinel — break its guard with sustained combinations.",
        )

    val IRON_BEHEMOTH =
        DungeonEnemy(
            id = "enemy-iron-behemoth",
            name = "Iron Behemoth",
            baseHealth = 3400,
            armor = 320,
            weaknessTags = setOf(DungeonElements.SUSTAINED, DungeonElements.COMBO),
            phases = twoPhase(enraged = true),
            description = "A relentless colossus worn down only by sustained output.",
        )

    val ABYSSAL_LEVIATHAN =
        DungeonEnemy(
            id = "enemy-abyssal-leviathan",
            name = "Abyssal Leviathan",
            baseHealth = 5200,
            armor = 180,
            weaknessTags = setOf(DungeonElements.TECHNIQUE, DungeonElements.SUSTAINED),
            phases =
                listOf(
                    EnemyPhase(0, "Surfacing", 1.0f, 0.0f),
                    EnemyPhase(1, "Submerged", 0.6f, 0.2f),
                    EnemyPhase(2, "Fury", 0.25f, 0.1f, enraged = true),
                ),
            description = "A deep-dwelling terror; precise technique pierces its shifting guard.",
        )

    val CELESTIAL_CONSTRUCT =
        DungeonEnemy(
            id = "enemy-celestial-construct",
            name = "Celestial Construct",
            baseHealth = 4000,
            armor = 260,
            weaknessTags = setOf(DungeonElements.CRITICAL, DungeonElements.TECHNIQUE),
            phases = twoPhase(enraged = false),
            description = "A radiant automaton — decisive critical strikes shatter its core.",
        )

    val ENEMIES = listOf(TUNNEL_SERPENT, ORC_WARDEN, IRON_BEHEMOTH, ABYSSAL_LEVIATHAN, CELESTIAL_CONSTRUCT)

    val DUNGEONS: List<DungeonDefinition> =
        listOf(
            DungeonDefinition(
                "dungeon-serpent-tunnels",
                "Serpent Tunnels",
                tier = 1,
                enemies = listOf(TUNNEL_SERPENT),
                recommendedPartySize = 2,
                soloAllowed = true,
                baseRewardXp = 300,
                description = "A winding warren for a first delve.",
            ),
            DungeonDefinition(
                "dungeon-warden-gate",
                "Warden's Gate",
                tier = 2,
                enemies = listOf(ORC_WARDEN),
                recommendedPartySize = 3,
                soloAllowed = true,
                baseRewardXp = 500,
                description = "A guarded threshold that rewards coordination.",
            ),
            DungeonDefinition(
                "dungeon-iron-deep",
                "The Iron Deep",
                tier = 3,
                enemies = listOf(IRON_BEHEMOTH),
                recommendedPartySize = 3,
                soloAllowed = false,
                baseRewardXp = 700,
                description = "An endurance crucible.",
            ),
            DungeonDefinition(
                "dungeon-abyssal-trench",
                "Abyssal Trench",
                tier = 4,
                enemies = listOf(ABYSSAL_LEVIATHAN),
                recommendedPartySize = 4,
                soloAllowed = false,
                baseRewardXp = 1000,
                description = "The deep's apex predator.",
            ),
            DungeonDefinition(
                "dungeon-celestial-vault",
                "Celestial Vault",
                tier = 4,
                enemies = listOf(CELESTIAL_CONSTRUCT),
                recommendedPartySize = 4,
                soloAllowed = false,
                baseRewardXp = 950,
                description = "A radiant sanctum sealed by a construct.",
            ),
        )

    fun dungeonById(id: String?): DungeonDefinition? = DUNGEONS.firstOrNull { it.id == id }
}
