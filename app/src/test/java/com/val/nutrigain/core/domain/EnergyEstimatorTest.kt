// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.ActivityLevel
import com.`val`.nutrigain.core.model.MetabolicSex
import org.junit.Assert.assertEquals
import org.junit.Test

class EnergyEstimatorTest {

    private val estimator = EnergyEstimator()

    @Test
    fun `estime les calories puis arrondit la cible a dix`() {
        val estimate = estimator.estimate(
            weightKg = 55.0,
            heightCm = 165.0,
            ageYears = 30,
            metabolicSex = MetabolicSex.FEMALE,
            activityLevel = ActivityLevel.LIGHT
        )

        assertEquals(1_270, estimate.restingCalories)
        assertEquals(1_747, estimate.maintenanceCalories)
        assertEquals(
            2_050,
            estimator.buildDailyTarget(
                maintenanceCalories =
                    estimate.maintenanceCalories,
                surplusCalories = 300
            )
        )
    }
}
