// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
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
import com.`val`.nutrigain.core.model.HealthAnswer
import com.`val`.nutrigain.core.model.HealthQuestion
import com.`val`.nutrigain.core.model.MetabolicSex
import com.`val`.nutrigain.feature.common.planValidationMessage
import com.`val`.nutrigain.feature.health.HealthQuestionnaireForm

@Composable
fun OnboardingRoute(
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    /*
     * Le bouton Retour Android suit les étapes du formulaire au lieu de
     * fermer brutalement l'activité. Il reste neutralisé pendant une
     * écriture afin d'éviter une navigation concurrente.
     */
    BackHandler(
        enabled = state.isSaving ||
            state.step != OnboardingStep.PROFILE
    ) {
        if (!state.isSaving) {
            viewModel.goToPreviousStep()
        }
    }

    OnboardingScreen(
        state = state,
        onBirthDateChange = viewModel::updateBirthDate,
        onHeightChange = viewModel::updateHeight,
        onCurrentWeightChange = viewModel::updateCurrentWeight,
        onTargetWeightChange = viewModel::updateTargetWeight,
        onMetabolicSexSelected = viewModel::selectMetabolicSex,
        onActivitySelected = viewModel::selectActivityLevel,
        onPaceSelected = viewModel::selectPace,
        onHealthAnswer = viewModel::updateHealthAnswer,
        onMedicalContextNoteChange =
            viewModel::updateMedicalContextNote,
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
    onHealthAnswer: (HealthQuestion, HealthAnswer) -> Unit,
    onMedicalContextNoteChange: (String) -> Unit,
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

                OnboardingStep.HEALTH -> HealthStep(
                    state = state,
                    onHealthAnswer = onHealthAnswer,
                    onMedicalContextNoteChange =
                        onMedicalContextNoteChange
                )

                OnboardingStep.PRIVACY -> PrivacyStep(
                    state = state,
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
                            if (
                                state.step ==
                                OnboardingStep.PRIVACY
                            ) {
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
        contentPadding = PaddingValues(
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
                enabled = !state.isSaving,
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
                enabled = !state.isSaving,
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
                enabled = !state.isSaving,
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
                enabled = !state.isSaving,
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
                    enabled = !state.isSaving,
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
                    enabled = !state.isSaving,
                    onClick = {
                        onMetabolicSexSelected(
                            MetabolicSex.MALE
                        )
                    }
                )
            }

            state.validationIssues[PlanField.METABOLIC_SEX]
                ?.let { issue ->
                    SelectionError(issue)
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
        contentPadding = PaddingValues(
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
                        enabled = !state.isSaving,
                        onClick = { onActivitySelected(level) }
                    )
                }
            }
            state.validationIssues[PlanField.ACTIVITY_LEVEL]
                ?.let { issue ->
                    SelectionError(issue)
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
                        enabled = !state.isSaving,
                        onClick = { onPaceSelected(pace) }
                    )
                }
            }
            state.validationIssues[PlanField.GAIN_PACE]
                ?.let { issue ->
                    SelectionError(issue)
                }
        }
    }
}

@Composable
private fun HealthStep(
    state: OnboardingUiState,
    onHealthAnswer: (HealthQuestion, HealthAnswer) -> Unit,
    onMedicalContextNoteChange: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 24.dp,
            vertical = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
            HealthQuestionnaireForm(
                answers = state.healthAnswers,
                medicalContextNote = state.medicalContextNote,
                onAnswer = onHealthAnswer,
                onMedicalContextNoteChange =
                    onMedicalContextNoteChange,
                enabled = !state.isSaving,
                questionnaireIssue = state.validationIssues[
                    PlanField.HEALTH_QUESTIONNAIRE
                ],
                medicalNoteIssue = state.validationIssues[
                    PlanField.MEDICAL_CONTEXT_NOTE
                ]
            )
        }
    }
}

@Composable
private fun PrivacyStep(
    state: OnboardingUiState,
    onAcknowledgementChange: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 24.dp,
            vertical = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            StepHeader(
                title = stringResource(
                    R.string.privacy_step_title
                ),
                description = stringResource(
                    R.string.privacy_step_description
                )
            )
        }

        item {
            PrivacyCard(
                title = stringResource(
                    R.string.privacy_local_title
                ),
                body = stringResource(
                    R.string.privacy_local_body
                )
            )
        }

        item {
            PrivacyCard(
                title = stringResource(
                    R.string.privacy_ai_title
                ),
                body = stringResource(
                    R.string.privacy_ai_body
                )
            )
        }

        item {
            AcknowledgementRow(
                checked = state.safetyAcknowledged,
                onCheckedChange = onAcknowledgementChange,
                enabled = !state.isSaving,
                isError = state.validationIssues.containsKey(
                    PlanField.SAFETY_ACKNOWLEDGEMENT
                )
            )

            state.validationIssues[
                PlanField.SAFETY_ACKNOWLEDGEMENT
            ]?.let { issue ->
                Text(
                    text = planValidationMessage(issue),
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
    enabled: Boolean,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    placeholder: String? = null,
    suffix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
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
                Text(text = planValidationMessage(it))
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
    enabled: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .selectable(
                selected = selected,
                enabled = enabled,
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
            onClick = null,
            enabled = enabled
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
private fun AcknowledgementRow(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean,
    isError: Boolean
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .toggleable(
                value = checked,
                enabled = enabled,
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
            onCheckedChange = null,
            enabled = enabled
        )
        Text(
            text = stringResource(
                R.string.safety_acknowledgement
            ),
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
private fun PrivacyCard(
    title: String,
    body: String
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun SelectionError(issue: PlanValidationCode) {
    Text(
        text = planValidationMessage(issue),
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp)
    )
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
