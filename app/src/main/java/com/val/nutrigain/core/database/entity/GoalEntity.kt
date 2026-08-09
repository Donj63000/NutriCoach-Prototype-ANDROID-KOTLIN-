// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "goal",
    indices = [
        Index(value = ["active"])
    ]
)
data class GoalEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "start_weight_kg")
    val startWeightKg: Double,
    @ColumnInfo(name = "target_weight_kg")
    val targetWeightKg: Double,
    @ColumnInfo(name = "start_date")
    val startDate: String,
    @ColumnInfo(name = "indicative_target_date")
    val indicativeTargetDate: String?,
    @ColumnInfo(name = "estimated_maintenance_calories")
    val estimatedMaintenanceCalories: Int?,
    @ColumnInfo(name = "daily_calorie_target")
    val dailyCalorieTarget: Int?,
    @ColumnInfo(name = "initial_surplus_calories")
    val initialSurplusCalories: Int?,
    @ColumnInfo(name = "target_gain_kg_per_week")
    val targetGainKgPerWeek: Double,
    @ColumnInfo(name = "pace")
    val pace: String,
    @ColumnInfo(name = "initial_bmi")
    val initialBmi: Double,
    @ColumnInfo(name = "active")
    val active: Boolean,
    @ColumnInfo(name = "calculation_version")
    val calculationVersion: String,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long
)
