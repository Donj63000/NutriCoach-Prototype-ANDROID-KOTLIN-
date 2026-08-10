// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.HealthAnswer
import com.`val`.nutrigain.core.model.HealthQuestionnaireAnswers
import com.`val`.nutrigain.core.model.SafetyLevel
import com.`val`.nutrigain.core.model.SafetyReason
import javax.inject.Inject

data class SafetyAssessment(
    val level: SafetyLevel,
    val reasons: Set<SafetyReason>
)

class SafetyRuleEngine @Inject constructor() {

    fun evaluate(
        answers: HealthQuestionnaireAnswers,
        currentBmi: Double,
        targetBmi: Double
    ): SafetyAssessment {
        require(currentBmi.isFinite() && currentBmi > 0.0)
        require(targetBmi.isFinite() && targetBmi > 0.0)
        val professionalReviewReasons = linkedSetOf<SafetyReason>()
        val cautionReasons = linkedSetOf<SafetyReason>()

        when {
            currentBmi < PROFESSIONAL_REVIEW_LOW_BMI ->
                professionalReviewReasons +=
                    SafetyReason.CURRENT_BMI_MARKEDLY_LOW

            currentBmi < REFERENCE_BMI_MIN ->
                cautionReasons +=
                    SafetyReason.CURRENT_BMI_OUTSIDE_REFERENCE

            currentBmi >= PROFESSIONAL_REVIEW_HIGH_BMI ->
                professionalReviewReasons +=
                    SafetyReason.CURRENT_BMI_HIGH

            currentBmi >= REFERENCE_BMI_MAX ->
                cautionReasons +=
                    SafetyReason.CURRENT_BMI_OUTSIDE_REFERENCE
        }

        when {
            targetBmi >= PROFESSIONAL_REVIEW_HIGH_BMI ->
                professionalReviewReasons +=
                    SafetyReason.TARGET_BMI_HIGH

            targetBmi >= REFERENCE_BMI_MAX ->
                cautionReasons +=
                    SafetyReason.TARGET_BMI_OUTSIDE_REFERENCE
        }

        /*
         * Les réponses positives ou incertaines sur un signal clinique
         * suspendent l'objectif automatique. Ce moteur effectue uniquement un
         * triage de prudence ; il ne pose aucun diagnostic.
         */
        evaluateCriticalAnswer(
            answer = answers.unintentionalWeightLoss,
            positiveReason = SafetyReason.UNINTENTIONAL_WEIGHT_LOSS,
            professionalReviewReasons = professionalReviewReasons
        )
        evaluateCriticalAnswer(
            answer = answers.reducedAppetite,
            positiveReason = SafetyReason.REDUCED_APPETITE,
            professionalReviewReasons = professionalReviewReasons
        )
        evaluateCriticalAnswer(
            answer = answers.swallowingDifficultyOrPersistentVomiting,
            positiveReason =
                SafetyReason
                    .SWALLOWING_DIFFICULTY_OR_PERSISTENT_VOMITING,
            professionalReviewReasons = professionalReviewReasons
        )
        evaluateCriticalAnswer(
            answer = answers.pregnantOrBreastfeeding,
            positiveReason =
                SafetyReason.PREGNANCY_OR_BREASTFEEDING,
            professionalReviewReasons = professionalReviewReasons
        )
        evaluateCriticalAnswer(
            answer = answers.eatingDisorderHistory,
            positiveReason = SafetyReason.EATING_DISORDER_HISTORY,
            professionalReviewReasons = professionalReviewReasons
        )
        evaluateCriticalAnswer(
            answer = answers.significantDigestiveSymptoms,
            positiveReason =
                SafetyReason.SIGNIFICANT_DIGESTIVE_SYMPTOMS,
            professionalReviewReasons = professionalReviewReasons
        )
        evaluateCriticalAnswer(
            answer = answers.relevantMedicalCondition,
            positiveReason =
                SafetyReason.RELEVANT_MEDICAL_CONDITION,
            professionalReviewReasons = professionalReviewReasons
        )
        evaluateCriticalAnswer(
            answer = answers.relevantMedication,
            positiveReason = SafetyReason.RELEVANT_MEDICATION,
            professionalReviewReasons = professionalReviewReasons
        )

        /*
         * Une allergie ou une intolérance doit contraindre les futures
         * suggestions de repas, mais ne rend pas à elle seule l'estimation
         * énergétique impossible.
         */
        if (
            answers.foodAllergiesOrIntolerances != HealthAnswer.NO
        ) {
            cautionReasons +=
                SafetyReason.FOOD_ALLERGIES_OR_INTOLERANCES
        }

        return when {
            professionalReviewReasons.isNotEmpty() ->
                SafetyAssessment(
                    level =
                        SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
                    reasons =
                        professionalReviewReasons + cautionReasons
                )

            cautionReasons.isNotEmpty() ->
                SafetyAssessment(
                    level = SafetyLevel.CAUTION,
                    reasons = cautionReasons
                )

            else ->
                SafetyAssessment(
                    level = SafetyLevel.NORMAL,
                    reasons = emptySet()
                )
        }
    }

    private fun evaluateCriticalAnswer(
        answer: HealthAnswer,
        positiveReason: SafetyReason,
        professionalReviewReasons: MutableSet<SafetyReason>
    ) {
        when (answer) {
            HealthAnswer.YES ->
                professionalReviewReasons += positiveReason

            HealthAnswer.UNSURE -> {
                professionalReviewReasons += positiveReason
                professionalReviewReasons +=
                    SafetyReason.UNCERTAIN_CRITICAL_ANSWER
            }

            HealthAnswer.NO -> Unit
        }
    }

    private companion object {
        const val PROFESSIONAL_REVIEW_LOW_BMI = 17.0
        const val PROFESSIONAL_REVIEW_HIGH_BMI = 30.0
        const val REFERENCE_BMI_MIN = 18.5
        const val REFERENCE_BMI_MAX = 25.0
    }
}
