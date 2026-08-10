// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.HealthQuestionnaireAnswers
import com.`val`.nutrigain.core.model.SafetyLevel
import com.`val`.nutrigain.core.model.SafetyReason
import com.`val`.nutrigain.core.model.UserSetup
import java.time.Clock
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject
import kotlin.math.ceil

data class HealthQuestionnaireReviewRequest(
    val answers: HealthQuestionnaireAnswers,
    val medicalContextNote: String?,
    val safetyAcknowledged: Boolean
)

sealed interface ReviewHealthQuestionnaireResult {
    data class Success(
        val updatedSetup: UserSetup
    ) : ReviewHealthQuestionnaireResult

    data class Invalid(
        val issues: Map<PlanField, PlanValidationCode>
    ) : ReviewHealthQuestionnaireResult
}

class ReviewHealthQuestionnaireUseCase @Inject constructor(
    private val bmiCalculator: BmiCalculator,
    private val energyEstimator: EnergyEstimator,
    private val safetyRuleEngine: SafetyRuleEngine,
    private val clock: Clock
) {

    operator fun invoke(
        currentSetup: UserSetup,
        request: HealthQuestionnaireReviewRequest
    ): ReviewHealthQuestionnaireResult {
        val issues = validate(request)
        if (issues.isNotEmpty()) {
            return ReviewHealthQuestionnaireResult.Invalid(issues)
        }

        val today = LocalDate.now(clock)
        val now = clock.instant()
        val profile = currentSetup.profile
        val currentWeight = currentSetup.latestWeight.weightKg
        val currentBmi = bmiCalculator.calculate(
            weightKg = currentWeight,
            heightCm = profile.heightCm
        )
        val targetBmi = bmiCalculator.calculate(
            weightKg = currentSetup.goal.targetWeightKg,
            heightCm = profile.heightCm
        )
        val ruleAssessment = safetyRuleEngine.evaluate(
            answers = request.answers,
            currentBmi = currentBmi.value,
            targetBmi = targetBmi.value
        )

        val ageYears =
            Period.between(profile.birthDate, today).years
        /*
         * Un profil créé à 100 ans peut dépasser ensuite la plage documentée
         * de l'équation. La révision doit rester possible sans provoquer une
         * exception : on suspend alors le calcul automatique et on conserve
         * une raison d'audit explicite.
         */
        val assessment = if (
            ageYears !in
                PlanConstraints.MIN_AGE_YEARS..
                    PlanConstraints.MAX_AGE_YEARS
        ) {
            ruleAssessment.copy(
                level = SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
                reasons = ruleAssessment.reasons +
                    SafetyReason.AGE_OUTSIDE_SUPPORTED_RANGE
            )
        } else {
            ruleAssessment
        }

        val estimate = if (
            assessment.level ==
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED
        ) {
            null
        } else {
            energyEstimator.estimate(
                weightKg = currentWeight,
                heightCm = profile.heightCm,
                ageYears = ageYears,
                metabolicSex = profile.metabolicSex,
                activityLevel = profile.activityLevel
            )
        }

        val dailyTarget = estimate?.let {
            energyEstimator.buildDailyTarget(
                maintenanceCalories = it.maintenanceCalories,
                surplusCalories =
                    currentSetup.goal.pace.initialSurplusCalories
            )
        }

        val updatedSafetyProfile =
            currentSetup.safetyProfile.copy(
                answers = request.answers,
                medicalContextNote =
                    HealthQuestionnairePolicy
                        .normalizeMedicalContextNote(
                            request.medicalContextNote
                        ),
                level = assessment.level,
                reasons = assessment.reasons,
                questionnaireVersion =
                    HealthQuestionnairePolicy
                        .QUESTIONNAIRE_VERSION,
                assessmentVersion =
                    HealthQuestionnairePolicy
                        .ASSESSMENT_VERSION,
                disclaimerVersion =
                    HealthQuestionnairePolicy
                        .DISCLAIMER_VERSION,
                acknowledgedAt = now,
                answeredAt = now,
                reviewDueAt =
                    HealthQuestionnairePolicy
                        .nextReviewDate(today),
                updatedAt = now
            )

        val updatedGoal = currentSetup.goal.copy(
            indicativeTargetDate = indicativeTargetDate(
                currentWeightKg = currentWeight,
                targetWeightKg =
                    currentSetup.goal.targetWeightKg,
                targetGainKgPerWeek =
                    currentSetup.goal.targetGainKgPerWeek,
                startDate = today,
                safetyLevel = assessment.level
            ),
            calculationWeightKg = currentWeight,
            calculationDate = today,
            estimatedMaintenanceCalories =
                estimate?.maintenanceCalories,
            dailyCalorieTarget = dailyTarget,
            initialSurplusCalories = estimate?.let {
                currentSetup.goal.pace.initialSurplusCalories
            },
            calculationVersion = CALCULATION_VERSION,
            updatedAt = now
        )

        return ReviewHealthQuestionnaireResult.Success(
            currentSetup.copy(
                safetyProfile = updatedSafetyProfile,
                goal = updatedGoal
            )
        )
    }

    private fun validate(
        request: HealthQuestionnaireReviewRequest
    ): Map<PlanField, PlanValidationCode> {
        val issues = linkedMapOf<PlanField, PlanValidationCode>()

        if (
            !HealthQuestionnairePolicy
                .isMedicalContextNoteValid(
                    request.medicalContextNote
                )
        ) {
            issues[PlanField.MEDICAL_CONTEXT_NOTE] =
                PlanValidationCode.MEDICAL_NOTE_TOO_LONG
        }

        if (!request.safetyAcknowledged) {
            issues[PlanField.SAFETY_ACKNOWLEDGEMENT] =
                PlanValidationCode.ACKNOWLEDGEMENT_REQUIRED
        }

        return issues
    }

    private fun indicativeTargetDate(
        currentWeightKg: Double,
        targetWeightKg: Double,
        targetGainKgPerWeek: Double,
        startDate: LocalDate,
        safetyLevel: SafetyLevel
    ): LocalDate? {
        if (
            safetyLevel ==
                SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED ||
            targetWeightKg <= currentWeightKg
        ) {
            return null
        }

        val weeks = ceil(
            (targetWeightKg - currentWeightKg) /
                targetGainKgPerWeek
        ).toLong()

        if (weeks !in 1..MAX_INDICATIVE_DURATION_WEEKS) {
            return null
        }

        return startDate.plusWeeks(weeks)
    }

    private companion object {
        const val CALCULATION_VERSION = "gain-plan-2.0.0"
        const val MAX_INDICATIVE_DURATION_WEEKS = 104L
    }
}
