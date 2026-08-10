// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migration manuelle car le questionnaire v2 transforme plusieurs booléens en
 * réponses tri-état et ajoute des métadonnées d'audit. Une migration
 * destructive ferait perdre des données de santé et n'est jamais acceptable.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val migrationTimeEpochMs = System.currentTimeMillis()

        migrateSafetyProfile(
            database = database,
            migrationTimeEpochMs = migrationTimeEpochMs
        )
        migrateGoals(
            database = database,
            migrationTimeEpochMs = migrationTimeEpochMs
        )
        migrateWeightIndex(database)
        createAiDataAccessPolicy(
            database = database,
            migrationTimeEpochMs = migrationTimeEpochMs
        )
    }

    private fun migrateSafetyProfile(
        database: SupportSQLiteDatabase,
        migrationTimeEpochMs: Long
    ) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS safety_profile_new (
                id TEXT NOT NULL,
                unintentional_weight_loss TEXT NOT NULL,
                reduced_appetite TEXT NOT NULL,
                swallowing_difficulty_or_persistent_vomiting
                    TEXT NOT NULL,
                pregnant_or_breastfeeding TEXT NOT NULL,
                eating_disorder_history TEXT NOT NULL,
                significant_digestive_symptoms TEXT NOT NULL,
                relevant_medical_condition TEXT NOT NULL,
                relevant_medication TEXT NOT NULL,
                food_allergies_or_intolerances TEXT NOT NULL,
                medical_context_note TEXT,
                safety_level TEXT NOT NULL,
                assessment_reasons TEXT NOT NULL,
                questionnaire_version TEXT NOT NULL,
                assessment_version TEXT NOT NULL,
                disclaimer_version TEXT NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL,
                acknowledged_at_epoch_ms INTEGER NOT NULL,
                answered_at_epoch_ms INTEGER NOT NULL,
                review_due_date TEXT NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )

        /*
         * Les anciennes réponses restent connues. Les nouvelles questions
         * sont marquées "incertain" plutôt qu'inventées. Le plan automatique
         * est suspendu jusqu'à une révision explicite dans la nouvelle UI.
         */
        database.execSQL(
            """
            INSERT INTO safety_profile_new (
                id,
                unintentional_weight_loss,
                reduced_appetite,
                swallowing_difficulty_or_persistent_vomiting,
                pregnant_or_breastfeeding,
                eating_disorder_history,
                significant_digestive_symptoms,
                relevant_medical_condition,
                relevant_medication,
                food_allergies_or_intolerances,
                medical_context_note,
                safety_level,
                assessment_reasons,
                questionnaire_version,
                assessment_version,
                disclaimer_version,
                created_at_epoch_ms,
                acknowledged_at_epoch_ms,
                answered_at_epoch_ms,
                review_due_date,
                updated_at_epoch_ms
            )
            SELECT
                id,
                CASE
                    WHEN unintentional_weight_loss = 1
                        THEN 'YES'
                    WHEN unintentional_weight_loss = 0
                        THEN 'NO'
                    ELSE 'UNSURE'
                END,
                'UNSURE',
                'UNSURE',
                CASE
                    WHEN pregnant_or_breastfeeding = 1
                        THEN 'YES'
                    WHEN pregnant_or_breastfeeding = 0
                        THEN 'NO'
                    ELSE 'UNSURE'
                END,
                CASE
                    WHEN eating_disorder_history = 1
                        THEN 'YES'
                    WHEN eating_disorder_history = 0
                        THEN 'NO'
                    ELSE 'UNSURE'
                END,
                CASE
                    WHEN significant_digestive_symptoms = 1
                        THEN 'YES'
                    WHEN significant_digestive_symptoms = 0
                        THEN 'NO'
                    ELSE 'UNSURE'
                END,
                CASE
                    WHEN relevant_medical_condition = 1
                        THEN 'YES'
                    WHEN relevant_medical_condition = 0
                        THEN 'NO'
                    ELSE 'UNSURE'
                END,
                CASE
                    WHEN relevant_medication = 1
                        THEN 'YES'
                    WHEN relevant_medication = 0
                        THEN 'NO'
                    ELSE 'UNSURE'
                END,
                'UNSURE',
                NULL,
                'PROFESSIONAL_REVIEW_REQUIRED',
                'QUESTIONNAIRE_REVIEW_REQUIRED',
                'health-questionnaire-2.0.0-migrated',
                'safety-rules-2.0.0',
                'medical-disclaimer-2.0.0',
                acknowledged_at_epoch_ms,
                acknowledged_at_epoch_ms,
                acknowledged_at_epoch_ms,
                '1970-01-01',
                ?
            FROM safety_profile
            """.trimIndent(),
            arrayOf<Any>(migrationTimeEpochMs)
        )

        database.execSQL("DROP TABLE safety_profile")
        database.execSQL(
            "ALTER TABLE safety_profile_new RENAME TO safety_profile"
        )
    }

    private fun migrateGoals(
        database: SupportSQLiteDatabase,
        migrationTimeEpochMs: Long
    ) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS goal_new (
                id TEXT NOT NULL,
                start_weight_kg REAL NOT NULL,
                target_weight_kg REAL NOT NULL,
                start_date TEXT NOT NULL,
                indicative_target_date TEXT,
                calculation_weight_kg REAL NOT NULL,
                calculation_date TEXT NOT NULL,
                estimated_maintenance_calories INTEGER,
                daily_calorie_target INTEGER,
                initial_surplus_calories INTEGER,
                target_gain_kg_per_week REAL NOT NULL,
                pace TEXT NOT NULL,
                initial_bmi REAL NOT NULL,
                active INTEGER NOT NULL,
                calculation_version TEXT NOT NULL,
                created_at_epoch_ms INTEGER NOT NULL,
                updated_at_epoch_ms INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )

        database.execSQL(
            """
            INSERT INTO goal_new (
                id,
                start_weight_kg,
                target_weight_kg,
                start_date,
                indicative_target_date,
                calculation_weight_kg,
                calculation_date,
                estimated_maintenance_calories,
                daily_calorie_target,
                initial_surplus_calories,
                target_gain_kg_per_week,
                pace,
                initial_bmi,
                active,
                calculation_version,
                created_at_epoch_ms,
                updated_at_epoch_ms
            )
            SELECT
                id,
                start_weight_kg,
                target_weight_kg,
                start_date,
                NULL,
                start_weight_kg,
                start_date,
                NULL,
                NULL,
                NULL,
                target_gain_kg_per_week,
                pace,
                initial_bmi,
                active,
                'gain-plan-2.0.0-review-required',
                created_at_epoch_ms,
                ?
            FROM goal
            """.trimIndent(),
            arrayOf<Any>(migrationTimeEpochMs)
        )

        database.execSQL("DROP TABLE goal")
        database.execSQL("ALTER TABLE goal_new RENAME TO goal")
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
                index_goal_active_created_at_epoch_ms
            ON goal(active, created_at_epoch_ms)
            """.trimIndent()
        )
    }

    private fun migrateWeightIndex(
        database: SupportSQLiteDatabase
    ) {
        database.execSQL(
            "DROP INDEX IF EXISTS index_weight_entry_measured_at_epoch_ms"
        )
        database.execSQL(
            """
            CREATE INDEX IF NOT EXISTS
                index_weight_entry_measured_at_epoch_ms_created_at_epoch_ms
            ON weight_entry(
                measured_at_epoch_ms,
                created_at_epoch_ms
            )
            """.trimIndent()
        )
    }

    private fun createAiDataAccessPolicy(
        database: SupportSQLiteDatabase,
        migrationTimeEpochMs: Long
    ) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS ai_data_access_policy (
                id TEXT NOT NULL,
                enabled INTEGER NOT NULL,
                profile_scope_enabled INTEGER NOT NULL,
                goal_scope_enabled INTEGER NOT NULL,
                latest_weight_scope_enabled INTEGER NOT NULL,
                safety_summary_scope_enabled INTEGER NOT NULL,
                consent_version TEXT,
                granted_at_epoch_ms INTEGER,
                revoked_at_epoch_ms INTEGER,
                updated_at_epoch_ms INTEGER NOT NULL,
                PRIMARY KEY(id)
            )
            """.trimIndent()
        )

        /*
         * La migration n'accorde jamais un consentement par déduction. Elle
         * crée uniquement une politique locale explicitement désactivée.
         */
        database.execSQL(
            """
            INSERT OR IGNORE INTO ai_data_access_policy (
                id,
                enabled,
                profile_scope_enabled,
                goal_scope_enabled,
                latest_weight_scope_enabled,
                safety_summary_scope_enabled,
                consent_version,
                granted_at_epoch_ms,
                revoked_at_epoch_ms,
                updated_at_epoch_ms
            )
            SELECT
                'current-ai-policy',
                0,
                0,
                0,
                0,
                0,
                NULL,
                NULL,
                NULL,
                ?
            WHERE EXISTS (
                SELECT 1 FROM user_profile
            )
            """.trimIndent(),
            arrayOf<Any>(migrationTimeEpochMs)
        )
    }
}
