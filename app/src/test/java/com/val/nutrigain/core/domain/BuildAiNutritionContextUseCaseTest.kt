// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.AiDataScope
import com.`val`.nutrigain.core.model.HealthAnswer
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildAiNutritionContextUseCaseTest {

    private val clock = Clock.fixed(
        Instant.parse("2026-08-09T10:00:00Z"),
        ZoneOffset.UTC
    )
    private val useCase = BuildAiNutritionContextUseCase(clock)

    @Test
    fun `refuse tout acces lorsque la politique est desactivee`() {
        val result = useCase(createTestSetup(clock))

        assertEquals(
            AiContextDeniedReason.POLICY_DISABLED,
            (result as BuildAiNutritionContextResult.Denied)
                .reason
        )
    }

    @Test
    fun `construit uniquement le contexte explicitement consenti`() {
        val setup = createTestSetup(
            clock = clock,
            medicalContextNote = "Donnée libre à ne pas transmettre"
        )
        val enabledPolicy = setup.aiDataAccessPolicy.copy(
            enabled = true,
            scopes = setOf(
                AiDataScope.PROFILE,
                AiDataScope.LATEST_WEIGHT,
                AiDataScope.SAFETY_SUMMARY
            ),
            consentVersion =
                BuildAiNutritionContextUseCase
                    .CURRENT_AI_CONSENT_VERSION,
            grantedAt =
                Instant.parse("2026-08-09T09:00:00Z"),
            updatedAt =
                Instant.parse("2026-08-09T09:00:00Z")
        )

        val result = useCase(
            setup.copy(aiDataAccessPolicy = enabledPolicy)
        )
        val context =
            (result as BuildAiNutritionContextResult.Ready)
                .context

        assertEquals(30, context.profile?.ageYears)
        assertEquals(165.0, context.profile?.heightCm ?: 0.0, 0.001)
        assertNull(context.goal)
        assertNotNull(context.latestWeight)
        assertEquals(0L, context.latestWeight?.measurementAgeDays)
        assertFalse(context.calorieTargetMayBeModified)

        /*
         * Contrôle de non-régression simple : les deux valeurs directement
         * identifiantes/sensibles ne doivent apparaître dans aucune donnée
         * sérialisable du contexte.
         */
        assertFalse(context.toString().contains("1996-03-01"))
        assertFalse(
            context.toString().contains(
                "Donnée libre à ne pas transmettre"
            )
        )
    }

    @Test
    fun `exige le resume de prudence dans les portees`() {
        val setup = createTestSetup(clock)
        val policy = setup.aiDataAccessPolicy.copy(
            enabled = true,
            scopes = setOf(AiDataScope.PROFILE),
            consentVersion =
                BuildAiNutritionContextUseCase
                    .CURRENT_AI_CONSENT_VERSION,
            grantedAt = clock.instant(),
            updatedAt = clock.instant()
        )

        val result = useCase(
            setup.copy(aiDataAccessPolicy = policy)
        )

        assertEquals(
            AiContextDeniedReason.REQUIRED_SAFETY_SCOPE_MISSING,
            (result as BuildAiNutritionContextResult.Denied)
                .reason
        )
    }

    @Test
    fun `refuse un questionnaire arrivant a echeance aujourd hui`() {
        val setup = createTestSetup(clock)
        val policy = validPolicy(setup)
        val dueSetup = setup.copy(
            safetyProfile = setup.safetyProfile.copy(
                reviewDueAt = LocalDate.parse("2026-08-09")
            ),
            aiDataAccessPolicy = policy
        )

        val result = useCase(dueSetup)

        assertEquals(
            AiContextDeniedReason.QUESTIONNAIRE_STALE,
            (result as BuildAiNutritionContextResult.Denied)
                .reason
        )
    }


    @Test
    fun `refuse des regles de prudence devenues obsoletes`() {
        val setup = createTestSetup(clock)
        val staleSetup = setup.copy(
            safetyProfile = setup.safetyProfile.copy(
                assessmentVersion = "safety-rules-1.0.0"
            ),
            aiDataAccessPolicy = validPolicy(setup)
        )

        val result = useCase(staleSetup)

        assertEquals(
            AiContextDeniedReason.QUESTIONNAIRE_STALE,
            (result as BuildAiNutritionContextResult.Denied)
                .reason
        )
    }

    @Test
    fun `refuse un profil sorti de la plage age du moteur`() {
        val setup = createTestSetup(clock)
        val agedSetup = setup.copy(
            profile = setup.profile.copy(
                birthDate = LocalDate.parse("1925-08-08")
            ),
            aiDataAccessPolicy = validPolicy(setup)
        )

        val result = useCase(agedSetup)

        assertEquals(
            AiContextDeniedReason.PROFILE_OUTSIDE_SUPPORTED_RANGE,
            (result as BuildAiNutritionContextResult.Denied)
                .reason
        )
    }

    @Test
    fun `refuse le contexte lorsqu une revue professionnelle est requise`() {
        val setup = createTestSetup(
            clock = clock,
            answers = safeHealthAnswers().copy(
                reducedAppetite = HealthAnswer.YES
            )
        )
        val result = useCase(
            setup.copy(aiDataAccessPolicy = validPolicy(setup))
        )

        assertEquals(
            AiContextDeniedReason.PROFESSIONAL_REVIEW_REQUIRED,
            (result as BuildAiNutritionContextResult.Denied)
                .reason
        )
    }

    @Test
    fun `refuse un consentement revoque`() {
        val setup = createTestSetup(clock)
        val policy = validPolicy(setup).copy(
            revokedAt = clock.instant()
        )

        val result = useCase(
            setup.copy(aiDataAccessPolicy = policy)
        )

        assertEquals(
            AiContextDeniedReason.INVALID_OR_OUTDATED_CONSENT,
            (result as BuildAiNutritionContextResult.Denied)
                .reason
        )
    }

    private fun validPolicy(
        setup: com.`val`.nutrigain.core.model.UserSetup
    ) = setup.aiDataAccessPolicy.copy(
        enabled = true,
        scopes = setOf(
            AiDataScope.PROFILE,
            AiDataScope.GOAL,
            AiDataScope.LATEST_WEIGHT,
            AiDataScope.SAFETY_SUMMARY
        ),
        consentVersion =
            BuildAiNutritionContextUseCase
                .CURRENT_AI_CONSENT_VERSION,
        grantedAt = clock.instant(),
        updatedAt = clock.instant()
    )
}
