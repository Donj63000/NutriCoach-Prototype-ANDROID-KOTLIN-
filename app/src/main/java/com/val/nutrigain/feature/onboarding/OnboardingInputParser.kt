// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.onboarding

import com.`val`.nutrigain.core.domain.PlanConstraints
import com.`val`.nutrigain.core.domain.PlanField
import com.`val`.nutrigain.core.domain.PlanValidationCode
import java.time.Clock
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import javax.inject.Inject

data class ParsedProfileInput(
    val birthDate: LocalDate,
    val heightCm: Double,
    val currentWeightKg: Double,
    val targetWeightKg: Double
)

sealed interface ProfileInputParseResult {
    data class Success(
        val input: ParsedProfileInput
    ) : ProfileInputParseResult

    data class Invalid(
        val issues: Map<PlanField, PlanValidationCode>
    ) : ProfileInputParseResult
}

class OnboardingInputParser @Inject constructor(
    private val clock: Clock
) {

    fun parseProfile(
        birthDateText: String,
        heightText: String,
        currentWeightText: String,
        targetWeightText: String
    ): ProfileInputParseResult {
        val issues = linkedMapOf<PlanField, PlanValidationCode>()
        val today = LocalDate.now(clock)

        val birthDate = parseBirthDate(birthDateText)
        when {
            birthDateText.isBlank() ->
                issues[PlanField.BIRTH_DATE] =
                    PlanValidationCode.REQUIRED

            birthDate == null || birthDate.isAfter(today) ->
                issues[PlanField.BIRTH_DATE] =
                    PlanValidationCode.INVALID_BIRTH_DATE

            Period.between(birthDate, today).years <
                PlanConstraints.MIN_AGE_YEARS ->
                issues[PlanField.BIRTH_DATE] =
                    PlanValidationCode.ADULT_ONLY

            Period.between(birthDate, today).years >
                PlanConstraints.MAX_AGE_YEARS ->
                issues[PlanField.BIRTH_DATE] =
                    PlanValidationCode.AGE_OUT_OF_RANGE
        }

        val height = parseDecimal(heightText)
        when {
            heightText.isBlank() ->
                issues[PlanField.HEIGHT] =
                    PlanValidationCode.REQUIRED

            height == null ||
                height !in
                PlanConstraints.MIN_HEIGHT_CM..
                PlanConstraints.MAX_HEIGHT_CM ->
                issues[PlanField.HEIGHT] =
                    PlanValidationCode.INVALID_HEIGHT
        }

        val currentWeight = parseDecimal(currentWeightText)
        when {
            currentWeightText.isBlank() ->
                issues[PlanField.CURRENT_WEIGHT] =
                    PlanValidationCode.REQUIRED

            currentWeight == null ||
                currentWeight !in
                PlanConstraints.MIN_WEIGHT_KG..
                PlanConstraints.MAX_WEIGHT_KG ->
                issues[PlanField.CURRENT_WEIGHT] =
                    PlanValidationCode.INVALID_WEIGHT
        }

        val targetWeight = parseDecimal(targetWeightText)
        when {
            targetWeightText.isBlank() ->
                issues[PlanField.TARGET_WEIGHT] =
                    PlanValidationCode.REQUIRED

            targetWeight == null ||
                targetWeight !in
                PlanConstraints.MIN_WEIGHT_KG..
                PlanConstraints.MAX_WEIGHT_KG ->
                issues[PlanField.TARGET_WEIGHT] =
                    PlanValidationCode.INVALID_WEIGHT

            currentWeight != null &&
                currentWeight in
                PlanConstraints.MIN_WEIGHT_KG..
                PlanConstraints.MAX_WEIGHT_KG &&
                targetWeight <= currentWeight ->
                issues[PlanField.TARGET_WEIGHT] =
                    PlanValidationCode.TARGET_NOT_HIGHER
        }

        if (
            issues.isNotEmpty() ||
            birthDate == null ||
            height == null ||
            currentWeight == null ||
            targetWeight == null
        ) {
            return ProfileInputParseResult.Invalid(issues)
        }

        return ProfileInputParseResult.Success(
            ParsedProfileInput(
                birthDate = birthDate,
                heightCm = height,
                currentWeightKg = currentWeight,
                targetWeightKg = targetWeight
            )
        )
    }

    internal fun parseDecimal(rawValue: String): Double? {
        val normalized = rawValue
            .trim()
            .replace(',', '.')

        if (!DECIMAL_PATTERN.matches(normalized)) {
            return null
        }

        return normalized
            .toDoubleOrNull()
            ?.takeIf(Double::isFinite)
    }

    internal fun parseBirthDate(rawValue: String): LocalDate? {
        return runCatching {
            LocalDate.parse(
                rawValue.trim(),
                BIRTH_DATE_FORMATTER
            )
        }.getOrNull()
    }

    private companion object {
        val DECIMAL_PATTERN =
            Regex(pattern = """^[0-9]+(?:[.,][0-9]{1,2})?$""")

        val BIRTH_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter
                .ofPattern("dd/MM/uuuu")
                .withResolverStyle(ResolverStyle.STRICT)
    }
}
