// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import javax.inject.Inject

enum class BmiCategory {
    MARKEDLY_LOW,
    LOW,
    REFERENCE_RANGE,
    HIGH,
    VERY_HIGH
}

data class BmiResult(
    val value: Double,
    val category: BmiCategory
)

class BmiCalculator @Inject constructor() {

    fun calculate(
        weightKg: Double,
        heightCm: Double
    ): BmiResult {
        require(weightKg.isFinite() && weightKg > 0.0) {
            "Le poids doit être un nombre strictement positif."
        }
        require(heightCm.isFinite() && heightCm > 0.0) {
            "La taille doit être un nombre strictement positif."
        }

        val heightMeters = heightCm / 100.0
        val value = weightKg / (heightMeters * heightMeters)

        return BmiResult(
            value = value,
            category = when {
                value < 17.0 -> BmiCategory.MARKEDLY_LOW
                value < 18.5 -> BmiCategory.LOW
                value < 25.0 -> BmiCategory.REFERENCE_RANGE
                value < 30.0 -> BmiCategory.HIGH
                else -> BmiCategory.VERY_HIGH
            }
        )
    }
}
