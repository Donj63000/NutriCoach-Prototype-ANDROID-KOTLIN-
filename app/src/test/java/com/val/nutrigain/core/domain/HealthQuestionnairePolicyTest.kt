// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HealthQuestionnairePolicyTest {

    @Test
    fun `normalise la note sans conserver de controles invisibles`() {
        assertEquals(
            "Contexte utile et lisible",
            HealthQuestionnairePolicy
                .normalizeMedicalContextNote(
                    " \u0000Contexte\tutile\n et   lisible "
                )
        )
        assertNull(
            HealthQuestionnairePolicy
                .normalizeMedicalContextNote(" \n\t ")
        )
    }

    @Test
    fun `la date de revision est inclusive`() {
        val due = LocalDate.parse("2026-08-09")

        assertFalse(
            HealthQuestionnairePolicy.isReviewDue(
                reviewDueAt = due,
                today = LocalDate.parse("2026-08-08")
            )
        )
        assertTrue(
            HealthQuestionnairePolicy.isReviewDue(
                reviewDueAt = due,
                today = due
            )
        )
    }
}
