// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.ActivityLevel
import com.`val`.nutrigain.core.model.GainPace
import com.`val`.nutrigain.core.model.HealthAnswer
import com.`val`.nutrigain.core.model.HealthQuestionnaireAnswers
import com.`val`.nutrigain.core.model.MetabolicSex
import com.`val`.nutrigain.core.model.UserSetup
import java.time.Clock
import java.time.LocalDate

internal fun safeHealthAnswers() =
    HealthQuestionnaireAnswers(
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

internal fun createTestSetup(
    clock: Clock,
    answers: HealthQuestionnaireAnswers =
        safeHealthAnswers(),
    medicalContextNote: String? = null
): UserSetup {
    val ids = ArrayDeque(listOf("goal-id", "weight-id"))
    val result = CreateInitialPlanUseCase(
        bmiCalculator = BmiCalculator(),
        energyEstimator = EnergyEstimator(),
        safetyRuleEngine = SafetyRuleEngine(),
        idGenerator = IdGenerator { ids.removeFirst() },
        clock = clock
    )(
        InitialPlanRequest(
            birthDate = LocalDate.parse("1996-03-01"),
            heightCm = 165.0,
            currentWeightKg = 55.0,
            targetWeightKg = 60.0,
            metabolicSex = MetabolicSex.FEMALE,
            activityLevel = ActivityLevel.LIGHT,
            pace = GainPace.PROGRESSIVE,
            healthAnswers = answers,
            medicalContextNote = medicalContextNote,
            safetyAcknowledged = true
        )
    )

    check(result is CreateInitialPlanResult.Success)
    return result.setup
}
