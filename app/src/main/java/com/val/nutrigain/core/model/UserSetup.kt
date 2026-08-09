// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.model

import java.time.Instant
import java.time.LocalDate

enum class MetabolicSex {
    FEMALE,
    MALE
}

enum class ActivityLevel(
    val maintenanceMultiplier: Double
) {
    SEDENTARY(1.20),
    LIGHT(1.375),
    MODERATE(1.55),
    HIGH(1.725)
}

enum class GainPace(
    val initialSurplusCalories: Int,
    val targetGainKgPerWeek: Double
) {
    GENTLE(
        initialSurplusCalories = 200,
        targetGainKgPerWeek = 0.15
    ),
    PROGRESSIVE(
        initialSurplusCalories = 300,
        targetGainKgPerWeek = 0.25
    ),
    SUSTAINED(
        initialSurplusCalories = 400,
        targetGainKgPerWeek = 0.35
    )
}

enum class SafetyLevel {
    NORMAL,
    CAUTION,
    PROFESSIONAL_REVIEW_REQUIRED
}

data class SafetyAnswers(
    val unintentionalWeightLoss: Boolean,
    val pregnantOrBreastfeeding: Boolean,
    val eatingDisorderHistory: Boolean,
    val significantDigestiveSymptoms: Boolean,
    val relevantMedicalCondition: Boolean,
    val relevantMedication: Boolean
)

data class UserProfile(
    val id: String,
    val birthDate: LocalDate,
    val heightCm: Double,
    val metabolicSex: MetabolicSex,
    val activityLevel: ActivityLevel,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class SafetyProfile(
    val id: String,
    val answers: SafetyAnswers,
    val level: SafetyLevel,
    val acknowledgedAt: Instant,
    val updatedAt: Instant
)

data class Goal(
    val id: String,
    val startWeightKg: Double,
    val targetWeightKg: Double,
    val startDate: LocalDate,
    val indicativeTargetDate: LocalDate?,
    val estimatedMaintenanceCalories: Int?,
    val dailyCalorieTarget: Int?,
    val initialSurplusCalories: Int?,
    val targetGainKgPerWeek: Double,
    val pace: GainPace,
    val initialBmi: Double,
    val active: Boolean,
    val calculationVersion: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class WeightMeasurement(
    val id: String,
    val weightKg: Double,
    val measuredAt: Instant,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class UserSetup(
    val profile: UserProfile,
    val safetyProfile: SafetyProfile,
    val goal: Goal,
    val latestWeight: WeightMeasurement
)
