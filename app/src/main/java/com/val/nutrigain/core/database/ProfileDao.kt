// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.`val`.nutrigain.core.database.entity.AiDataAccessPolicyEntity
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
            safety.reduced_appetite
                AS safety_reduced_appetite,
            safety.swallowing_difficulty_or_persistent_vomiting
                AS safety_swallowing_difficulty_or_persistent_vomiting,
            safety.pregnant_or_breastfeeding
                AS safety_pregnant_or_breastfeeding,
            safety.eating_disorder_history
                AS safety_eating_disorder_history,
            safety.significant_digestive_symptoms
                AS safety_significant_digestive_symptoms,
            safety.relevant_medical_condition
                AS safety_relevant_medical_condition,
            safety.relevant_medication
                AS safety_relevant_medication,
            safety.food_allergies_or_intolerances
                AS safety_food_allergies_or_intolerances,
            safety.medical_context_note
                AS safety_medical_context_note,
            safety.safety_level AS safety_safety_level,
            safety.assessment_reasons
                AS safety_assessment_reasons,
            safety.questionnaire_version
                AS safety_questionnaire_version,
            safety.assessment_version
                AS safety_assessment_version,
            safety.disclaimer_version
                AS safety_disclaimer_version,
            safety.created_at_epoch_ms
                AS safety_created_at_epoch_ms,
            safety.acknowledged_at_epoch_ms
                AS safety_acknowledged_at_epoch_ms,
            safety.answered_at_epoch_ms
                AS safety_answered_at_epoch_ms,
            safety.review_due_date
                AS safety_review_due_date,
            safety.updated_at_epoch_ms
                AS safety_updated_at_epoch_ms,

            active_goal.id AS goal_id,
            active_goal.start_weight_kg AS goal_start_weight_kg,
            active_goal.target_weight_kg AS goal_target_weight_kg,
            active_goal.start_date AS goal_start_date,
            active_goal.indicative_target_date
                AS goal_indicative_target_date,
            active_goal.calculation_weight_kg
                AS goal_calculation_weight_kg,
            active_goal.calculation_date
                AS goal_calculation_date,
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
            active_goal.calculation_version
                AS goal_calculation_version,
            active_goal.created_at_epoch_ms
                AS goal_created_at_epoch_ms,
            active_goal.updated_at_epoch_ms
                AS goal_updated_at_epoch_ms,

            latest_weight.id AS weight_id,
            latest_weight.weight_kg AS weight_weight_kg,
            latest_weight.measured_at_epoch_ms
                AS weight_measured_at_epoch_ms,
            latest_weight.note AS weight_note,
            latest_weight.created_at_epoch_ms
                AS weight_created_at_epoch_ms,
            latest_weight.updated_at_epoch_ms
                AS weight_updated_at_epoch_ms,

            ai_policy.id AS ai_policy_id,
            ai_policy.enabled AS ai_policy_enabled,
            ai_policy.profile_scope_enabled
                AS ai_policy_profile_scope_enabled,
            ai_policy.goal_scope_enabled
                AS ai_policy_goal_scope_enabled,
            ai_policy.latest_weight_scope_enabled
                AS ai_policy_latest_weight_scope_enabled,
            ai_policy.safety_summary_scope_enabled
                AS ai_policy_safety_summary_scope_enabled,
            ai_policy.consent_version
                AS ai_policy_consent_version,
            ai_policy.granted_at_epoch_ms
                AS ai_policy_granted_at_epoch_ms,
            ai_policy.revoked_at_epoch_ms
                AS ai_policy_revoked_at_epoch_ms,
            ai_policy.updated_at_epoch_ms
                AS ai_policy_updated_at_epoch_ms
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
        LEFT JOIN ai_data_access_policy AS ai_policy
            ON ai_policy.id = 'current-ai-policy'
        LIMIT 1
        """
    )
    abstract fun observeCurrentSetup(): Flow<StoredUserSetupEntity>

    @Upsert
    abstract suspend fun upsertProfile(entity: UserProfileEntity)

    @Upsert
    abstract suspend fun upsertSafetyProfile(
        entity: SafetyProfileEntity
    )

    @Upsert
    abstract suspend fun upsertGoal(entity: GoalEntity)

    @Upsert
    abstract suspend fun upsertWeight(entity: WeightEntryEntity)

    @Upsert
    abstract suspend fun upsertAiDataAccessPolicy(
        entity: AiDataAccessPolicyEntity
    )

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
        initialWeight: WeightEntryEntity,
        aiDataAccessPolicy: AiDataAccessPolicyEntity
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
        upsertAiDataAccessPolicy(aiDataAccessPolicy)
    }

    @Transaction
    open suspend fun saveSafetyReview(
        safetyProfile: SafetyProfileEntity,
        goal: GoalEntity
    ) {
        /*
         * Le niveau de prudence et le repère calorique doivent toujours être
         * mis à jour ensemble : un signal médical ne doit jamais laisser une
         * ancienne cible automatique active.
         */
        upsertSafetyProfile(safetyProfile)
        upsertGoal(goal)
    }

    @Query("DELETE FROM weight_entry")
    abstract suspend fun deleteAllWeights()

    @Query("DELETE FROM goal")
    abstract suspend fun deleteAllGoals()

    @Query("DELETE FROM safety_profile")
    abstract suspend fun deleteSafetyProfile()

    @Query("DELETE FROM user_profile")
    abstract suspend fun deleteUserProfile()

    @Query("DELETE FROM ai_data_access_policy")
    abstract suspend fun deleteAiDataAccessPolicy()

    @Transaction
    open suspend fun deleteAllUserData() {
        /*
         * La suppression est atomique afin que le démarrage passe directement
         * de l'état complet à l'état vide, sans erreur intermédiaire.
         */
        deleteAllWeights()
        deleteAllGoals()
        deleteSafetyProfile()
        deleteUserProfile()
        deleteAiDataAccessPolicy()
    }
}
