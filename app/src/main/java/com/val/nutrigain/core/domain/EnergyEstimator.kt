// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import com.`val`.nutrigain.core.model.ActivityLevel
import com.`val`.nutrigain.core.model.MetabolicSex
import javax.inject.Inject
import kotlin.math.roundToInt

data class EnergyEstimate(
    val restingCalories: Int,
    val maintenanceCalories: Int
)

class EnergyEstimator @Inject constructor() {

    fun estimate(
        weightKg: Double,
        heightCm: Double,
        ageYears: Int,
        metabolicSex: MetabolicSex,
        activityLevel: ActivityLevel
    ): EnergyEstimate {
        require(weightKg.isFinite() && weightKg > 0.0)
        require(heightCm.isFinite() && heightCm > 0.0)
        require(ageYears in 18..100)

        /*
         * Mifflin-St Jeor fournit un point de départ, pas une mesure de dépense.
         * La constante liée au sexe est isolée afin de rendre la limite de
         * l'équation explicite et révisable.
         */
        val sexConstant = when (metabolicSex) {
            MetabolicSex.FEMALE -> -161.0
            MetabolicSex.MALE -> 5.0
        }

        val restingCaloriesRaw =
            (10.0 * weightKg) +
                (6.25 * heightCm) -
                (5.0 * ageYears) +
                sexConstant

        /*
         * Le facteur d'activité est appliqué avant l'arrondi afin d'éviter
         * de propager une perte de précision intermédiaire.
         */
        val maintenanceCaloriesRaw =
            restingCaloriesRaw * activityLevel.maintenanceMultiplier

        return EnergyEstimate(
            restingCalories = restingCaloriesRaw
                .roundToInt()
                .coerceAtLeast(1),
            maintenanceCalories = maintenanceCaloriesRaw
                .roundToInt()
                .coerceAtLeast(1)
        )
    }

    fun buildDailyTarget(
        maintenanceCalories: Int,
        surplusCalories: Int
    ): Int {
        require(maintenanceCalories > 0)
        require(surplusCalories in 0..1_000)

        // Un arrondi à 10 kcal évite d'afficher une précision artificielle.
        return ((maintenanceCalories + surplusCalories) / 10.0)
            .roundToInt()
            .times(10)
    }
}
