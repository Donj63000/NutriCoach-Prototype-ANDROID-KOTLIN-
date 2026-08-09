// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.SafetyAnswers
import com.`val`.nutrigain.core.model.SafetyLevel
import org.junit.Assert.assertEquals
import org.junit.Test

class SafetyRuleEngineTest {

    private val engine = SafetyRuleEngine()

    @Test
    fun `demande une revue professionnelle pour une perte involontaire`() {
        val level = evaluate(
            answers = safeAnswers().copy(
                unintentionalWeightLoss = true
            )
        )

        assertEquals(
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
            level
        )
    }

    @Test
    fun `active la prudence pour un imc bas sans autre signal`() {
        val level = evaluate(
            currentBmi = 18.0,
            targetBmi = 20.0
        )

        assertEquals(SafetyLevel.CAUTION, level)
    }

    @Test
    fun `tous les signaux critiques suspendent le plan automatique`() {
        val criticalAnswers = listOf(
            safeAnswers().copy(unintentionalWeightLoss = true),
            safeAnswers().copy(pregnantOrBreastfeeding = true),
            safeAnswers().copy(eatingDisorderHistory = true),
            safeAnswers().copy(significantDigestiveSymptoms = true),
            safeAnswers().copy(relevantMedicalCondition = true),
            safeAnswers().copy(relevantMedication = true)
        )

        criticalAnswers.forEach { answers ->
            assertEquals(
                SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
                evaluate(answers = answers)
            )
        }
    }

    @Test
    fun `respecte les bornes basses des seuils d imc`() {
        assertEquals(
            SafetyLevel.CAUTION,
            evaluate(currentBmi = 17.0, targetBmi = 20.0)
        )
        assertEquals(
            SafetyLevel.NORMAL,
            evaluate(currentBmi = 18.5, targetBmi = 20.0)
        )
    }

    @Test
    fun `demande une revue lorsque l imc actuel atteint trente`() {
        val level = evaluate(
            currentBmi = 30.0,
            targetBmi = 31.0
        )

        assertEquals(
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
            level
        )
    }

    @Test
    fun `demande une revue lorsque la cible atteint trente`() {
        val level = evaluate(
            currentBmi = 24.0,
            targetBmi = 30.0
        )

        assertEquals(
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
            level
        )
    }

    @Test
    fun `active la prudence lorsque la cible atteint vingt cinq`() {
        val level = evaluate(
            currentBmi = 22.0,
            targetBmi = 25.0
        )

        assertEquals(SafetyLevel.CAUTION, level)
    }

    @Test
    fun `autorise le plan normal sans signal de prudence`() {
        val level = evaluate()

        assertEquals(SafetyLevel.NORMAL, level)
    }

    private fun evaluate(
        answers: SafetyAnswers = safeAnswers(),
        currentBmi: Double = 21.0,
        targetBmi: Double = 22.0
    ): SafetyLevel {
        return engine.evaluate(
            answers = answers,
            currentBmi = currentBmi,
            targetBmi = targetBmi
        )
    }

    private fun safeAnswers() = SafetyAnswers(
        unintentionalWeightLoss = false,
        pregnantOrBreastfeeding = false,
        eatingDisorderHistory = false,
        significantDigestiveSymptoms = false,
        relevantMedicalCondition = false,
        relevantMedication = false
    )
}
