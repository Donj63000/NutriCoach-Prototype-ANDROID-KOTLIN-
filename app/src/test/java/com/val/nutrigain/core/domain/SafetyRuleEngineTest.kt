// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.HealthAnswer
import com.`val`.nutrigain.core.model.HealthQuestionnaireAnswers
import com.`val`.nutrigain.core.model.SafetyLevel
import com.`val`.nutrigain.core.model.SafetyReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SafetyRuleEngineTest {

    private val engine = SafetyRuleEngine()

    @Test
    fun `demande une revue professionnelle pour une perte involontaire`() {
        val assessment = evaluate(
            answers = safeAnswers().copy(
                unintentionalWeightLoss = HealthAnswer.YES
            )
        )

        assertEquals(
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
            assessment.level
        )
        assertTrue(
            SafetyReason.UNINTENTIONAL_WEIGHT_LOSS in
                assessment.reasons
        )
    }

    @Test
    fun `une reponse incertaine suspend le plan et reste explicable`() {
        val assessment = evaluate(
            answers = safeAnswers().copy(
                reducedAppetite = HealthAnswer.UNSURE
            )
        )

        assertEquals(
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
            assessment.level
        )
        assertTrue(
            SafetyReason.REDUCED_APPETITE in assessment.reasons
        )
        assertTrue(
            SafetyReason.UNCERTAIN_CRITICAL_ANSWER in
                assessment.reasons
        )
    }

    @Test
    fun `tous les signaux critiques suspendent le plan automatique`() {
        val criticalAnswers = listOf(
            safeAnswers().copy(
                unintentionalWeightLoss = HealthAnswer.YES
            ),
            safeAnswers().copy(
                reducedAppetite = HealthAnswer.YES
            ),
            safeAnswers().copy(
                swallowingDifficultyOrPersistentVomiting =
                    HealthAnswer.YES
            ),
            safeAnswers().copy(
                pregnantOrBreastfeeding = HealthAnswer.YES
            ),
            safeAnswers().copy(
                eatingDisorderHistory = HealthAnswer.YES
            ),
            safeAnswers().copy(
                significantDigestiveSymptoms = HealthAnswer.YES
            ),
            safeAnswers().copy(
                relevantMedicalCondition = HealthAnswer.YES
            ),
            safeAnswers().copy(
                relevantMedication = HealthAnswer.YES
            )
        )

        criticalAnswers.forEach { answers ->
            assertEquals(
                SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
                evaluate(answers = answers).level
            )
        }
    }

    @Test
    fun `une allergie seule active la prudence sans bloquer le calcul`() {
        val assessment = evaluate(
            answers = safeAnswers().copy(
                foodAllergiesOrIntolerances = HealthAnswer.YES
            )
        )

        assertEquals(SafetyLevel.CAUTION, assessment.level)
        assertEquals(
            setOf(
                SafetyReason.FOOD_ALLERGIES_OR_INTOLERANCES
            ),
            assessment.reasons
        )
    }

    @Test
    fun `respecte les bornes basses des seuils d imc`() {
        assertEquals(
            SafetyLevel.CAUTION,
            evaluate(
                currentBmi = 17.0,
                targetBmi = 20.0
            ).level
        )
        assertEquals(
            SafetyLevel.NORMAL,
            evaluate(
                currentBmi = 18.5,
                targetBmi = 20.0
            ).level
        )
    }

    @Test
    fun `demande une revue lorsque l imc actuel atteint trente`() {
        assertEquals(
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
            evaluate(
                currentBmi = 30.0,
                targetBmi = 31.0
            ).level
        )
    }

    @Test
    fun `demande une revue lorsque la cible atteint trente`() {
        assertEquals(
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
            evaluate(
                currentBmi = 24.0,
                targetBmi = 30.0
            ).level
        )
    }

    @Test
    fun `active la prudence lorsque la cible atteint vingt cinq`() {
        assertEquals(
            SafetyLevel.CAUTION,
            evaluate(
                currentBmi = 22.0,
                targetBmi = 25.0
            ).level
        )
    }

    @Test
    fun `autorise le plan normal sans signal de prudence`() {
        assertEquals(SafetyLevel.NORMAL, evaluate().level)
    }

    private fun evaluate(
        answers: HealthQuestionnaireAnswers = safeAnswers(),
        currentBmi: Double = 21.0,
        targetBmi: Double = 22.0
    ): SafetyAssessment {
        return engine.evaluate(
            answers = answers,
            currentBmi = currentBmi,
            targetBmi = targetBmi
        )
    }

    private fun safeAnswers() = HealthQuestionnaireAnswers(
        unintentionalWeightLoss = HealthAnswer.NO,
        reducedAppetite = HealthAnswer.NO,
        swallowingDifficultyOrPersistentVomiting =
            HealthAnswer.NO,
        pregnantOrBreastfeeding = HealthAnswer.NO,
        eatingDisorderHistory = HealthAnswer.NO,
        significantDigestiveSymptoms = HealthAnswer.NO,
        relevantMedicalCondition = HealthAnswer.NO,
        relevantMedication = HealthAnswer.NO,
        foodAllergiesOrIntolerances = HealthAnswer.NO
    )
}
