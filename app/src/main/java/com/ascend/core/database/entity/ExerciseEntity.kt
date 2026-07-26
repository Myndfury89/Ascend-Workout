package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A movement in the exercise library (push-ups, back squat, plank, ...). The
 * catalog is seeded content, referenced by [WorkoutSetEntity.exerciseId]. Custom
 * user exercises reuse this table with [isBuiltIn] = false.
 */
@Entity(
    tableName = "exercise",
    indices = [
        Index(value = ["category"]),
        Index(value = ["name"]),
    ],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    // The attribute this movement primarily trains (e.g. push-ups -> STRENGTH).
    val primaryAttribute: String,
    // How a set of this movement is measured (REPETITIONS, DURATION, DISTANCE, ...).
    val measurementType: String,
    val defaultUnit: String,
    // True when the movement is normally loaded with external weight.
    val isWeighted: Boolean = false,
    val preferredSetSize: Int? = null,
    // Data-driven activity tags (comma-separated), read by the class affinity engine.
    val tags: String = "",
    val isBuiltIn: Boolean = true,
    val createdAt: Long,
    val updatedAt: Long,
)
