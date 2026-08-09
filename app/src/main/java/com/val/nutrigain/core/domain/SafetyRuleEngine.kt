// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.SafetyAnswers
import com.`val`.nutrigain.core.model.SafetyLevel
import javax.inject.Inject

class SafetyRuleEngine @Inject constructor() {

    fun evaluate(
        answers: SafetyAnswers,
        currentBmi: Double,
        targetBmi: Double
    ): SafetyLevel {
        require(currentBmi.isFinite() && currentBmi > 0.0)
        require(targetBmi.isFinite() && targetBmi > 0.0)
        require(targetBmi >= currentBmi)

        /*
         * Ces règles effectuent uniquement un triage de prudence. Elles ne
         * diagnostiquent rien. Un objectif automatique est suspendu lorsqu'un
         * signal déclaré, un IMC actuel très bas ou une cible située dans la
         * zone statistique de l'obésité demande une évaluation individualisée.
         */
        val requiresProfessionalReview =
            currentBmi < PROFESSIONAL_REVIEW_LOW_BMI ||
                currentBmi >= PROFESSIONAL_REVIEW_HIGH_BMI ||
                targetBmi >= PROFESSIONAL_REVIEW_HIGH_BMI ||
                answers.unintentionalWeightLoss ||
                answers.pregnantOrBreastfeeding ||
                answers.eatingDisorderHistory ||
                answers.significantDigestiveSymptoms ||
                answers.relevantMedicalCondition ||
                answers.relevantMedication

        if (requiresProfessionalReview) {
            return SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED
        }

        val requiresCaution =
            currentBmi < REFERENCE_BMI_MIN ||
                currentBmi >= REFERENCE_BMI_MAX ||
                targetBmi >= REFERENCE_BMI_MAX

        return if (requiresCaution) {
            SafetyLevel.CAUTION
        } else {
            SafetyLevel.NORMAL
        }
    }

    private companion object {
        const val PROFESSIONAL_REVIEW_LOW_BMI = 17.0
        const val PROFESSIONAL_REVIEW_HIGH_BMI = 30.0
        const val REFERENCE_BMI_MIN = 18.5
        const val REFERENCE_BMI_MAX = 25.0
    }
}
