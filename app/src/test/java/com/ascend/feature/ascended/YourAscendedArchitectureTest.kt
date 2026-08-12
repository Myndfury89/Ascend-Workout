package com.ascend.feature.ascended

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Source guards for the Your Ascended production surface:
 *  - production code must NEVER depend on prototype code (the allowed direction is prototype -> production);
 *  - the read-only presentation surface must not reference any progression-mutation path.
 */
class YourAscendedArchitectureTest {
    private val root = File("src/main/java/com/ascend/feature/ascended")

    /** The production Ascended files — the model + presentation packages and the two top-level files. */
    private fun productionFiles(): List<File> =
        buildList {
            addAll(File(root, "model").walk().filter { it.isFile && it.extension == "kt" })
            addAll(File(root, "presentation").walk().filter { it.isFile && it.extension == "kt" })
            add(File(root, "YourAscendedViewModel.kt"))
            add(File(root, "YourAscendedScreen.kt"))
        }.filter { it.exists() }

    @Test
    fun `production ascended code never imports prototype code`() {
        val files = productionFiles()
        assertTrue("expected to find production ascended files to scan", files.isNotEmpty())
        val forbidden = listOf("feature.ascended.prototype", "feature.dashboard.prototype")
        files.forEach { file ->
            val text = file.readText()
            forbidden.forEach { needle ->
                assertTrue(
                    "${file.name} must not reference prototype code ($needle) — production must not depend on prototype",
                    !text.contains(needle),
                )
            }
        }
    }

    @Test
    fun `production ascended code references no progression-mutation path`() {
        val files = productionFiles()
        val forbidden =
            listOf(
                "ProgressionRepository",
                "QuestRepository",
                "WorkoutRepository",
                "SkillRepository",
                "AttributeRepository",
                "ReadinessRepository",
                "ClassProgressionRepository",
                "awardXp",
                "awardAttribute",
                "unlockSkill",
                "completeQuest",
                "completeWorkout",
                // The cosmetic body base is chosen by the player and must never be derived from the
                // physiological onboarding datum.
                "physiologySex",
            )
        files.forEach { file ->
            val text = file.readText()
            forbidden.forEach { needle ->
                assertTrue(
                    "${file.name} must not reference a progression-mutation path ($needle) — Your Ascended is read-only",
                    !text.contains(needle),
                )
            }
        }
    }
}
