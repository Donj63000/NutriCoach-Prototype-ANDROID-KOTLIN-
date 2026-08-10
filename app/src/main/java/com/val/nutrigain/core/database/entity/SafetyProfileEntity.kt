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
    val unintentionalWeightLoss: String,
    @ColumnInfo(name = "reduced_appetite")
    val reducedAppetite: String,
    @ColumnInfo(
        name = "swallowing_difficulty_or_persistent_vomiting"
    )
    val swallowingDifficultyOrPersistentVomiting: String,
    @ColumnInfo(name = "pregnant_or_breastfeeding")
    val pregnantOrBreastfeeding: String,
    @ColumnInfo(name = "eating_disorder_history")
    val eatingDisorderHistory: String,
    @ColumnInfo(name = "significant_digestive_symptoms")
    val significantDigestiveSymptoms: String,
    @ColumnInfo(name = "relevant_medical_condition")
    val relevantMedicalCondition: String,
    @ColumnInfo(name = "relevant_medication")
    val relevantMedication: String,
    @ColumnInfo(name = "food_allergies_or_intolerances")
    val foodAllergiesOrIntolerances: String,
    @ColumnInfo(name = "medical_context_note")
    val medicalContextNote: String?,
    @ColumnInfo(name = "safety_level")
    val safetyLevel: String,
    @ColumnInfo(name = "assessment_reasons")
    val assessmentReasons: String,
    @ColumnInfo(name = "questionnaire_version")
    val questionnaireVersion: String,
    @ColumnInfo(name = "assessment_version")
    val assessmentVersion: String,
    @ColumnInfo(name = "disclaimer_version")
    val disclaimerVersion: String,
    @ColumnInfo(name = "created_at_epoch_ms")
    val createdAtEpochMs: Long,
    @ColumnInfo(name = "acknowledged_at_epoch_ms")
    val acknowledgedAtEpochMs: Long,
    @ColumnInfo(name = "answered_at_epoch_ms")
    val answeredAtEpochMs: Long,
    @ColumnInfo(name = "review_due_date")
    val reviewDueDate: String,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long
)
