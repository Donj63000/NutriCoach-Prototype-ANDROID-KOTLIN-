// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.`val`.nutrigain.core.database.entity.AiDataAccessPolicyEntity
import com.`val`.nutrigain.core.database.entity.GoalEntity
import com.`val`.nutrigain.core.database.entity.SafetyProfileEntity
import com.`val`.nutrigain.core.database.entity.UserProfileEntity
import com.`val`.nutrigain.core.database.entity.WeightEntryEntity
import java.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileDaoInstrumentedTest {

    private lateinit var database: NutriCoachDatabase
    private lateinit var dao: ProfileDao

    @Before
    fun createDatabase() {
        val context =
            ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            NutriCoachDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
        dao = database.profileDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun projectionVideRetourneDesComposantsNuls() = runBlocking {
        val stored = dao.observeCurrentSetup().first()

        assertNull(stored.profile)
        assertNull(stored.safetyProfile)
        assertNull(stored.goal)
        assertNull(stored.latestWeight)
        assertNull(stored.aiDataAccessPolicy)
    }

    @Test
    fun enregistrementInitialEstReluCommeUnEnsembleComplet() = runBlocking {
        val now = Instant.parse("2026-08-09T10:00:00Z")
            .toEpochMilli()

        dao.saveInitialSetup(
            profile = profile(now),
            safetyProfile = safety(now),
            goal = goal(now),
            initialWeight = weight(now),
            aiDataAccessPolicy = aiPolicy(now)
        )

        val stored = dao.observeCurrentSetup().first {
            it.profile != null &&
                it.safetyProfile != null &&
                it.goal != null &&
                it.latestWeight != null &&
                it.aiDataAccessPolicy != null
        }

        assertEquals("current-profile", stored.profile?.id)
        assertEquals(
            "current-safety-profile",
            stored.safetyProfile?.id
        )
        assertEquals("goal-id", stored.goal?.id)
        assertEquals("weight-id", stored.latestWeight?.id)
        assertEquals(
            "current-ai-policy",
            stored.aiDataAccessPolicy?.id
        )
        assertNotNull(stored.goal?.dailyCalorieTarget)
    }

    @Test
    fun revisionSuspendLeRepereDansLaMemeTransaction() = runBlocking {
        val now = Instant.parse("2026-08-09T10:00:00Z")
            .toEpochMilli()
        dao.saveInitialSetup(
            profile = profile(now),
            safetyProfile = safety(now),
            goal = goal(now),
            initialWeight = weight(now),
            aiDataAccessPolicy = aiPolicy(now)
        )

        dao.saveSafetyReview(
            safetyProfile = safety(now + 1).copy(
                reducedAppetite = "YES",
                safetyLevel = "PROFESSIONAL_REVIEW_REQUIRED",
                assessmentReasons = "REDUCED_APPETITE"
            ),
            goal = goal(now + 1).copy(
                estimatedMaintenanceCalories = null,
                dailyCalorieTarget = null,
                initialSurplusCalories = null
            )
        )

        val stored = dao.observeCurrentSetup().first {
            it.safetyProfile?.updatedAtEpochMs == now + 1 &&
                it.goal?.updatedAtEpochMs == now + 1
        }

        assertEquals(
            "PROFESSIONAL_REVIEW_REQUIRED",
            stored.safetyProfile?.safetyLevel
        )
        assertNull(stored.goal?.dailyCalorieTarget)
    }

    @Test
    fun suppressionTransactionnelleRetablitUnEtatVide() = runBlocking {
        val now = Instant.parse("2026-08-09T10:00:00Z")
            .toEpochMilli()
        dao.saveInitialSetup(
            profile = profile(now),
            safetyProfile = safety(now),
            goal = goal(now),
            initialWeight = weight(now),
            aiDataAccessPolicy = aiPolicy(now)
        )

        dao.deleteAllUserData()

        val stored = dao.observeCurrentSetup().first {
            it.profile == null &&
                it.safetyProfile == null &&
                it.goal == null &&
                it.latestWeight == null &&
                it.aiDataAccessPolicy == null
        }

        assertNull(stored.profile)
        assertNull(stored.aiDataAccessPolicy)
    }

    private fun profile(now: Long) = UserProfileEntity(
        id = "current-profile",
        birthDate = "1996-03-01",
        heightCm = 165.0,
        metabolicSex = "FEMALE",
        activityLevel = "LIGHT",
        createdAtEpochMs = now,
        updatedAtEpochMs = now
    )

    private fun safety(now: Long) = SafetyProfileEntity(
        id = "current-safety-profile",
        unintentionalWeightLoss = "NO",
        reducedAppetite = "NO",
        swallowingDifficultyOrPersistentVomiting = "NO",
        pregnantOrBreastfeeding = "NO",
        eatingDisorderHistory = "NO",
        significantDigestiveSymptoms = "NO",
        relevantMedicalCondition = "NO",
        relevantMedication = "NO",
        foodAllergiesOrIntolerances = "NO",
        medicalContextNote = null,
        safetyLevel = "NORMAL",
        assessmentReasons = "",
        questionnaireVersion = "health-questionnaire-2.0.0",
        assessmentVersion = "safety-rules-2.0.0",
        disclaimerVersion = "medical-disclaimer-2.0.0",
        createdAtEpochMs = now,
        acknowledgedAtEpochMs = now,
        answeredAtEpochMs = now,
        reviewDueDate = "2027-02-09",
        updatedAtEpochMs = now
    )

    private fun goal(now: Long) = GoalEntity(
        id = "goal-id",
        startWeightKg = 55.0,
        targetWeightKg = 60.0,
        startDate = "2026-08-09",
        indicativeTargetDate = "2026-12-27",
        calculationWeightKg = 55.0,
        calculationDate = "2026-08-09",
        estimatedMaintenanceCalories = 1_747,
        dailyCalorieTarget = 2_050,
        initialSurplusCalories = 300,
        targetGainKgPerWeek = 0.25,
        pace = "PROGRESSIVE",
        initialBmi = 20.2,
        active = true,
        calculationVersion = "gain-plan-2.0.0",
        createdAtEpochMs = now,
        updatedAtEpochMs = now
    )

    private fun weight(now: Long) = WeightEntryEntity(
        id = "weight-id",
        weightKg = 55.0,
        measuredAtEpochMs = now,
        note = null,
        createdAtEpochMs = now,
        updatedAtEpochMs = now
    )

    private fun aiPolicy(now: Long) =
        AiDataAccessPolicyEntity(
            id = "current-ai-policy",
            enabled = false,
            profileScopeEnabled = false,
            goalScopeEnabled = false,
            latestWeightScopeEnabled = false,
            safetySummaryScopeEnabled = false,
            consentVersion = null,
            grantedAtEpochMs = null,
            revokedAtEpochMs = null,
            updatedAtEpochMs = now
        )
}
