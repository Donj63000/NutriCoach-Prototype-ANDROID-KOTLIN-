// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class BmiCalculatorTest {

    private val calculator = BmiCalculator()

    @Test
    fun `calcule l imc sans arrondi intermediaire`() {
        val result = calculator.calculate(
            weightKg = 55.0,
            heightCm = 165.0
        )

        assertEquals(20.202, result.value, 0.001)
        assertEquals(
            BmiCategory.REFERENCE_RANGE,
            result.category
        )
    }

    @Test
    fun `classe un imc inferieur a 17 dans la zone fortement basse`() {
        val result = calculator.calculate(
            weightKg = 45.0,
            heightCm = 165.0
        )

        assertEquals(
            BmiCategory.MARKEDLY_LOW,
            result.category
        )
    }
}
