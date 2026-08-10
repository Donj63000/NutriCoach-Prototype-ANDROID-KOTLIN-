// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import java.time.LocalDate

object HealthQuestionnairePolicy {
    const val QUESTIONNAIRE_VERSION = "health-questionnaire-2.0.0"
    const val ASSESSMENT_VERSION = "safety-rules-2.0.0"
    const val DISCLAIMER_VERSION = "medical-disclaimer-2.0.0"
    const val MAX_MEDICAL_NOTE_LENGTH = 500
    const val REVIEW_INTERVAL_MONTHS = 6L

    fun nextReviewDate(answeredOn: LocalDate): LocalDate {
        return answeredOn.plusMonths(REVIEW_INTERVAL_MONTHS)
    }

    /**
     * La date d'échéance est inclusive : le questionnaire doit être revu le
     * jour indiqué, et pas seulement à partir du lendemain.
     */
    fun isReviewDue(
        reviewDueAt: LocalDate,
        today: LocalDate
    ): Boolean {
        return !reviewDueAt.isAfter(today)
    }

    /**
     * Normalise une note avant persistance afin d'éviter les caractères de
     * contrôle invisibles et les espaces artificiels, sans modifier son sens.
     */
    fun normalizeMedicalContextNote(rawValue: String?): String? {
        if (rawValue == null) {
            return null
        }

        val normalized = buildString(rawValue.length) {
            rawValue.forEach { character ->
                when {
                    character == '\n' || character == '\t' ->
                        append(' ')

                    !character.isISOControl() ->
                        append(character)
                }
            }
        }
            .trim()
            .replace(MULTIPLE_WHITESPACE, " ")

        return normalized.ifBlank { null }
    }

    fun isMedicalContextNoteValid(rawValue: String?): Boolean {
        return normalizeMedicalContextNote(rawValue)
            ?.length
            ?.let { it <= MAX_MEDICAL_NOTE_LENGTH }
            ?: true
    }

    private val MULTIPLE_WHITESPACE = Regex("""\s{2,}""")
}
