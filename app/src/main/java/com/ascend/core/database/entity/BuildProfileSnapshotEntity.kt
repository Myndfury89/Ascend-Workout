package com.ascend.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The cached read-only Build snapshot for a user (schema v17). The resolved characteristics and class
 * affinities are stored as a JSON payload so the presentation layer can read a computed result without
 * recomputing. This table is written only by the Build layer and never participates in progression.
 */
@Entity(tableName = "build_profile_snapshot")
data class BuildProfileSnapshotEntity(
    @PrimaryKey val userId: String,
    val computedAt: Long,
    val windowDays: Int,
    val overallConfidence: Double,
    val payloadJson: String,
)
