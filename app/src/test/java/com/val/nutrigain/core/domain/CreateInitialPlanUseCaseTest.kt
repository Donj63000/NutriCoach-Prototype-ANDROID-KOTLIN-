// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.ActivityLevel
import com.`val`.nutrigain.core.model.GainPace
import com.`val`.nutrigain.core.model.HealthAnswer
import com.`val`.nutrigain.core.model.HealthQuestionnaireAnswers
import com.`val`.nutrigain.core.model.MetabolicSex
import com.`val`.nutrigain.core.model.SafetyLevel
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `cree un plan coherent et tracable pour un profil sans signal`() {
        val result = useCase(
            validRequest().copy(
                medicalContextNote =
                    "  Recommandation\t déjà\nconnue.  "
            )
        )

        assertTrue(result is CreateInitialPlanResult.Success)
        val setup = (result as CreateInitialPlanResult.Success)
            .setup

        assertEquals(
            SafetyLevel.NORMAL,
            setup.safetyProfile.level
        )
        assertEquals(
            "Recommandation déjà connue.",
            setup.safetyProfile.medicalContextNote
        )
        assertEquals(
            HealthQuestionnairePolicy.QUESTIONNAIRE_VERSION,
            setup.safetyProfile.questionnaireVersion
        )
        assertEquals(
            LocalDate.parse("2027-02-09"),
            setup.safetyProfile.reviewDueAt
        )
        assertEquals(1_747, setup.goal.estimatedMaintenanceCalories)
        assertEquals(2_050, setup.goal.dailyCalorieTarget)
        assertEquals(300, setup.goal.initialSurplusCalories)
        assertEquals(
            0.25,
            setup.goal.targetGainKgPerWeek,
            0.001
        )
        assertEquals(
            "gain-plan-2.0.0",
            setup.goal.calculationVersion
        )
        assertEquals(
            LocalDate.parse("2026-08-09"),
            setup.goal.startDate
        )
        assertEquals(
            LocalDate.parse("2026-08-09"),
            setup.goal.calculationDate
        )
        assertEquals(
            55.0,
            setup.goal.calculationWeightKg,
            0.001
        )
        assertFalse(setup.aiDataAccessPolicy.enabled)
        assertTrue(setup.aiDataAccessPolicy.scopes.isEmpty())
    }

    @Test
    fun `ne calcule pas de cible lorsqu une revue est requise`() {
        val request = validRequest().copy(
            healthAnswers = safeAnswers().copy(
                unintentionalWeightLoss = HealthAnswer.YES
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
        assertNull(setup.goal.indicativeTargetDate)
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

    @Test
    fun `refuse une note medicale normalisee trop longue`() {
        val result = useCase(
            validRequest().copy(
                medicalContextNote = "x".repeat(
                    HealthQuestionnairePolicy
                        .MAX_MEDICAL_NOTE_LENGTH + 1
                )
            )
        )

        assertTrue(result is CreateInitialPlanResult.Invalid)
        assertEquals(
            PlanValidationCode.MEDICAL_NOTE_TOO_LONG,
            (result as CreateInitialPlanResult.Invalid)
                .issues[PlanField.MEDICAL_CONTEXT_NOTE]
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
        healthAnswers = safeAnswers(),
        medicalContextNote = null,
        safetyAcknowledged = true
    )

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
