// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.ActivityLevel
import com.`val`.nutrigain.core.model.GainPace
import com.`val`.nutrigain.core.model.MetabolicSex
import com.`val`.nutrigain.core.model.SafetyAnswers
import com.`val`.nutrigain.core.model.SafetyLevel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateInitialPlanUseCaseTest {

    private val clock = Clock.fixed(
        Instant.parse("2026-08-09T10:00:00Z"),
        ZoneOffset.UTC
    )
    private val ids = ArrayDeque(
        listOf("goal-id", "weight-id")
    )
    private val useCase = CreateInitialPlanUseCase(
        bmiCalculator = BmiCalculator(),
        energyEstimator = EnergyEstimator(),
        safetyRuleEngine = SafetyRuleEngine(),
        idGenerator = IdGenerator {
            ids.removeFirst()
        },
        clock = clock
    )

    @Test
    fun `cree un plan coherent pour un profil sans signal`() {
        val result = useCase(validRequest())

        assertTrue(result is CreateInitialPlanResult.Success)
        val setup = (result as CreateInitialPlanResult.Success)
            .setup

        assertEquals(
            SafetyLevel.NORMAL,
            setup.safetyProfile.level
        )
        assertEquals(1_747, setup.goal.estimatedMaintenanceCalories)
        assertEquals(2_050, setup.goal.dailyCalorieTarget)
        assertEquals(300, setup.goal.initialSurplusCalories)
        assertEquals(
            0.25,
            setup.goal.targetGainKgPerWeek,
            0.001
        )
        assertEquals("gain-plan-1.0.0", setup.goal.calculationVersion)
        assertEquals(
            LocalDate.parse("2026-08-09"),
            setup.goal.startDate
        )
    }

    @Test
    fun `ne calcule pas de cible lorsqu une revue est requise`() {
        val request = validRequest().copy(
            safetyAnswers = safeAnswers().copy(
                unintentionalWeightLoss = true
            )
        )

        val result = useCase(request)
        val setup = (result as CreateInitialPlanResult.Success)
            .setup

        assertEquals(
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
            setup.safetyProfile.level
        )
        assertNull(setup.goal.estimatedMaintenanceCalories)
        assertNull(setup.goal.dailyCalorieTarget)
        assertNull(setup.goal.initialSurplusCalories)
    }


    @Test
    fun `suspend la cible automatique lorsque le poids vise un imc de trente`() {
        val result = useCase(
            validRequest().copy(targetWeightKg = 82.0)
        )
        val setup = (result as CreateInitialPlanResult.Success)
            .setup

        assertEquals(
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
            setup.safetyProfile.level
        )
        assertNull(setup.goal.dailyCalorieTarget)
    }

    @Test
    fun `refuse un objectif inferieur au poids actuel`() {
        val result = useCase(
            validRequest().copy(targetWeightKg = 54.0)
        )

        assertTrue(result is CreateInitialPlanResult.Invalid)
        val issues = (result as CreateInitialPlanResult.Invalid)
            .issues
        assertEquals(
            PlanValidationCode.TARGET_NOT_HIGHER,
            issues[PlanField.TARGET_WEIGHT]
        )
    }

    private fun validRequest() = InitialPlanRequest(
        birthDate = LocalDate.parse("1996-03-01"),
        heightCm = 165.0,
        currentWeightKg = 55.0,
        targetWeightKg = 60.0,
        metabolicSex = MetabolicSex.FEMALE,
        activityLevel = ActivityLevel.LIGHT,
        pace = GainPace.PROGRESSIVE,
        safetyAnswers = safeAnswers(),
        safetyAcknowledged = true
    )

    private fun safeAnswers() = SafetyAnswers(
        unintentionalWeightLoss = false,
        pregnantOrBreastfeeding = false,
        eatingDisorderHistory = false,
        significantDigestiveSymptoms = false,
        relevantMedicalCondition = false,
        relevantMedication = false
    )
}
