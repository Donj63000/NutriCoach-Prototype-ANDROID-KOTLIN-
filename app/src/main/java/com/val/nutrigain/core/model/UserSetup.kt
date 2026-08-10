// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.model

import java.time.Instant
import java.time.LocalDate

enum class MetabolicSex {
    FEMALE,
    MALE
}

enum class ActivityLevel(
    val maintenanceMultiplier: Double
) {
    SEDENTARY(1.20),
    LIGHT(1.375),
    MODERATE(1.55),
    HIGH(1.725)
}

enum class GainPace(
    val initialSurplusCalories: Int,
    val targetGainKgPerWeek: Double
) {
    GENTLE(
        initialSurplusCalories = 200,
        targetGainKgPerWeek = 0.15
    ),
    PROGRESSIVE(
        initialSurplusCalories = 300,
        targetGainKgPerWeek = 0.25
    ),
    SUSTAINED(
        initialSurplusCalories = 400,
        targetGainKgPerWeek = 0.35
    )
}

/**
 * Réponse explicite à une question de prudence.
 *
 * L'état UNKNOWN n'existe volontairement pas dans le modèle persisté final :
 * l'absence de réponse appartient au brouillon d'interface, tandis que UNSURE
 * est un choix conscient qui déclenche des garde-fous.
 */
enum class HealthAnswer {
    YES,
    NO,
    UNSURE
}

/**
 * Identifiants stables des questions médicales.
 *
 * Ces valeurs sont persistées et peuvent être exposées sous forme de codes
 * techniques à une future couche d'IA. Elles ne doivent donc jamais être
 * renommées sans migration et changement de version du questionnaire.
 */
enum class HealthQuestion {
    UNINTENTIONAL_WEIGHT_LOSS,
    REDUCED_APPETITE,
    SWALLOWING_DIFFICULTY_OR_PERSISTENT_VOMITING,
    PREGNANT_OR_BREASTFEEDING,
    EATING_DISORDER_HISTORY,
    SIGNIFICANT_DIGESTIVE_SYMPTOMS,
    RELEVANT_MEDICAL_CONDITION,
    RELEVANT_MEDICATION,
    FOOD_ALLERGIES_OR_INTOLERANCES
}

data class HealthQuestionnaireAnswers(
    val unintentionalWeightLoss: HealthAnswer,
    val reducedAppetite: HealthAnswer,
    val swallowingDifficultyOrPersistentVomiting: HealthAnswer,
    val pregnantOrBreastfeeding: HealthAnswer,
    val eatingDisorderHistory: HealthAnswer,
    val significantDigestiveSymptoms: HealthAnswer,
    val relevantMedicalCondition: HealthAnswer,
    val relevantMedication: HealthAnswer,
    val foodAllergiesOrIntolerances: HealthAnswer
) {
    operator fun get(question: HealthQuestion): HealthAnswer {
        return when (question) {
            HealthQuestion.UNINTENTIONAL_WEIGHT_LOSS ->
                unintentionalWeightLoss

            HealthQuestion.REDUCED_APPETITE ->
                reducedAppetite

            HealthQuestion.SWALLOWING_DIFFICULTY_OR_PERSISTENT_VOMITING ->
                swallowingDifficultyOrPersistentVomiting

            HealthQuestion.PREGNANT_OR_BREASTFEEDING ->
                pregnantOrBreastfeeding

            HealthQuestion.EATING_DISORDER_HISTORY ->
                eatingDisorderHistory

            HealthQuestion.SIGNIFICANT_DIGESTIVE_SYMPTOMS ->
                significantDigestiveSymptoms

            HealthQuestion.RELEVANT_MEDICAL_CONDITION ->
                relevantMedicalCondition

            HealthQuestion.RELEVANT_MEDICATION ->
                relevantMedication

            HealthQuestion.FOOD_ALLERGIES_OR_INTOLERANCES ->
                foodAllergiesOrIntolerances
        }
    }

    fun asMap(): Map<HealthQuestion, HealthAnswer> {
        return HealthQuestion.entries.associateWith(::get)
    }

    companion object {
        /**
         * Construit uniquement un questionnaire complet.
         *
         * L'interface peut manipuler une Map partielle pendant la saisie, mais
         * aucune donnée incomplète ne peut franchir la frontière du domaine.
         */
        fun from(
            answers: Map<HealthQuestion, HealthAnswer>
        ): HealthQuestionnaireAnswers? {
            if (!HealthQuestion.entries.all(answers::containsKey)) {
                return null
            }

            return HealthQuestionnaireAnswers(
                unintentionalWeightLoss = checkNotNull(
                    answers[HealthQuestion.UNINTENTIONAL_WEIGHT_LOSS]
                ),
                reducedAppetite = checkNotNull(
                    answers[HealthQuestion.REDUCED_APPETITE]
                ),
                swallowingDifficultyOrPersistentVomiting = checkNotNull(
                    answers[
                        HealthQuestion
                            .SWALLOWING_DIFFICULTY_OR_PERSISTENT_VOMITING
                    ]
                ),
                pregnantOrBreastfeeding = checkNotNull(
                    answers[HealthQuestion.PREGNANT_OR_BREASTFEEDING]
                ),
                eatingDisorderHistory = checkNotNull(
                    answers[HealthQuestion.EATING_DISORDER_HISTORY]
                ),
                significantDigestiveSymptoms = checkNotNull(
                    answers[
                        HealthQuestion.SIGNIFICANT_DIGESTIVE_SYMPTOMS
                    ]
                ),
                relevantMedicalCondition = checkNotNull(
                    answers[HealthQuestion.RELEVANT_MEDICAL_CONDITION]
                ),
                relevantMedication = checkNotNull(
                    answers[HealthQuestion.RELEVANT_MEDICATION]
                ),
                foodAllergiesOrIntolerances = checkNotNull(
                    answers[
                        HealthQuestion.FOOD_ALLERGIES_OR_INTOLERANCES
                    ]
                )
            )
        }
    }
}

enum class SafetyLevel {
    NORMAL,
    CAUTION,
    PROFESSIONAL_REVIEW_REQUIRED
}

/**
 * Raisons techniques expliquant un niveau de prudence.
 *
 * Elles sont conservées avec le résultat pour rendre les décisions auditables
 * sans exposer la note médicale libre.
 */
enum class SafetyReason {
    AGE_OUTSIDE_SUPPORTED_RANGE,
    CURRENT_BMI_MARKEDLY_LOW,
    CURRENT_BMI_HIGH,
    TARGET_BMI_HIGH,
    CURRENT_BMI_OUTSIDE_REFERENCE,
    TARGET_BMI_OUTSIDE_REFERENCE,
    UNINTENTIONAL_WEIGHT_LOSS,
    REDUCED_APPETITE,
    SWALLOWING_DIFFICULTY_OR_PERSISTENT_VOMITING,
    PREGNANCY_OR_BREASTFEEDING,
    EATING_DISORDER_HISTORY,
    SIGNIFICANT_DIGESTIVE_SYMPTOMS,
    RELEVANT_MEDICAL_CONDITION,
    RELEVANT_MEDICATION,
    FOOD_ALLERGIES_OR_INTOLERANCES,
    UNCERTAIN_CRITICAL_ANSWER,
    QUESTIONNAIRE_REVIEW_REQUIRED
}

data class UserProfile(
    val id: String,
    val birthDate: LocalDate,
    val heightCm: Double,
    val metabolicSex: MetabolicSex,
    val activityLevel: ActivityLevel,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class SafetyProfile(
    val id: String,
    val answers: HealthQuestionnaireAnswers,
    val medicalContextNote: String?,
    val level: SafetyLevel,
    val reasons: Set<SafetyReason>,
    val questionnaireVersion: String,
    val assessmentVersion: String,
    val disclaimerVersion: String,
    val createdAt: Instant,
    val acknowledgedAt: Instant,
    val answeredAt: Instant,
    val reviewDueAt: LocalDate,
    val updatedAt: Instant
)

data class Goal(
    val id: String,
    val startWeightKg: Double,
    val targetWeightKg: Double,
    val startDate: LocalDate,
    val indicativeTargetDate: LocalDate?,
    val calculationWeightKg: Double,
    val calculationDate: LocalDate,
    val estimatedMaintenanceCalories: Int?,
    val dailyCalorieTarget: Int?,
    val initialSurplusCalories: Int?,
    val targetGainKgPerWeek: Double,
    val pace: GainPace,
    val initialBmi: Double,
    val active: Boolean,
    val calculationVersion: String,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class WeightMeasurement(
    val id: String,
    val weightKg: Double,
    val measuredAt: Instant,
    val note: String?,
    val createdAt: Instant,
    val updatedAt: Instant
)

enum class AiDataScope {
    PROFILE,
    GOAL,
    LATEST_WEIGHT,
    SAFETY_SUMMARY
}

/**
 * Autorisation locale séparée du questionnaire médical.
 *
 * La politique est créée désactivée. Une future fonctionnalité devra recueillir
 * un consentement versionné avant de l'activer ; le simple fait que les données
 * existent localement ne vaut jamais autorisation d'accès.
 */
data class AiDataAccessPolicy(
    val id: String,
    val enabled: Boolean,
    val scopes: Set<AiDataScope>,
    val consentVersion: String?,
    val grantedAt: Instant?,
    val revokedAt: Instant?,
    val updatedAt: Instant
) {
    companion object {
        const val CURRENT_POLICY_ID = "current-ai-policy"

        fun disabled(now: Instant): AiDataAccessPolicy {
            return AiDataAccessPolicy(
                id = CURRENT_POLICY_ID,
                enabled = false,
                scopes = emptySet(),
                consentVersion = null,
                grantedAt = null,
                revokedAt = null,
                updatedAt = now
            )
        }
    }
}

data class UserSetup(
    val profile: UserProfile,
    val safetyProfile: SafetyProfile,
    val goal: Goal,
    val latestWeight: WeightMeasurement,
    val aiDataAccessPolicy: AiDataAccessPolicy
)
