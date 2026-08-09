// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.`val`.nutrigain.core.database.entity.GoalEntity
import com.`val`.nutrigain.core.database.entity.SafetyProfileEntity
import com.`val`.nutrigain.core.database.entity.UserProfileEntity
import com.`val`.nutrigain.core.database.entity.WeightEntryEntity
import com.`val`.nutrigain.core.database.model.StoredUserSetupEntity
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProfileDao {

    @Query(
        """
        SELECT
            profile.id AS profile_id,
            profile.birth_date AS profile_birth_date,
            profile.height_cm AS profile_height_cm,
            profile.metabolic_sex AS profile_metabolic_sex,
            profile.activity_level AS profile_activity_level,
            profile.created_at_epoch_ms AS profile_created_at_epoch_ms,
            profile.updated_at_epoch_ms AS profile_updated_at_epoch_ms,

            safety.id AS safety_id,
            safety.unintentional_weight_loss
                AS safety_unintentional_weight_loss,
            safety.pregnant_or_breastfeeding
                AS safety_pregnant_or_breastfeeding,
            safety.eating_disorder_history
                AS safety_eating_disorder_history,
            safety.significant_digestive_symptoms
                AS safety_significant_digestive_symptoms,
            safety.relevant_medical_condition
                AS safety_relevant_medical_condition,
            safety.relevant_medication AS safety_relevant_medication,
            safety.safety_level AS safety_safety_level,
            safety.acknowledged_at_epoch_ms
                AS safety_acknowledged_at_epoch_ms,
            safety.updated_at_epoch_ms AS safety_updated_at_epoch_ms,

            active_goal.id AS goal_id,
            active_goal.start_weight_kg AS goal_start_weight_kg,
            active_goal.target_weight_kg AS goal_target_weight_kg,
            active_goal.start_date AS goal_start_date,
            active_goal.indicative_target_date
                AS goal_indicative_target_date,
            active_goal.estimated_maintenance_calories
                AS goal_estimated_maintenance_calories,
            active_goal.daily_calorie_target
                AS goal_daily_calorie_target,
            active_goal.initial_surplus_calories
                AS goal_initial_surplus_calories,
            active_goal.target_gain_kg_per_week
                AS goal_target_gain_kg_per_week,
            active_goal.pace AS goal_pace,
            active_goal.initial_bmi AS goal_initial_bmi,
            active_goal.active AS goal_active,
            active_goal.calculation_version AS goal_calculation_version,
            active_goal.created_at_epoch_ms AS goal_created_at_epoch_ms,
            active_goal.updated_at_epoch_ms AS goal_updated_at_epoch_ms,

            latest_weight.id AS weight_id,
            latest_weight.weight_kg AS weight_weight_kg,
            latest_weight.measured_at_epoch_ms
                AS weight_measured_at_epoch_ms,
            latest_weight.note AS weight_note,
            latest_weight.created_at_epoch_ms
                AS weight_created_at_epoch_ms,
            latest_weight.updated_at_epoch_ms
                AS weight_updated_at_epoch_ms
        FROM (SELECT 1) AS seed
        LEFT JOIN user_profile AS profile
            ON profile.id = 'current-profile'
        LEFT JOIN safety_profile AS safety
            ON safety.id = 'current-safety-profile'
        LEFT JOIN goal AS active_goal
            ON active_goal.id = (
                SELECT id
                FROM goal
                WHERE active = 1
                ORDER BY
                    created_at_epoch_ms DESC,
                    id DESC
                LIMIT 1
            )
        LEFT JOIN weight_entry AS latest_weight
            ON latest_weight.id = (
                SELECT id
                FROM weight_entry
                ORDER BY
                    measured_at_epoch_ms DESC,
                    created_at_epoch_ms DESC,
                    id DESC
                LIMIT 1
            )
        LIMIT 1
        """
    )
    abstract fun observeCurrentSetup(): Flow<StoredUserSetupEntity>

    @Upsert
    abstract suspend fun upsertProfile(entity: UserProfileEntity)

    @Upsert
    abstract suspend fun upsertSafetyProfile(entity: SafetyProfileEntity)

    @Upsert
    abstract suspend fun upsertGoal(entity: GoalEntity)

    @Upsert
    abstract suspend fun upsertWeight(entity: WeightEntryEntity)

    @Query(
        """
        UPDATE goal
        SET active = 0, updated_at_epoch_ms = :updatedAtEpochMs
        WHERE active = 1
        """
    )
    abstract suspend fun deactivateCurrentGoals(
        updatedAtEpochMs: Long
    )

    @Transaction
    open suspend fun saveInitialSetup(
        profile: UserProfileEntity,
        safetyProfile: SafetyProfileEntity,
        goal: GoalEntity,
        initialWeight: WeightEntryEntity
    ) {
        /*
         * Une transaction unique empêche l'interface d'observer un profil
         * durablement partiel après une interruption du processus.
         */
        upsertProfile(profile)
        upsertSafetyProfile(safetyProfile)
        deactivateCurrentGoals(goal.updatedAtEpochMs)
        upsertGoal(goal)
        upsertWeight(initialWeight)
    }
}
