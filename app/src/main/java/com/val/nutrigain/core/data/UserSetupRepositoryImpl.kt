// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.data

import com.`val`.nutrigain.core.database.ProfileDao
import com.`val`.nutrigain.core.database.entity.GoalEntity
import com.`val`.nutrigain.core.database.entity.SafetyProfileEntity
import com.`val`.nutrigain.core.database.entity.UserProfileEntity
import com.`val`.nutrigain.core.database.entity.WeightEntryEntity
import com.`val`.nutrigain.core.model.Goal
import com.`val`.nutrigain.core.model.SafetyAnswers
import com.`val`.nutrigain.core.model.SafetyProfile
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
             * du ViewModel. Une erreur de clé ou d’ouverture de base peut ainsi
             * être transformée en état sûr par AppStartViewModel.
             */
            emitAll(profileDaoProvider.get().observeCurrentSetup())
        }
            .map { stored ->
                val storedParts = listOf(
                    stored.profile,
                    stored.safetyProfile,
                    stored.goal,
                    stored.latestWeight
                )

                when {
                    storedParts.all { it == null } ->
                        StoredUserSetup.Empty

                    storedParts.any { it == null } ->
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
                initialWeight = setup.latestWeight.toEntity()
            )
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
        answers = SafetyAnswers(
            unintentionalWeightLoss = unintentionalWeightLoss,
            pregnantOrBreastfeeding = pregnantOrBreastfeeding,
            eatingDisorderHistory = eatingDisorderHistory,
            significantDigestiveSymptoms =
                significantDigestiveSymptoms,
            relevantMedicalCondition = relevantMedicalCondition,
            relevantMedication = relevantMedication
        ),
        level = enumValueOrFail(safetyLevel),
        acknowledgedAt =
            Instant.ofEpochMilli(acknowledgedAtEpochMs),
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
            answers.unintentionalWeightLoss,
        pregnantOrBreastfeeding =
            answers.pregnantOrBreastfeeding,
        eatingDisorderHistory =
            answers.eatingDisorderHistory,
        significantDigestiveSymptoms =
            answers.significantDigestiveSymptoms,
        relevantMedicalCondition =
            answers.relevantMedicalCondition,
        relevantMedication = answers.relevantMedication,
        safetyLevel = level.name,
        acknowledgedAtEpochMs = acknowledgedAt.toEpochMilli(),
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

private inline fun <reified T : Enum<T>> enumValueOrFail(
    storedValue: String
): T {
    return enumValues<T>().firstOrNull { value ->
        value.name == storedValue
    } ?: error(
        "Valeur persistée inconnue pour ${T::class.java.simpleName}."
    )
}
