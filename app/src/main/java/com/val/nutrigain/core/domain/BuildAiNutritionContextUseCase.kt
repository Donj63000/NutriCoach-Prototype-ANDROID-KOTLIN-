// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.ActivityLevel
import com.`val`.nutrigain.core.model.AiDataAccessPolicy
import com.`val`.nutrigain.core.model.AiDataScope
import com.`val`.nutrigain.core.model.GainPace
import com.`val`.nutrigain.core.model.SafetyLevel
import com.`val`.nutrigain.core.model.UserSetup
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Period
import javax.inject.Inject

data class AiProfileContext(
    val ageYears: Int,
    val heightCm: Double,
    val activityLevel: ActivityLevel
)

data class AiGoalContext(
    val targetWeightKg: Double,
    val pace: GainPace,
    val targetGainKgPerWeek: Double,
    val currentDailyCalorieTarget: Int?,
    val calculationDate: LocalDate
)

data class AiLatestWeightContext(
    val weightKg: Double,
    val measurementAgeDays: Long
)

data class AiSafetySummaryContext(
    val level: SafetyLevel,
    val reasonCodes: Set<String>,
    val questionnaireVersion: String,
    val reviewDueAt: LocalDate
)

data class AiNutritionContext(
    val schemaVersion: String,
    val generatedAt: Instant,
    val profile: AiProfileContext?,
    val goal: AiGoalContext?,
    val latestWeight: AiLatestWeightContext?,
    val safetySummary: AiSafetySummaryContext,
    /**
     * Une IA peut expliquer ou proposer des idées, mais ne reçoit jamais le
     * droit de modifier la cible déterministe enregistrée par l'application.
     */
    val calorieTargetMayBeModified: Boolean = false
)

enum class AiContextDeniedReason {
    POLICY_DISABLED,
    INVALID_OR_OUTDATED_CONSENT,
    REQUIRED_SAFETY_SCOPE_MISSING,
    QUESTIONNAIRE_STALE,
    PROFILE_OUTSIDE_SUPPORTED_RANGE,
    PROFESSIONAL_REVIEW_REQUIRED
}

sealed interface BuildAiNutritionContextResult {
    data class Ready(
        val context: AiNutritionContext
    ) : BuildAiNutritionContextResult

    data class Denied(
        val reason: AiContextDeniedReason
    ) : BuildAiNutritionContextResult
}

class BuildAiNutritionContextUseCase @Inject constructor(
    private val clock: Clock
) {

    operator fun invoke(
        setup: UserSetup
    ): BuildAiNutritionContextResult {
        val now = clock.instant()
        val today = LocalDate.now(clock)
        val policy = setup.aiDataAccessPolicy

        if (!policy.enabled) {
            return BuildAiNutritionContextResult.Denied(
                AiContextDeniedReason.POLICY_DISABLED
            )
        }

        if (
            policy.id != AiDataAccessPolicy.CURRENT_POLICY_ID ||
            policy.consentVersion != CURRENT_AI_CONSENT_VERSION ||
            policy.grantedAt == null ||
            policy.grantedAt.isAfter(now) ||
            policy.updatedAt.isAfter(now) ||
            policy.grantedAt.isAfter(policy.updatedAt) ||
            policy.revokedAt != null ||
            policy.scopes.isEmpty()
        ) {
            return BuildAiNutritionContextResult.Denied(
                AiContextDeniedReason.INVALID_OR_OUTDATED_CONSENT
            )
        }

        /*
         * Les garde-fous ne peuvent pas être appliqués si la future IA ne
         * reçoit pas au minimum le résumé de sécurité consenti.
         */
        if (AiDataScope.SAFETY_SUMMARY !in policy.scopes) {
            return BuildAiNutritionContextResult.Denied(
                AiContextDeniedReason.REQUIRED_SAFETY_SCOPE_MISSING
            )
        }

        val safetyProfile = setup.safetyProfile
        if (
            safetyProfile.questionnaireVersion !=
                HealthQuestionnairePolicy.QUESTIONNAIRE_VERSION ||
            safetyProfile.assessmentVersion !=
                HealthQuestionnairePolicy.ASSESSMENT_VERSION ||
            safetyProfile.disclaimerVersion !=
                HealthQuestionnairePolicy.DISCLAIMER_VERSION ||
            HealthQuestionnairePolicy.isReviewDue(
                reviewDueAt = safetyProfile.reviewDueAt,
                today = today
            )
        ) {
            return BuildAiNutritionContextResult.Denied(
                AiContextDeniedReason.QUESTIONNAIRE_STALE
            )
        }

        if (
            safetyProfile.level ==
            SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED
        ) {
            return BuildAiNutritionContextResult.Denied(
                AiContextDeniedReason.PROFESSIONAL_REVIEW_REQUIRED
            )
        }

        val ageYears = Period.between(
            setup.profile.birthDate,
            today
        ).years
        if (
            ageYears !in
                PlanConstraints.MIN_AGE_YEARS..
                    PlanConstraints.MAX_AGE_YEARS
        ) {
            /*
             * La couche IA applique au minimum les mêmes limites que le moteur
             * déterministe. Elle ne doit pas contourner une équation devenue
             * hors domaine depuis la création du profil.
             */
            return BuildAiNutritionContextResult.Denied(
                AiContextDeniedReason.PROFILE_OUTSIDE_SUPPORTED_RANGE
            )
        }

        val profileContext =
            if (AiDataScope.PROFILE in policy.scopes) {
                AiProfileContext(
                    /*
                     * L'âge calculé suffit au contexte nutritionnel : la date
                     * de naissance exacte n'est jamais exposée.
                     */
                    ageYears = ageYears,
                    heightCm = setup.profile.heightCm,
                    activityLevel = setup.profile.activityLevel
                )
            } else {
                null
            }

        val goalContext =
            if (AiDataScope.GOAL in policy.scopes) {
                AiGoalContext(
                    targetWeightKg = setup.goal.targetWeightKg,
                    pace = setup.goal.pace,
                    targetGainKgPerWeek =
                        setup.goal.targetGainKgPerWeek,
                    currentDailyCalorieTarget =
                        setup.goal.dailyCalorieTarget,
                    calculationDate =
                        setup.goal.calculationDate
                )
            } else {
                null
            }

        val latestWeightContext =
            if (AiDataScope.LATEST_WEIGHT in policy.scopes) {
                AiLatestWeightContext(
                    weightKg = setup.latestWeight.weightKg,
                    /*
                     * Une ancienneté relative est suffisante et évite de
                     * transmettre l'horodatage exact de la mesure.
                     */
                    measurementAgeDays = Duration.between(
                        setup.latestWeight.measuredAt,
                        now
                    )
                        .toDays()
                        .coerceAtLeast(0L)
                )
            } else {
                null
            }

        return BuildAiNutritionContextResult.Ready(
            AiNutritionContext(
                schemaVersion = AI_CONTEXT_SCHEMA_VERSION,
                generatedAt = now,
                profile = profileContext,
                goal = goalContext,
                latestWeight = latestWeightContext,
                safetySummary = AiSafetySummaryContext(
                    level = safetyProfile.level,
                    reasonCodes = safetyProfile.reasons
                        .mapTo(sortedSetOf()) { it.name },
                    questionnaireVersion =
                        safetyProfile.questionnaireVersion,
                    reviewDueAt = safetyProfile.reviewDueAt
                )
            )
        )
    }

    companion object {
        const val CURRENT_AI_CONSENT_VERSION =
            "ai-personalization-consent-1.0.0"
        const val AI_CONTEXT_SCHEMA_VERSION =
            "nutricoach-ai-context-1.0.0"
    }
}
