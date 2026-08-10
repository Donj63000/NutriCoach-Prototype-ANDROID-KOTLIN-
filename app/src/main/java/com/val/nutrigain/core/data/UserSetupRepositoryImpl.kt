// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.data

import com.`val`.nutrigain.core.database.ProfileDao
import com.`val`.nutrigain.core.database.entity.AiDataAccessPolicyEntity
import com.`val`.nutrigain.core.database.entity.GoalEntity
import com.`val`.nutrigain.core.database.entity.SafetyProfileEntity
import com.`val`.nutrigain.core.database.entity.UserProfileEntity
import com.`val`.nutrigain.core.database.entity.WeightEntryEntity
import com.`val`.nutrigain.core.model.AiDataAccessPolicy
import com.`val`.nutrigain.core.model.AiDataScope
import com.`val`.nutrigain.core.model.Goal
import com.`val`.nutrigain.core.model.HealthQuestionnaireAnswers
import com.`val`.nutrigain.core.model.SafetyProfile
import com.`val`.nutrigain.core.model.SafetyReason
import com.`val`.nutrigain.core.model.UserProfile
import com.`val`.nutrigain.core.model.UserSetup
import com.`val`.nutrigain.core.model.WeightMeasurement
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class UserSetupRepositoryImpl @Inject constructor(
    private val profileDaoProvider: Provider<ProfileDao>
) : UserSetupRepository {

    override fun observeSetup(): Flow<StoredUserSetup> {
        return flow {
            /*
             * Le DAO est résolu pendant la collecte, pas pendant la création
             * du ViewModel. Une erreur de clé ou d'ouverture de base peut ainsi
             * être transformée en état sûr par AppStartViewModel.
             */
            emitAll(profileDaoProvider.get().observeCurrentSetup())
        }
            .map { stored ->
                val coreParts = listOf(
                    stored.profile,
                    stored.safetyProfile,
                    stored.goal,
                    stored.latestWeight
                )

                when {
                    /*
                     * Une politique IA orpheline ne doit pas bloquer une remise
                     * en route : le prochain onboarding la remplace par une
                     * politique explicitement désactivée.
                     */
                    coreParts.all { it == null } ->
                        StoredUserSetup.Empty

                    coreParts.any { it == null } ||
                        stored.aiDataAccessPolicy == null ->
                        StoredUserSetup.Incomplete

                    else -> StoredUserSetup.Ready(
                        UserSetup(
                            profile = checkNotNull(
                                stored.profile
                            ).toDomain(),
                            safetyProfile = checkNotNull(
                                stored.safetyProfile
                            ).toDomain(),
                            goal = checkNotNull(
                                stored.goal
                            ).toDomain(),
                            latestWeight = checkNotNull(
                                stored.latestWeight
                            ).toDomain(),
                            aiDataAccessPolicy = checkNotNull(
                                stored.aiDataAccessPolicy
                            ).toDomain()
                        )
                    )
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
    }

    override suspend fun saveInitialSetup(setup: UserSetup) {
        withContext(Dispatchers.IO) {
            profileDaoProvider.get().saveInitialSetup(
                profile = setup.profile.toEntity(),
                safetyProfile = setup.safetyProfile.toEntity(),
                goal = setup.goal.toEntity(),
                initialWeight = setup.latestWeight.toEntity(),
                aiDataAccessPolicy =
                    setup.aiDataAccessPolicy.toEntity()
            )
        }
    }

    override suspend fun saveSafetyReview(
        safetyProfile: SafetyProfile,
        goal: Goal
    ) {
        withContext(Dispatchers.IO) {
            profileDaoProvider.get().saveSafetyReview(
                safetyProfile = safetyProfile.toEntity(),
                goal = goal.toEntity()
            )
        }
    }

    override suspend fun saveAiDataAccessPolicy(
        policy: AiDataAccessPolicy
    ) {
        withContext(Dispatchers.IO) {
            profileDaoProvider
                .get()
                .upsertAiDataAccessPolicy(policy.toEntity())
        }
    }

    override suspend fun deleteAllUserData() {
        withContext(Dispatchers.IO) {
            profileDaoProvider.get().deleteAllUserData()
        }
    }
}

private fun UserProfileEntity.toDomain(): UserProfile {
    return UserProfile(
        id = id,
        birthDate = LocalDate.parse(birthDate),
        heightCm = heightCm,
        metabolicSex = enumValueOrFail(metabolicSex),
        activityLevel = enumValueOrFail(activityLevel),
        createdAt = Instant.ofEpochMilli(createdAtEpochMs),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMs)
    )
}

private fun SafetyProfileEntity.toDomain(): SafetyProfile {
    return SafetyProfile(
        id = id,
        answers = HealthQuestionnaireAnswers(
            unintentionalWeightLoss =
                enumValueOrFail(unintentionalWeightLoss),
            reducedAppetite =
                enumValueOrFail(reducedAppetite),
            swallowingDifficultyOrPersistentVomiting =
                enumValueOrFail(
                    swallowingDifficultyOrPersistentVomiting
                ),
            pregnantOrBreastfeeding =
                enumValueOrFail(pregnantOrBreastfeeding),
            eatingDisorderHistory =
                enumValueOrFail(eatingDisorderHistory),
            significantDigestiveSymptoms =
                enumValueOrFail(significantDigestiveSymptoms),
            relevantMedicalCondition =
                enumValueOrFail(relevantMedicalCondition),
            relevantMedication =
                enumValueOrFail(relevantMedication),
            foodAllergiesOrIntolerances =
                enumValueOrFail(foodAllergiesOrIntolerances)
        ),
        medicalContextNote = medicalContextNote,
        level = enumValueOrFail(safetyLevel),
        reasons = parseReasons(assessmentReasons),
        questionnaireVersion = questionnaireVersion,
        assessmentVersion = assessmentVersion,
        disclaimerVersion = disclaimerVersion,
        createdAt = Instant.ofEpochMilli(createdAtEpochMs),
        acknowledgedAt =
            Instant.ofEpochMilli(acknowledgedAtEpochMs),
        answeredAt = Instant.ofEpochMilli(answeredAtEpochMs),
        reviewDueAt = LocalDate.parse(reviewDueDate),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMs)
    )
}

private fun GoalEntity.toDomain(): Goal {
    return Goal(
        id = id,
        startWeightKg = startWeightKg,
        targetWeightKg = targetWeightKg,
        startDate = LocalDate.parse(startDate),
        indicativeTargetDate =
            indicativeTargetDate?.let(LocalDate::parse),
        calculationWeightKg = calculationWeightKg,
        calculationDate = LocalDate.parse(calculationDate),
        estimatedMaintenanceCalories =
            estimatedMaintenanceCalories,
        dailyCalorieTarget = dailyCalorieTarget,
        initialSurplusCalories = initialSurplusCalories,
        targetGainKgPerWeek = targetGainKgPerWeek,
        pace = enumValueOrFail(pace),
        initialBmi = initialBmi,
        active = active,
        calculationVersion = calculationVersion,
        createdAt = Instant.ofEpochMilli(createdAtEpochMs),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMs)
    )
}

private fun WeightEntryEntity.toDomain(): WeightMeasurement {
    return WeightMeasurement(
        id = id,
        weightKg = weightKg,
        measuredAt = Instant.ofEpochMilli(measuredAtEpochMs),
        note = note,
        createdAt = Instant.ofEpochMilli(createdAtEpochMs),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMs)
    )
}

private fun AiDataAccessPolicyEntity.toDomain(): AiDataAccessPolicy {
    val scopes = buildSet {
        if (profileScopeEnabled) {
            add(AiDataScope.PROFILE)
        }
        if (goalScopeEnabled) {
            add(AiDataScope.GOAL)
        }
        if (latestWeightScopeEnabled) {
            add(AiDataScope.LATEST_WEIGHT)
        }
        if (safetySummaryScopeEnabled) {
            add(AiDataScope.SAFETY_SUMMARY)
        }
    }

    return AiDataAccessPolicy(
        id = id,
        enabled = enabled,
        scopes = scopes,
        consentVersion = consentVersion,
        grantedAt = grantedAtEpochMs?.let(
            Instant::ofEpochMilli
        ),
        revokedAt = revokedAtEpochMs?.let(
            Instant::ofEpochMilli
        ),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMs)
    )
}

private fun UserProfile.toEntity(): UserProfileEntity {
    return UserProfileEntity(
        id = id,
        birthDate = birthDate.toString(),
        heightCm = heightCm,
        metabolicSex = metabolicSex.name,
        activityLevel = activityLevel.name,
        createdAtEpochMs = createdAt.toEpochMilli(),
        updatedAtEpochMs = updatedAt.toEpochMilli()
    )
}

private fun SafetyProfile.toEntity(): SafetyProfileEntity {
    return SafetyProfileEntity(
        id = id,
        unintentionalWeightLoss =
            answers.unintentionalWeightLoss.name,
        reducedAppetite = answers.reducedAppetite.name,
        swallowingDifficultyOrPersistentVomiting =
            answers
                .swallowingDifficultyOrPersistentVomiting
                .name,
        pregnantOrBreastfeeding =
            answers.pregnantOrBreastfeeding.name,
        eatingDisorderHistory =
            answers.eatingDisorderHistory.name,
        significantDigestiveSymptoms =
            answers.significantDigestiveSymptoms.name,
        relevantMedicalCondition =
            answers.relevantMedicalCondition.name,
        relevantMedication = answers.relevantMedication.name,
        foodAllergiesOrIntolerances =
            answers.foodAllergiesOrIntolerances.name,
        medicalContextNote = medicalContextNote,
        safetyLevel = level.name,
        assessmentReasons = reasons
            .asSequence()
            .map { reason -> reason.name }
            .sorted()
            .joinToString(REASON_SEPARATOR),
        questionnaireVersion = questionnaireVersion,
        assessmentVersion = assessmentVersion,
        disclaimerVersion = disclaimerVersion,
        createdAtEpochMs = createdAt.toEpochMilli(),
        acknowledgedAtEpochMs = acknowledgedAt.toEpochMilli(),
        answeredAtEpochMs = answeredAt.toEpochMilli(),
        reviewDueDate = reviewDueAt.toString(),
        updatedAtEpochMs = updatedAt.toEpochMilli()
    )
}

private fun Goal.toEntity(): GoalEntity {
    return GoalEntity(
        id = id,
        startWeightKg = startWeightKg,
        targetWeightKg = targetWeightKg,
        startDate = startDate.toString(),
        indicativeTargetDate = indicativeTargetDate?.toString(),
        calculationWeightKg = calculationWeightKg,
        calculationDate = calculationDate.toString(),
        estimatedMaintenanceCalories =
            estimatedMaintenanceCalories,
        dailyCalorieTarget = dailyCalorieTarget,
        initialSurplusCalories = initialSurplusCalories,
        targetGainKgPerWeek = targetGainKgPerWeek,
        pace = pace.name,
        initialBmi = initialBmi,
        active = active,
        calculationVersion = calculationVersion,
        createdAtEpochMs = createdAt.toEpochMilli(),
        updatedAtEpochMs = updatedAt.toEpochMilli()
    )
}

private fun WeightMeasurement.toEntity(): WeightEntryEntity {
    return WeightEntryEntity(
        id = id,
        weightKg = weightKg,
        measuredAtEpochMs = measuredAt.toEpochMilli(),
        note = note,
        createdAtEpochMs = createdAt.toEpochMilli(),
        updatedAtEpochMs = updatedAt.toEpochMilli()
    )
}

private fun AiDataAccessPolicy.toEntity(): AiDataAccessPolicyEntity {
    return AiDataAccessPolicyEntity(
        id = id,
        enabled = enabled,
        profileScopeEnabled = AiDataScope.PROFILE in scopes,
        goalScopeEnabled = AiDataScope.GOAL in scopes,
        latestWeightScopeEnabled =
            AiDataScope.LATEST_WEIGHT in scopes,
        safetySummaryScopeEnabled =
            AiDataScope.SAFETY_SUMMARY in scopes,
        consentVersion = consentVersion,
        grantedAtEpochMs = grantedAt?.toEpochMilli(),
        revokedAtEpochMs = revokedAt?.toEpochMilli(),
        updatedAtEpochMs = updatedAt.toEpochMilli()
    )
}

private fun parseReasons(
    storedValue: String
): Set<SafetyReason> {
    if (storedValue.isBlank()) {
        return emptySet()
    }

    return storedValue
        .split(REASON_SEPARATOR)
        .mapTo(linkedSetOf()) { reason ->
            enumValueOrFail<SafetyReason>(reason)
        }
}

private inline fun <reified T : Enum<T>> enumValueOrFail(
    storedValue: String
): T {
    return enumValues<T>().firstOrNull { value ->
        value.name == storedValue
    } ?: error(
        "Valeur persistée inconnue pour ${T::class.java.simpleName}."
    )
}

private const val REASON_SEPARATOR = ","
