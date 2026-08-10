// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.health

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.`val`.nutrigain.R
import com.`val`.nutrigain.core.domain.PlanField
import com.`val`.nutrigain.core.model.HealthAnswer
import com.`val`.nutrigain.core.model.HealthQuestion
import com.`val`.nutrigain.core.model.UserSetup
import com.`val`.nutrigain.feature.common.planValidationMessage
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun HealthReviewRoute(
    setup: UserSetup,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: HealthReviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    /*
     * Comme pour le bouton visuel, le retour système est bloqué pendant
     * la transaction afin de ne pas masquer l'issue d'un enregistrement.
     */
    BackHandler {
        if (!state.isSaving) {
            onBack()
        }
    }

    /*
     * L'effet est rejoué à chaque nouvelle entrée dans cet écran, même si le
     * ViewModel reste attaché à l'activité. L'accusé de lecture repart donc
     * toujours décoché.
     */
    LaunchedEffect(Unit) {
        viewModel.begin(setup)
    }

    LaunchedEffect(state.saveCompleted) {
        if (state.saveCompleted) {
            viewModel.consumeSaveCompleted()
            onSaved()
        }
    }

    HealthReviewScreen(
        state = state,
        setup = setup,
        onBack = onBack,
        onAnswer = viewModel::updateAnswer,
        onMedicalContextNoteChange =
            viewModel::updateMedicalContextNote,
        onAcknowledgementChange =
            viewModel::setSafetyAcknowledged,
        onSave = viewModel::save
    )
}

@Composable
private fun HealthReviewScreen(
    state: HealthReviewUiState,
    setup: UserSetup,
    onBack: () -> Unit,
    onAnswer: (HealthQuestion, HealthAnswer) -> Unit,
    onMedicalContextNoteChange: (String) -> Unit,
    onAcknowledgementChange: (Boolean) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onBack,
                enabled = !state.isSaving
            ) {
                Text(text = stringResource(R.string.review_back))
            }
            Text(
                text = stringResource(
                    R.string.health_review_title
                ),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        HorizontalDivider()

        if (!state.isReady) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }

        val dateFormatter = remember {
            DateTimeFormatter
                .ofLocalizedDate(FormatStyle.LONG)
                .withLocale(Locale.FRANCE)
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(
                horizontal = 20.dp,
                vertical = 20.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = stringResource(
                        R.string.health_review_description
                    ),
                    style = MaterialTheme.typography.bodyLarge,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement =
                            Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.health_review_previous_title
                            ),
                            style =
                                MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.health_review_previous_date,
                                dateFormatter.format(
                                    setup.safetyProfile.answeredAt
                                        .atZone(
                                            java.time.ZoneId
                                                .systemDefault()
                                        )
                                        .toLocalDate()
                                )
                            ),
                            style =
                                MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = stringResource(
                                R.string.health_review_due_date,
                                dateFormatter.format(
                                    setup.safetyProfile.reviewDueAt
                                )
                            ),
                            style =
                                MaterialTheme.typography.bodyMedium,
                            color =
                                MaterialTheme.colorScheme
                                    .onSurfaceVariant
                        )
                    }
                }
            }

            item {
                HealthQuestionnaireForm(
                    answers = state.answers,
                    medicalContextNote =
                        state.medicalContextNote,
                    onAnswer = onAnswer,
                    onMedicalContextNoteChange =
                        onMedicalContextNoteChange,
                    enabled = !state.isSaving,
                    questionnaireIssue =
                        state.validationIssues[
                            PlanField.HEALTH_QUESTIONNAIRE
                        ],
                    medicalNoteIssue =
                        state.validationIssues[
                            PlanField.MEDICAL_CONTEXT_NOTE
                        ]
                )
            }

            item {
                ReviewAcknowledgement(
                    checked = state.safetyAcknowledged,
                    onCheckedChange =
                        onAcknowledgementChange,
                    enabled = !state.isSaving,
                    isError =
                        state.validationIssues.containsKey(
                            PlanField.SAFETY_ACKNOWLEDGEMENT
                        )
                )

                state.validationIssues[
                    PlanField.SAFETY_ACKNOWLEDGEMENT
                ]?.let { issue ->
                    Text(
                        text = planValidationMessage(issue),
                        color = MaterialTheme.colorScheme.error,
                        style =
                            MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(
                            start = 12.dp,
                            top = 4.dp
                        )
                    )
                }
            }

            if (state.hasUnexpectedError) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor =
                                MaterialTheme.colorScheme
                                    .errorContainer
                        )
                    ) {
                        Text(
                            text = stringResource(
                                R.string.unexpected_error
                            ),
                            color =
                                MaterialTheme.colorScheme
                                    .onErrorContainer,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider()

        Button(
            onClick = onSave,
            enabled = !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(
                        R.string.health_review_saving
                    )
                )
            } else {
                Text(
                    text = stringResource(
                        R.string.health_review_save
                    )
                )
            }
        }
    }
}

@Composable
private fun ReviewAcknowledgement(
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
            .padding(12.dp),
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
            color = if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
