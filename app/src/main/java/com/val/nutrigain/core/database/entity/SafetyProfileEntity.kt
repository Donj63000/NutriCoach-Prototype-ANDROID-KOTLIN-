// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "safety_profile")
data class SafetyProfileEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "unintentional_weight_loss")
    val unintentionalWeightLoss: Boolean,
    @ColumnInfo(name = "pregnant_or_breastfeeding")
    val pregnantOrBreastfeeding: Boolean,
    @ColumnInfo(name = "eating_disorder_history")
    val eatingDisorderHistory: Boolean,
    @ColumnInfo(name = "significant_digestive_symptoms")
    val significantDigestiveSymptoms: Boolean,
    @ColumnInfo(name = "relevant_medical_condition")
    val relevantMedicalCondition: Boolean,
    @ColumnInfo(name = "relevant_medication")
    val relevantMedication: Boolean,
    @ColumnInfo(name = "safety_level")
    val safetyLevel: String,
    @ColumnInfo(name = "acknowledged_at_epoch_ms")
    val acknowledgedAtEpochMs: Long,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long
)
