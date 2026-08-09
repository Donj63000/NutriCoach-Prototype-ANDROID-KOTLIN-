// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.onboarding

internal object OnboardingInputFormatter {

    /**
     * Conserve uniquement un format de date numérique français et insère les
     * séparateurs. Le clavier numérique reste ainsi utilisable sans touche « / ».
     */
    fun formatBirthDate(rawValue: String): String? {
        if (rawValue.any { character ->
                !character.isDigit() && character != '/'
            }
        ) {
            return null
        }

        val digits = rawValue.filter(Char::isDigit)
        if (digits.length > MAX_BIRTH_DATE_DIGITS) {
            return null
        }

        return buildString(capacity = MAX_BIRTH_DATE_INPUT_LENGTH) {
            digits.forEachIndexed { index, digit ->
                if (index == DAY_DIGIT_COUNT ||
                    index == DAY_DIGIT_COUNT + MONTH_DIGIT_COUNT
                ) {
                    append('/')
                }
                append(digit)
            }
        }
    }

    private const val DAY_DIGIT_COUNT = 2
    private const val MONTH_DIGIT_COUNT = 2
    private const val MAX_BIRTH_DATE_DIGITS = 8
    private const val MAX_BIRTH_DATE_INPUT_LENGTH = 10
}
