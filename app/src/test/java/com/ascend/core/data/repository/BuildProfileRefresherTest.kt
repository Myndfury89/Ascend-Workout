package com.ascend.core.data.repository

import com.ascend.core.database.entity.WorkoutEntity
import com.ascend.core.database.relation.WorkoutWithSets
import com.ascend.core.model.WorkoutStatus
import org.junit.Assert.assertEquals
import org.junit.Test

/** The recompute-on-write signal must fire on completion, not on in-progress editing. */
class BuildProfileRefresherTest {
    private fun workout(
        id: String,
        status: WorkoutStatus,
        updatedAt: Long,
    ): WorkoutWithSets =
        WorkoutWithSets(
            workout =
                WorkoutEntity(
                    id = id,
                    userId = "u1",
                    title = id,
                    status = status.name,
                    performedAt = updatedAt,
                    createdAt = 0,
                    updatedAt = updatedAt,
                ),
            sets = emptyList(),
        )

    @Test
    fun `only completed workouts contribute to the signal`() {
        val signal = completedWorkoutSignal(listOf(workout("w1", WorkoutStatus.IN_PROGRESS, 100)))
        assertEquals(0 to 0L, signal)
    }

    @Test
    fun `completing a workout changes the signal`() {
        val before = completedWorkoutSignal(listOf(workout("w1", WorkoutStatus.IN_PROGRESS, 100)))
        val after = completedWorkoutSignal(listOf(workout("w1", WorkoutStatus.COMPLETED, 200)))
        assertEquals(0 to 0L, before)
        assertEquals(1 to 200L, after)
    }

    @Test
    fun `editing an in-progress workout does not change the signal`() {
        val completed = workout("w1", WorkoutStatus.COMPLETED, 100)
        val signalA = completedWorkoutSignal(listOf(completed))
        // A new in-progress workout is added and edited; the completed set is unchanged.
        val signalB = completedWorkoutSignal(listOf(completed, workout("w2", WorkoutStatus.IN_PROGRESS, 999)))
        assertEquals(signalA, signalB)
    }
}
