// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.ActivityLevel
import com.`val`.nutrigain.core.model.GainPace
import com.`val`.nutrigain.core.model.Goal
import com.`val`.nutrigain.core.model.MetabolicSex
import com.`val`.nutrigain.core.model.SafetyAnswers
import com.`val`.nutrigain.core.model.SafetyLevel
import com.`val`.nutrigain.core.model.SafetyProfile
import com.`val`.nutrigain.core.model.UserProfile
import com.`val`.nutrigain.core.model.UserSetup
import com.`val`.nutrigain.core.model.WeightMeasurement
import java.time.Clock
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

data class InitialPlanRequest(
    val birthDate: LocalDate,
    val heightCm: Double,
    val currentWeightKg: Double,
    val targetWeightKg: Double,
    val metabolicSex: MetabolicSex,
    val activityLevel: ActivityLevel,
    val pace: GainPace,
    val safetyAnswers: SafetyAnswers,
    val safetyAcknowledged: Boolean
)

enum class PlanField {
    BIRTH_DATE,
    HEIGHT,
    CURRENT_WEIGHT,
    TARGET_WEIGHT,
    SAFETY_ACKNOWLEDGEMENT
}

enum class PlanValidationCode {
    REQUIRED,
    INVALID_BIRTH_DATE,
    ADULT_ONLY,
    AGE_OUT_OF_RANGE,
    INVALID_HEIGHT,
    INVALID_WEIGHT,
    TARGET_NOT_HIGHER,
    ACKNOWLEDGEMENT_REQUIRED
}

object PlanConstraints {
    const val MIN_AGE_YEARS = 18
    const val MAX_AGE_YEARS = 100
    const val MIN_HEIGHT_CM = 120.0
    const val MAX_HEIGHT_CM = 230.0
    const val MIN_WEIGHT_KG = 25.0
    const val MAX_WEIGHT_KG = 350.0
}

sealed interface CreateInitialPlanResult {
    data class Success(
        val setup: UserSetup
    ) : CreateInitialPlanResult

    data class Invalid(
        val issues: Map<PlanField, PlanValidationCode>
    ) : CreateInitialPlanResult
}

class CreateInitialPlanUseCase @Inject constructor(
    private val bmiCalculator: BmiCalculator,
    private val energyEstimator: EnergyEstimator,
    private val safetyRuleEngine: SafetyRuleEngine,
    private val idGenerator: IdGenerator,
    private val clock: Clock
) {

    operator fun invoke(request: InitialPlanRequest): CreateInitialPlanResult {
        val today = LocalDate.now(clock)
        val issues = validate(request, today)

        if (issues.isNotEmpty()) {
            return CreateInitialPlanResult.Invalid(issues)
        }

        val ageYears = Period.between(request.birthDate, today).years
        val currentBmi = bmiCalculator.calculate(
            weightKg = request.currentWeightKg,
            heightCm = request.heightCm
        )
        val targetBmi = bmiCalculator.calculate(
            weightKg = request.targetWeightKg,
            heightCm = request.heightCm
        )
        val safetyLevel = safetyRuleEngine.evaluate(
            answers = request.safetyAnswers,
            currentBmi = currentBmi.value,
            targetBmi = targetBmi.value
        )

        val energyEstimate = if (
            safetyLevel == SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED
        ) {
            null
        } else {
            energyEstimator.estimate(
                weightKg = request.currentWeightKg,
                heightCm = request.heightCm,
                ageYears = ageYears,
                metabolicSex = request.metabolicSex,
                activityLevel = request.activityLevel
            )
        }

        val targetCalories = energyEstimate?.let {
            energyEstimator.buildDailyTarget(
                maintenanceCalories = it.maintenanceCalories,
                surplusCalories = request.pace.initialSurplusCalories
            )
        }

        val now = clock.instant()
        val profileId = CURRENT_PROFILE_ID
        val safetyProfileId = CURRENT_SAFETY_PROFILE_ID

        val setup = UserSetup(
            profile = UserProfile(
                id = profileId,
                birthDate = request.birthDate,
                heightCm = request.heightCm,
                metabolicSex = request.metabolicSex,
                activityLevel = request.activityLevel,
                createdAt = now,
                updatedAt = now
            ),
            safetyProfile = SafetyProfile(
                id = safetyProfileId,
                answers = request.safetyAnswers,
                level = safetyLevel,
                acknowledgedAt = now,
                updatedAt = now
            ),
            goal = Goal(
                id = idGenerator.newId(),
                startWeightKg = request.currentWeightKg,
                targetWeightKg = request.targetWeightKg,
                startDate = today,
                indicativeTargetDate = null,
                estimatedMaintenanceCalories = energyEstimate?.maintenanceCalories,
                dailyCalorieTarget = targetCalories,
                initialSurplusCalories = energyEstimate
                    ?.let { request.pace.initialSurplusCalories },
                targetGainKgPerWeek =
                    request.pace.targetGainKgPerWeek,
                pace = request.pace,
                initialBmi = currentBmi.value,
                active = true,
                calculationVersion = CALCULATION_VERSION,
                createdAt = now,
                updatedAt = now
            ),
            latestWeight = WeightMeasurement(
                id = idGenerator.newId(),
                weightKg = request.currentWeightKg,
                measuredAt = now,
                note = null,
                createdAt = now,
                updatedAt = now
            )
        )

        return CreateInitialPlanResult.Success(setup)
    }

    private fun validate(
        request: InitialPlanRequest,
        today: LocalDate
    ): Map<PlanField, PlanValidationCode> {
        val issues = linkedMapOf<PlanField, PlanValidationCode>()
        val ageYears = Period.between(request.birthDate, today).years

        when {
            request.birthDate.isAfter(today) ->
                issues[PlanField.BIRTH_DATE] =
                    PlanValidationCode.INVALID_BIRTH_DATE

            ageYears < PlanConstraints.MIN_AGE_YEARS ->
                issues[PlanField.BIRTH_DATE] =
                    PlanValidationCode.ADULT_ONLY

            ageYears > PlanConstraints.MAX_AGE_YEARS ->
                issues[PlanField.BIRTH_DATE] =
                    PlanValidationCode.AGE_OUT_OF_RANGE
        }

        if (
            !request.heightCm.isFinite() ||
            request.heightCm !in PlanConstraints.MIN_HEIGHT_CM..PlanConstraints.MAX_HEIGHT_CM
        ) {
            issues[PlanField.HEIGHT] =
                PlanValidationCode.INVALID_HEIGHT
        }

        if (
            !request.currentWeightKg.isFinite() ||
            request.currentWeightKg !in PlanConstraints.MIN_WEIGHT_KG..PlanConstraints.MAX_WEIGHT_KG
        ) {
            issues[PlanField.CURRENT_WEIGHT] =
                PlanValidationCode.INVALID_WEIGHT
        }

        if (
            !request.targetWeightKg.isFinite() ||
            request.targetWeightKg !in PlanConstraints.MIN_WEIGHT_KG..PlanConstraints.MAX_WEIGHT_KG
        ) {
            issues[PlanField.TARGET_WEIGHT] =
                PlanValidationCode.INVALID_WEIGHT
        } else if (
            request.currentWeightKg.isFinite() &&
            request.currentWeightKg in
            PlanConstraints.MIN_WEIGHT_KG..PlanConstraints.MAX_WEIGHT_KG &&
            request.targetWeightKg <= request.currentWeightKg
        ) {
            issues[PlanField.TARGET_WEIGHT] =
                PlanValidationCode.TARGET_NOT_HIGHER
        }

        if (!request.safetyAcknowledged) {
            issues[PlanField.SAFETY_ACKNOWLEDGEMENT] =
                PlanValidationCode.ACKNOWLEDGEMENT_REQUIRED
        }

        return issues
    }

    private companion object {
        const val CURRENT_PROFILE_ID = "current-profile"
        const val CURRENT_SAFETY_PROFILE_ID = "current-safety-profile"
        const val CALCULATION_VERSION = "gain-plan-1.0.0"
    }
}
