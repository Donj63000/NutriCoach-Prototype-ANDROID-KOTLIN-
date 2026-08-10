// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.`val`.nutrigain.R
import com.`val`.nutrigain.core.domain.PlanValidationCode

@Composable
fun planValidationMessage(
    code: PlanValidationCode
): String {
    return stringResource(
        when (code) {
            PlanValidationCode.REQUIRED ->
                R.string.error_required

            PlanValidationCode.INVALID_BIRTH_DATE ->
                R.string.error_birth_date_invalid

            PlanValidationCode.ADULT_ONLY ->
                R.string.error_adult_only

            PlanValidationCode.AGE_OUT_OF_RANGE ->
                R.string.error_age_out_of_range

            PlanValidationCode.INVALID_HEIGHT ->
                R.string.error_height_invalid

            PlanValidationCode.INVALID_WEIGHT ->
                R.string.error_weight_invalid

            PlanValidationCode.TARGET_NOT_HIGHER ->
                R.string.error_target_not_higher

            PlanValidationCode.SELECTION_REQUIRED ->
                R.string.error_selection_required

            PlanValidationCode.QUESTIONNAIRE_INCOMPLETE ->
                R.string.error_questionnaire_incomplete

            PlanValidationCode.MEDICAL_NOTE_TOO_LONG ->
                R.string.error_medical_note_too_long

            PlanValidationCode.ACKNOWLEDGEMENT_REQUIRED ->
                R.string.error_acknowledgement_required
        }
    )
}
