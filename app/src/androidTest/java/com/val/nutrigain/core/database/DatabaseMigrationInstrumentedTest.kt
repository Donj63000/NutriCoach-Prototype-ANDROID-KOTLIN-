// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationInstrumentedTest {

    @get:Rule
    val migrationHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        NutriCoachDatabase::class.java
    )

    @Test
    fun migrationUnVersDeuxConserveLeProfilEtExigeUneRevision() {
        migrationHelper
            .createDatabase(POPULATED_DATABASE_NAME, 1)
            .apply {
                insertVersionOneFixture()
                close()
            }

        val migrated = migrationHelper.runMigrationsAndValidate(
            POPULATED_DATABASE_NAME,
            2,
            true,
            MIGRATION_1_2
        )

        migrated.query(
            """
            SELECT
                unintentional_weight_loss,
                pregnant_or_breastfeeding,
                reduced_appetite,
                relevant_medication,
                food_allergies_or_intolerances,
                safety_level,
                assessment_reasons,
                review_due_date
            FROM safety_profile
            WHERE id = 'current-safety-profile'
            """.trimIndent()
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("YES", cursor.getString(0))
            assertEquals("NO", cursor.getString(1))
            assertEquals("UNSURE", cursor.getString(2))
            /*
             * Une valeur historique corrompue n'est jamais interprétée comme
             * un « non » : la migration la convertit en réponse incertaine.
             */
            assertEquals("UNSURE", cursor.getString(3))
            assertEquals("UNSURE", cursor.getString(4))
            assertEquals(
                "PROFESSIONAL_REVIEW_REQUIRED",
                cursor.getString(5)
            )
            assertEquals(
                "QUESTIONNAIRE_REVIEW_REQUIRED",
                cursor.getString(6)
            )
            assertEquals("1970-01-01", cursor.getString(7))
        }

        migrated.query(
            """
            SELECT
                calculation_weight_kg,
                calculation_date,
                estimated_maintenance_calories,
                daily_calorie_target,
                calculation_version
            FROM goal
            WHERE id = 'goal-id'
            """.trimIndent()
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(55.0, cursor.getDouble(0), 0.001)
            assertEquals("2026-08-09", cursor.getString(1))
            assertNull(cursor.getString(2))
            assertNull(cursor.getString(3))
            assertEquals(
                "gain-plan-2.0.0-review-required",
                cursor.getString(4)
            )
        }

        migrated.query(
            """
            SELECT
                enabled,
                profile_scope_enabled,
                safety_summary_scope_enabled,
                consent_version
            FROM ai_data_access_policy
            WHERE id = 'current-ai-policy'
            """.trimIndent()
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(0, cursor.getInt(2))
            assertNull(cursor.getString(3))
        }

        migrated.close()
    }

    @Test
    fun migrationUnVersDeuxSupporteUneBaseVide() {
        migrationHelper
            .createDatabase(EMPTY_DATABASE_NAME, 1)
            .close()

        val migrated = migrationHelper.runMigrationsAndValidate(
            EMPTY_DATABASE_NAME,
            2,
            true,
            MIGRATION_1_2
        )

        /*
         * La création des cinq tables est déjà validée structurellement par
         * Room. Cette assertion vérifie en plus qu'une base sans profil ne
         * reçoit pas une politique IA orpheline.
         */
        migrated.query(
            "SELECT COUNT(*) FROM ai_data_access_policy"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }

        migrated.close()
    }

    private fun SupportSQLiteDatabase.insertVersionOneFixture() {
        val now = 1_754_733_600_000L

        execSQL(
            """
            INSERT INTO user_profile (
                id,
                birth_date,
                height_cm,
                metabolic_sex,
                activity_level,
                created_at_epoch_ms,
                updated_at_epoch_ms
            ) VALUES (
                'current-profile',
                '1996-03-01',
                165.0,
                'FEMALE',
                'LIGHT',
                $now,
                $now
            )
            """.trimIndent()
        )
        execSQL(
            """
            INSERT INTO safety_profile (
                id,
                unintentional_weight_loss,
                pregnant_or_breastfeeding,
                eating_disorder_history,
                significant_digestive_symptoms,
                relevant_medical_condition,
                relevant_medication,
                safety_level,
                acknowledged_at_epoch_ms,
                updated_at_epoch_ms
            ) VALUES (
                'current-safety-profile',
                1,
                0,
                0,
                0,
                0,
                7,
                'PROFESSIONAL_REVIEW_REQUIRED',
                $now,
                $now
            )
            """.trimIndent()
        )
        execSQL(
            """
            INSERT INTO goal (
                id,
                start_weight_kg,
                target_weight_kg,
                start_date,
                indicative_target_date,
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
            ) VALUES (
                'goal-id',
                55.0,
                60.0,
                '2026-08-09',
                '2026-12-27',
                1747,
                2050,
                300,
                0.25,
                'PROGRESSIVE',
                20.2,
                1,
                'gain-plan-1.0.0',
                $now,
                $now
            )
            """.trimIndent()
        )
        execSQL(
            """
            INSERT INTO weight_entry (
                id,
                weight_kg,
                measured_at_epoch_ms,
                note,
                created_at_epoch_ms,
                updated_at_epoch_ms
            ) VALUES (
                'weight-id',
                55.0,
                $now,
                NULL,
                $now,
                $now
            )
            """.trimIndent()
        )
    }

    private companion object {
        const val POPULATED_DATABASE_NAME =
            "migration-1-2-populated-test"
        const val EMPTY_DATABASE_NAME =
            "migration-1-2-empty-test"
    }
}
