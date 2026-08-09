// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.`val`.nutrigain.R
import com.`val`.nutrigain.core.domain.PlanField
import com.`val`.nutrigain.core.domain.PlanValidationCode
import com.`val`.nutrigain.core.model.ActivityLevel
import com.`val`.nutrigain.core.model.GainPace
import com.`val`.nutrigain.core.model.MetabolicSex

@Composable
fun OnboardingRoute(
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    OnboardingScreen(
        state = state,
        onBirthDateChange = viewModel::updateBirthDate,
        onHeightChange = viewModel::updateHeight,
        onCurrentWeightChange = viewModel::updateCurrentWeight,
        onTargetWeightChange = viewModel::updateTargetWeight,
        onMetabolicSexSelected = viewModel::selectMetabolicSex,
        onActivitySelected = viewModel::selectActivityLevel,
        onPaceSelected = viewModel::selectPace,
        onUnintentionalWeightLossChange =
            viewModel::setUnintentionalWeightLoss,
        onPregnantOrBreastfeedingChange =
            viewModel::setPregnantOrBreastfeeding,
        onEatingDisorderHistoryChange =
            viewModel::setEatingDisorderHistory,
        onDigestiveSymptomsChange =
            viewModel::setSignificantDigestiveSymptoms,
        onMedicalConditionChange =
            viewModel::setRelevantMedicalCondition,
        onMedicationChange = viewModel::setRelevantMedication,
        onAcknowledgementChange =
            viewModel::setSafetyAcknowledged,
        onPrevious = viewModel::goToPreviousStep,
        onNext = viewModel::goToNextStep
    )
}

@Composable
private fun OnboardingScreen(
    state: OnboardingUiState,
    onBirthDateChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onCurrentWeightChange: (String) -> Unit,
    onTargetWeightChange: (String) -> Unit,
    onMetabolicSexSelected: (MetabolicSex) -> Unit,
    onActivitySelected: (ActivityLevel) -> Unit,
    onPaceSelected: (GainPace) -> Unit,
    onUnintentionalWeightLossChange: (Boolean) -> Unit,
    onPregnantOrBreastfeedingChange: (Boolean) -> Unit,
    onEatingDisorderHistoryChange: (Boolean) -> Unit,
    onDigestiveSymptomsChange: (Boolean) -> Unit,
    onMedicalConditionChange: (Boolean) -> Unit,
    onMedicationChange: (Boolean) -> Unit,
    onAcknowledgementChange: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val stepNumber = state.step.ordinal + 1
    val stepCount = OnboardingStep.entries.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 24.dp,
                vertical = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_title),
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = stringResource(
                    R.string.onboarding_progress,
                    stepNumber,
                    stepCount
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LinearProgressIndicator(
                progress = {
                    stepNumber.toFloat() / stepCount.toFloat()
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        HorizontalDivider()

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (state.step) {
                OnboardingStep.PROFILE -> ProfileStep(
                    state = state,
                    onBirthDateChange = onBirthDateChange,
                    onHeightChange = onHeightChange,
                    onCurrentWeightChange =
                        onCurrentWeightChange,
                    onTargetWeightChange =
                        onTargetWeightChange,
                    onMetabolicSexSelected =
                        onMetabolicSexSelected
                )

                OnboardingStep.LIFESTYLE -> LifestyleStep(
                    state = state,
                    onActivitySelected = onActivitySelected,
                    onPaceSelected = onPaceSelected
                )

                OnboardingStep.SAFETY -> SafetyStep(
                    state = state,
                    onUnintentionalWeightLossChange =
                        onUnintentionalWeightLossChange,
                    onPregnantOrBreastfeedingChange =
                        onPregnantOrBreastfeedingChange,
                    onEatingDisorderHistoryChange =
                        onEatingDisorderHistoryChange,
                    onDigestiveSymptomsChange =
                        onDigestiveSymptomsChange,
                    onMedicalConditionChange =
                        onMedicalConditionChange,
                    onMedicationChange = onMedicationChange,
                    onAcknowledgementChange =
                        onAcknowledgementChange
                )
            }
        }

        if (state.hasUnexpectedError) {
            ErrorBanner(
                modifier = Modifier.padding(
                    horizontal = 24.dp,
                    vertical = 8.dp
                )
            )
        }

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.step != OnboardingStep.PROFILE) {
                TextButton(
                    onClick = onPrevious,
                    enabled = !state.isSaving
                ) {
                    Text(
                        text = stringResource(
                            R.string.onboarding_previous
                        )
                    )
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            Button(
                onClick = onNext,
                enabled = !state.isSaving,
                modifier = Modifier.weight(1f)
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = stringResource(
                            R.string.onboarding_saving
                        )
                    )
                } else {
                    Text(
                        text = stringResource(
                            if (state.step == OnboardingStep.SAFETY) {
                                R.string.onboarding_finish
                            } else {
                                R.string.onboarding_next
                            }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileStep(
    state: OnboardingUiState,
    onBirthDateChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onCurrentWeightChange: (String) -> Unit,
    onTargetWeightChange: (String) -> Unit,
    onMetabolicSexSelected: (MetabolicSex) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 24.dp,
            vertical = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StepHeader(
                title = stringResource(R.string.profile_step_title),
                description = stringResource(
                    R.string.profile_step_description
                )
            )
        }

        item {
            ValidatedTextField(
                value = state.birthDate,
                onValueChange = onBirthDateChange,
                label = stringResource(R.string.birth_date_label),
                placeholder = stringResource(
                    R.string.birth_date_placeholder
                ),
                issue = state.validationIssues[
                    PlanField.BIRTH_DATE
                ],
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        }

        item {
            ValidatedTextField(
                value = state.heightCm,
                onValueChange = onHeightChange,
                label = stringResource(R.string.height_label),
                suffix = stringResource(R.string.height_suffix),
                issue = state.validationIssues[PlanField.HEIGHT],
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            )
        }

        item {
            ValidatedTextField(
                value = state.currentWeightKg,
                onValueChange = onCurrentWeightChange,
                label = stringResource(
                    R.string.current_weight_label
                ),
                suffix = stringResource(R.string.weight_suffix),
                issue = state.validationIssues[
                    PlanField.CURRENT_WEIGHT
                ],
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Next
            )
        }

        item {
            ValidatedTextField(
                value = state.targetWeightKg,
                onValueChange = onTargetWeightChange,
                label = stringResource(
                    R.string.target_weight_label
                ),
                suffix = stringResource(R.string.weight_suffix),
                issue = state.validationIssues[
                    PlanField.TARGET_WEIGHT
                ],
                keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done
            )
        }

        item {
            Text(
                text = stringResource(
                    R.string.metabolic_parameter_title
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.metabolic_parameter_description
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ChoiceRow(
                    title = stringResource(
                        R.string.metabolic_parameter_female
                    ),
                    description = null,
                    selected =
                        state.metabolicSex == MetabolicSex.FEMALE,
                    onClick = {
                        onMetabolicSexSelected(
                            MetabolicSex.FEMALE
                        )
                    }
                )
                ChoiceRow(
                    title = stringResource(
                        R.string.metabolic_parameter_male
                    ),
                    description = null,
                    selected =
                        state.metabolicSex == MetabolicSex.MALE,
                    onClick = {
                        onMetabolicSexSelected(
                            MetabolicSex.MALE
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun LifestyleStep(
    state: OnboardingUiState,
    onActivitySelected: (ActivityLevel) -> Unit,
    onPaceSelected: (GainPace) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 24.dp,
            vertical = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            StepHeader(
                title = stringResource(
                    R.string.lifestyle_step_title
                ),
                description = stringResource(
                    R.string.lifestyle_step_description
                )
            )
        }

        item {
            Text(
                text = stringResource(
                    R.string.activity_level_title
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ActivityLevel.entries.forEach { level ->
                    ChoiceRow(
                        title = activityTitle(level),
                        description = activityDescription(level),
                        selected = state.activityLevel == level,
                        onClick = { onActivitySelected(level) }
                    )
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.gain_pace_title),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GainPace.entries.forEach { pace ->
                    ChoiceRow(
                        title = paceTitle(pace),
                        description = paceDescription(pace),
                        selected = state.pace == pace,
                        onClick = { onPaceSelected(pace) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SafetyStep(
    state: OnboardingUiState,
    onUnintentionalWeightLossChange: (Boolean) -> Unit,
    onPregnantOrBreastfeedingChange: (Boolean) -> Unit,
    onEatingDisorderHistoryChange: (Boolean) -> Unit,
    onDigestiveSymptomsChange: (Boolean) -> Unit,
    onMedicalConditionChange: (Boolean) -> Unit,
    onMedicationChange: (Boolean) -> Unit,
    onAcknowledgementChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 24.dp,
            vertical = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            StepHeader(
                title = stringResource(R.string.safety_step_title),
                description = stringResource(
                    R.string.safety_step_description
                )
            )
        }

        item {
            SafetyNotice()
        }

        item {
            SafetyCheckbox(
                checked = state.unintentionalWeightLoss,
                onCheckedChange =
                    onUnintentionalWeightLossChange,
                label = stringResource(
                    R.string.safety_unintentional_loss
                )
            )
        }

        item {
            SafetyCheckbox(
                checked = state.pregnantOrBreastfeeding,
                onCheckedChange =
                    onPregnantOrBreastfeedingChange,
                label = stringResource(
                    R.string.safety_pregnancy
                )
            )
        }

        item {
            SafetyCheckbox(
                checked = state.eatingDisorderHistory,
                onCheckedChange =
                    onEatingDisorderHistoryChange,
                label = stringResource(
                    R.string.safety_eating_disorder
                )
            )
        }

        item {
            SafetyCheckbox(
                checked = state.significantDigestiveSymptoms,
                onCheckedChange = onDigestiveSymptomsChange,
                label = stringResource(
                    R.string.safety_digestive
                )
            )
        }

        item {
            SafetyCheckbox(
                checked = state.relevantMedicalCondition,
                onCheckedChange = onMedicalConditionChange,
                label = stringResource(
                    R.string.safety_medical_condition
                )
            )
        }

        item {
            SafetyCheckbox(
                checked = state.relevantMedication,
                onCheckedChange = onMedicationChange,
                label = stringResource(
                    R.string.safety_medication
                )
            )
        }

        item {
            Spacer(modifier = Modifier.height(4.dp))
            SafetyCheckbox(
                checked = state.safetyAcknowledged,
                onCheckedChange = onAcknowledgementChange,
                label = stringResource(
                    R.string.safety_acknowledgement
                ),
                isError = state.validationIssues.containsKey(
                    PlanField.SAFETY_ACKNOWLEDGEMENT
                )
            )

            state.validationIssues[
                PlanField.SAFETY_ACKNOWLEDGEMENT
            ]?.let { issue ->
                Text(
                    text = validationMessage(issue),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(
                        start = 16.dp,
                        top = 4.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun StepHeader(
    title: String,
    description: String
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    issue: PlanValidationCode?,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    placeholder: String? = null,
    suffix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        placeholder = placeholder?.let {
            { Text(text = it) }
        },
        suffix = suffix?.let {
            { Text(text = it) }
        },
        singleLine = true,
        isError = issue != null,
        supportingText = issue?.let {
            {
                Text(text = validationMessage(it))
            }
        },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction
        ),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ChoiceRow(
    title: String,
    description: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = null
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            if (description != null) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SafetyCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String,
    isError: Boolean = false
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .toggleable(
                value = checked,
                onValueChange = onCheckedChange,
                role = Role.Checkbox
            )
            .padding(
                horizontal = 12.dp,
                vertical = 10.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = null
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SafetyNotice() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.safety_notice_title
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(
                    R.string.safety_notice_body
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ErrorBanner(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor =
                MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Text(
            text = stringResource(R.string.unexpected_error),
            color = MaterialTheme.colorScheme.onErrorContainer,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Composable
private fun activityTitle(
    activityLevel: ActivityLevel
): String {
    return stringResource(
        when (activityLevel) {
            ActivityLevel.SEDENTARY ->
                R.string.activity_sedentary

            ActivityLevel.LIGHT ->
                R.string.activity_light

            ActivityLevel.MODERATE ->
                R.string.activity_moderate

            ActivityLevel.HIGH ->
                R.string.activity_high
        }
    )
}

@Composable
private fun activityDescription(
    activityLevel: ActivityLevel
): String {
    return stringResource(
        when (activityLevel) {
            ActivityLevel.SEDENTARY ->
                R.string.activity_sedentary_description

            ActivityLevel.LIGHT ->
                R.string.activity_light_description

            ActivityLevel.MODERATE ->
                R.string.activity_moderate_description

            ActivityLevel.HIGH ->
                R.string.activity_high_description
        }
    )
}

@Composable
private fun paceTitle(
    pace: GainPace
): String {
    return stringResource(
        when (pace) {
            GainPace.GENTLE -> R.string.gain_pace_gentle
            GainPace.PROGRESSIVE ->
                R.string.gain_pace_progressive

            GainPace.SUSTAINED ->
                R.string.gain_pace_sustained
        }
    )
}

@Composable
private fun paceDescription(
    pace: GainPace
): String {
    return stringResource(
        when (pace) {
            GainPace.GENTLE ->
                R.string.gain_pace_gentle_description

            GainPace.PROGRESSIVE ->
                R.string.gain_pace_progressive_description

            GainPace.SUSTAINED ->
                R.string.gain_pace_sustained_description
        }
    )
}

@Composable
private fun validationMessage(
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

            PlanValidationCode.ACKNOWLEDGEMENT_REQUIRED ->
                R.string.error_acknowledgement_required
        }
    )
}
