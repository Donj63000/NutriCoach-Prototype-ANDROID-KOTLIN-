// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.onboarding

import com.`val`.nutrigain.core.domain.PlanField
import com.`val`.nutrigain.core.domain.PlanValidationCode
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingInputParserTest {

    private val parser = OnboardingInputParser(
        Clock.fixed(
            Instant.parse("2026-08-09T10:00:00Z"),
            ZoneOffset.UTC
        )
    )

    @Test
    fun `formate la date avec un clavier uniquement numerique`() {
        assertEquals(
            "01/03/1996",
            OnboardingInputFormatter.formatBirthDate("01031996")
        )
        assertEquals(
            "01/0",
            OnboardingInputFormatter.formatBirthDate("010")
        )
    }

    @Test
    fun `refuse les caracteres non autorises dans la date`() {
        assertEquals(
            null,
            OnboardingInputFormatter.formatBirthDate("01-03-1996")
        )
    }

    @Test
    fun `accepte les decimales francaises`() {
        val result = parser.parseProfile(
            birthDateText = "01/03/1996",
            heightText = "165,5",
            currentWeightText = "54,2",
            targetWeightText = "60"
        )

        assertTrue(result is ProfileInputParseResult.Success)
        val input = (result as ProfileInputParseResult.Success)
            .input
        assertEquals(165.5, input.heightCm, 0.001)
        assertEquals(54.2, input.currentWeightKg, 0.001)
    }

    @Test
    fun `rejette une date impossible`() {
        val result = parser.parseProfile(
            birthDateText = "31/02/1996",
            heightText = "165",
            currentWeightText = "54",
            targetWeightText = "60"
        )

        assertTrue(result is ProfileInputParseResult.Invalid)
        val issues = (result as ProfileInputParseResult.Invalid)
            .issues
        assertEquals(
            PlanValidationCode.INVALID_BIRTH_DATE,
            issues[PlanField.BIRTH_DATE]
        )
    }

    @Test
    fun `rejette une cible identique au poids actuel`() {
        val result = parser.parseProfile(
            birthDateText = "01/03/1996",
            heightText = "165",
            currentWeightText = "54",
            targetWeightText = "54"
        )

        assertTrue(result is ProfileInputParseResult.Invalid)
        val issues = (result as ProfileInputParseResult.Invalid)
            .issues
        assertEquals(
            PlanValidationCode.TARGET_NOT_HIGHER,
            issues[PlanField.TARGET_WEIGHT]
        )
    }
}
