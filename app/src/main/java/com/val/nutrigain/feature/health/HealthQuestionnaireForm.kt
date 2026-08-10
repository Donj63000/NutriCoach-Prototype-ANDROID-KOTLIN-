// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.health

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.`val`.nutrigain.R
import com.`val`.nutrigain.core.domain.HealthQuestionnairePolicy
import com.`val`.nutrigain.core.domain.PlanValidationCode
import com.`val`.nutrigain.core.model.HealthAnswer
import com.`val`.nutrigain.core.model.HealthQuestion

/**
 * Formulaire médical partagé entre l'onboarding et l'écran de révision.
 *
 * Le composant accepte une Map partielle pour représenter fidèlement les
 * questions encore sans réponse. Seule la couche domaine transforme ensuite
 * cette Map en HealthQuestionnaireAnswers complet.
 */
@Composable
fun HealthQuestionnaireForm(
    answers: Map<HealthQuestion, HealthAnswer>,
    medicalContextNote: String,
    onAnswer: (HealthQuestion, HealthAnswer) -> Unit,
    onMedicalContextNoteChange: (String) -> Unit,
    enabled: Boolean,
    questionnaireIssue: PlanValidationCode?,
    medicalNoteIssue: PlanValidationCode?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SafetyNotice()

        HealthQuestion.entries.forEach { question ->
            HealthQuestionCard(
                question = question,
                selectedAnswer = answers[question],
                onAnswer = { answer ->
                    onAnswer(question, answer)
                },
                enabled = enabled
            )
        }

        if (questionnaireIssue != null) {
            Text(
                text = stringResource(
                    R.string.error_questionnaire_incomplete
                ),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        OutlinedTextField(
            value = medicalContextNote,
            onValueChange = onMedicalContextNoteChange,
            enabled = enabled,
            label = {
                Text(
                    text = stringResource(
                        R.string.medical_context_note_label
                    )
                )
            },
            placeholder = {
                Text(
                    text = stringResource(
                        R.string.medical_context_note_placeholder
                    )
                )
            },
            supportingText = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(
                            R.string.medical_context_note_privacy
                        )
                    )
                    Text(
                        text = stringResource(
                            R.string.medical_context_note_counter,
                            medicalContextNote.length,
                            HealthQuestionnairePolicy
                                .MAX_MEDICAL_NOTE_LENGTH
                        )
                    )
                    if (medicalNoteIssue != null) {
                        Text(
                            text = stringResource(
                                R.string.error_medical_note_too_long
                            ),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            minLines = 3,
            maxLines = 6,
            isError = medicalNoteIssue != null,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SafetyNotice(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
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
                text = stringResource(R.string.safety_notice_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(R.string.safety_notice_body),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.safety_notice_emergency
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun HealthQuestionCard(
    question: HealthQuestion,
    selectedAnswer: HealthAnswer?,
    onAnswer: (HealthAnswer) -> Unit,
    enabled: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = healthQuestionLabel(question),
                style = MaterialTheme.typography.bodyLarge
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HealthAnswer.entries.forEach { answer ->
                    FilterChip(
                        selected = selectedAnswer == answer,
                        onClick = { onAnswer(answer) },
                        enabled = enabled,
                        label = {
                            Text(text = healthAnswerLabel(answer))
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun healthQuestionLabel(
    question: HealthQuestion
): String {
    return stringResource(
        when (question) {
            HealthQuestion.UNINTENTIONAL_WEIGHT_LOSS ->
                R.string.safety_unintentional_loss

            HealthQuestion.REDUCED_APPETITE ->
                R.string.safety_reduced_appetite

            HealthQuestion
                .SWALLOWING_DIFFICULTY_OR_PERSISTENT_VOMITING ->
                R.string.safety_swallowing_or_vomiting

            HealthQuestion.PREGNANT_OR_BREASTFEEDING ->
                R.string.safety_pregnancy

            HealthQuestion.EATING_DISORDER_HISTORY ->
                R.string.safety_eating_disorder

            HealthQuestion.SIGNIFICANT_DIGESTIVE_SYMPTOMS ->
                R.string.safety_digestive

            HealthQuestion.RELEVANT_MEDICAL_CONDITION ->
                R.string.safety_medical_condition

            HealthQuestion.RELEVANT_MEDICATION ->
                R.string.safety_medication

            HealthQuestion.FOOD_ALLERGIES_OR_INTOLERANCES ->
                R.string.safety_food_allergies
        }
    )
}

@Composable
private fun healthAnswerLabel(
    answer: HealthAnswer
): String {
    return stringResource(
        when (answer) {
            HealthAnswer.YES -> R.string.health_answer_yes
            HealthAnswer.NO -> R.string.health_answer_no
            HealthAnswer.UNSURE -> R.string.health_answer_unsure
        }
    )
}
