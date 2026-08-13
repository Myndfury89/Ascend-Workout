package com.ascend.core.domain.build

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Source guard: the Build analysis layer is read-only. It may read training evidence and write its own
 * snapshot, but it must never reference a progression-mutation path.
 */
class BuildArchitectureTest {
    private fun buildLayerFiles(): List<File> =
        buildList {
            addAll(File("src/main/java/com/ascend/core/domain/build").walk().filter { it.isFile && it.extension == "kt" })
            add(File("src/main/java/com/ascend/core/data/repository/BuildProfileRepositoryImpl.kt"))
        }.filter { it.exists() }

    @Test
    fun `the build layer references no progression-mutation path`() {
        val files = buildLayerFiles()
        assertTrue("expected build-layer files to scan", files.isNotEmpty())
        val forbidden =
            listOf(
                "ProgressionRepository",
                "QuestRepository",
                "WorkoutRepository",
                "SkillRepository",
                "AttributeRepository",
                "ClassRepository",
                "awardXp",
                "awardAttribute",
                "unlockSkill",
                "completeQuest",
                "completeWorkout",
                "ProgressionEvent",
            )
        files.forEach { file ->
            val text = file.readText()
            forbidden.forEach { needle ->
                assertTrue(
                    "${file.name} must not reference a progression-mutation path ($needle) — Build is read-only",
                    !text.contains(needle),
                )
            }
        }
    }
}
