// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.HealthAnswer
import com.`val`.nutrigain.core.model.SafetyLevel
import com.`val`.nutrigain.core.model.SafetyReason
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewHealthQuestionnaireUseCaseTest {

    private val initialClock = Clock.fixed(
        Instant.parse("2026-01-10T09:00:00Z"),
        ZoneOffset.UTC
    )
    private val reviewClock = Clock.fixed(
        Instant.parse("2026-03-15T12:00:00Z"),
        ZoneOffset.UTC
    )
    private val useCase = ReviewHealthQuestionnaireUseCase(
        bmiCalculator = BmiCalculator(),
        energyEstimator = EnergyEstimator(),
        safetyRuleEngine = SafetyRuleEngine(),
        clock = reviewClock
    )

    @Test
    fun `suspend atomiquement les estimations apres un nouveau signal`() {
        val setup = createTestSetup(initialClock)
        val result = useCase(
            currentSetup = setup,
            request = HealthQuestionnaireReviewRequest(
                answers = safeHealthAnswers().copy(
                    swallowingDifficultyOrPersistentVomiting =
                        HealthAnswer.YES
                ),
                medicalContextNote = null,
                safetyAcknowledged = true
            )
        )

        assertTrue(
            result is ReviewHealthQuestionnaireResult.Success
        )
        val updated =
            (result as ReviewHealthQuestionnaireResult.Success)
                .updatedSetup

        assertEquals(
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
            updated.safetyProfile.level
        )
        assertNull(updated.goal.estimatedMaintenanceCalories)
        assertNull(updated.goal.dailyCalorieTarget)
        assertNull(updated.goal.initialSurplusCalories)
        assertNull(updated.goal.indicativeTargetDate)
        assertEquals(
            LocalDate.parse("2026-09-15"),
            updated.safetyProfile.reviewDueAt
        )
    }

    @Test
    fun `restaure les estimations apres une revue sans signal`() {
        val blockedSetup = createTestSetup(
            clock = initialClock,
            answers = safeHealthAnswers().copy(
                relevantMedication = HealthAnswer.YES
            )
        ).copy(
            latestWeight = createTestSetup(initialClock)
                .latestWeight
                .copy(weightKg = 56.0)
        )

        val result = useCase(
            currentSetup = blockedSetup,
            request = HealthQuestionnaireReviewRequest(
                answers = safeHealthAnswers(),
                medicalContextNote =
                    "  Avis\tdéjà\nobtenu  ",
                safetyAcknowledged = true
            )
        )
        val updated =
            (result as ReviewHealthQuestionnaireResult.Success)
                .updatedSetup

        assertEquals(SafetyLevel.NORMAL, updated.safetyProfile.level)
        assertEquals(
            "Avis déjà obtenu",
            updated.safetyProfile.medicalContextNote
        )
        assertEquals(
            LocalDate.parse("2026-03-15"),
            updated.goal.calculationDate
        )
        assertEquals(56.0, updated.goal.calculationWeightKg, 0.001)
        assertEquals(1_760, updated.goal.estimatedMaintenanceCalories)
        assertEquals(2_060, updated.goal.dailyCalorieTarget)
    }


    @Test
    fun `suspend le calcul quand age depasse ensuite la plage supportee`() {
        val agedSetup = createTestSetup(initialClock).copy(
            profile = createTestSetup(initialClock).profile.copy(
                birthDate = LocalDate.parse("1926-01-11")
            )
        )
        val agedReviewUseCase = ReviewHealthQuestionnaireUseCase(
            bmiCalculator = BmiCalculator(),
            energyEstimator = EnergyEstimator(),
            safetyRuleEngine = SafetyRuleEngine(),
            clock = Clock.fixed(
                Instant.parse("2027-01-12T12:00:00Z"),
                ZoneOffset.UTC
            )
        )

        val result = agedReviewUseCase(
            currentSetup = agedSetup,
            request = HealthQuestionnaireReviewRequest(
                answers = safeHealthAnswers(),
                medicalContextNote = null,
                safetyAcknowledged = true
            )
        )

        assertTrue(
            result is ReviewHealthQuestionnaireResult.Success
        )
        val updated =
            (result as ReviewHealthQuestionnaireResult.Success)
                .updatedSetup

        assertEquals(
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED,
            updated.safetyProfile.level
        )
        assertTrue(
            SafetyReason.AGE_OUTSIDE_SUPPORTED_RANGE in
                updated.safetyProfile.reasons
        )
        assertNull(updated.goal.estimatedMaintenanceCalories)
        assertNull(updated.goal.dailyCalorieTarget)
    }

    @Test
    fun `exige un nouvel accuse de lecture`() {
        val result = useCase(
            currentSetup = createTestSetup(initialClock),
            request = HealthQuestionnaireReviewRequest(
                answers = safeHealthAnswers(),
                medicalContextNote = null,
                safetyAcknowledged = false
            )
        )

        assertTrue(
            result is ReviewHealthQuestionnaireResult.Invalid
        )
        assertEquals(
            PlanValidationCode.ACKNOWLEDGEMENT_REQUIRED,
            (result as ReviewHealthQuestionnaireResult.Invalid)
                .issues[PlanField.SAFETY_ACKNOWLEDGEMENT]
        )
    }
}
