// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.`val`.nutrigain.R
import com.`val`.nutrigain.core.domain.HealthQuestionnairePolicy
import com.`val`.nutrigain.core.model.AiDataScope
import com.`val`.nutrigain.core.model.SafetyLevel
import com.`val`.nutrigain.core.model.UserSetup
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun DashboardRoute(
    setup: UserSetup,
    onReviewHealth: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val actionState by
        viewModel.actionState.collectAsStateWithLifecycle()
    var showDeleteConfirmation by rememberSaveable {
        mutableStateOf(false)
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = {
                if (!actionState.isDeleting) {
                    showDeleteConfirmation = false
                }
            },
            title = {
                Text(
                    text = stringResource(
                        R.string.delete_data_dialog_title
                    )
                )
            },
            text = {
                Text(
                    text = stringResource(
                        R.string.delete_data_dialog_body
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAllUserData()
                    },
                    enabled = !actionState.isDeleting
                ) {
                    if (actionState.isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(
                                R.string.delete_data_confirm
                            )
                        )
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                    },
                    enabled = !actionState.isDeleting
                ) {
                    Text(
                        text = stringResource(
                            R.string.delete_data_cancel
                        )
                    )
                }
            }
        )
    }

    DashboardScreen(
        setup = setup,
        actionState = actionState,
        onReviewHealth = onReviewHealth,
        onRequestDelete = {
            viewModel.clearDeleteError()
            showDeleteConfirmation = true
        }
    )
}

@Composable
private fun DashboardScreen(
    setup: UserSetup,
    actionState: DashboardActionUiState,
    onReviewHealth: () -> Unit,
    onRequestDelete: () -> Unit
) {
    val decimalFormatter = remember {
        DecimalFormat(
            "0.#",
            DecimalFormatSymbols(Locale.FRANCE)
        )
    }
    val dateFormatter = remember {
        DateTimeFormatter
            .ofLocalizedDate(FormatStyle.MEDIUM)
            .withLocale(Locale.FRANCE)
    }
    val today = LocalDate.now()
    val reviewDue =
        HealthQuestionnairePolicy.isReviewDue(
            reviewDueAt = setup.safetyProfile.reviewDueAt,
            today = today
        )
    val heightMetres = setup.profile.heightCm / 100.0
    val currentBmi = setup.latestWeight.weightKg /
        (heightMetres * heightMetres)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentPadding = PaddingValues(
            horizontal = 20.dp,
            vertical = 24.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.dashboard_title),
                style = MaterialTheme.typography.headlineMedium
            )
        }

        item {
            SafetyStatusCard(
                level = setup.safetyProfile.level,
                reviewDue = reviewDue,
                reviewDueAt =
                    dateFormatter.format(
                        setup.safetyProfile.reviewDueAt
                    ),
                onReview = onReviewHealth
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                MetricCard(
                    label = stringResource(
                        R.string.dashboard_current_weight
                    ),
                    value = stringResource(
                        R.string.dashboard_weight_value,
                        decimalFormatter.format(
                            setup.latestWeight.weightKg
                        )
                    ),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = stringResource(
                        R.string.dashboard_target_weight
                    ),
                    value = stringResource(
                        R.string.dashboard_weight_value,
                        decimalFormatter.format(
                            setup.goal.targetWeightKg
                        )
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            MetricCard(
                label = stringResource(R.string.dashboard_bmi),
                value = stringResource(
                    R.string.dashboard_bmi_value,
                    decimalFormatter.format(currentBmi)
                ),
                supportingText = stringResource(
                    R.string.dashboard_bmi_limit
                )
            )
        }

        item {
            CaloriePlanCard(
                setup = setup,
                decimalFormatter = decimalFormatter,
                dateFormatter = dateFormatter
            )
        }

        item {
            GoalTimelineCard(
                setup = setup,
                dateFormatter = dateFormatter
            )
        }

        item {
            AiAccessCard(setup = setup)
        }

        item {
            DataControlsCard(
                isDeleting = actionState.isDeleting,
                deleteFailed = actionState.deleteFailed,
                onRequestDelete = onRequestDelete
            )
        }

        item {
            Text(
                text = stringResource(
                    R.string.dashboard_plan_estimate_notice
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SafetyStatusCard(
    level: SafetyLevel,
    reviewDue: Boolean,
    reviewDueAt: String,
    onReview: () -> Unit
) {
    val title: String
    val body: String
    val containerColor: Color
    val contentColor: Color

    when {
        reviewDue -> {
            title = stringResource(
                R.string.dashboard_review_due_title
            )
            body = stringResource(
                R.string.dashboard_review_due_body
            )
            containerColor =
                MaterialTheme.colorScheme.errorContainer
            contentColor =
                MaterialTheme.colorScheme.onErrorContainer
        }

        level == SafetyLevel.NORMAL -> {
            title = stringResource(
                R.string.dashboard_normal_title
            )
            body = stringResource(
                R.string.dashboard_normal_body
            )
            containerColor =
                MaterialTheme.colorScheme.primaryContainer
            contentColor =
                MaterialTheme.colorScheme.onPrimaryContainer
        }

        level == SafetyLevel.CAUTION -> {
            title = stringResource(
                R.string.dashboard_caution_title
            )
            body = stringResource(
                R.string.dashboard_caution_body
            )
            containerColor =
                MaterialTheme.colorScheme.secondaryContainer
            contentColor =
                MaterialTheme.colorScheme.onSecondaryContainer
        }

        else -> {
            title = stringResource(
                R.string.dashboard_no_automatic_target_title
            )
            body = stringResource(
                R.string.dashboard_no_automatic_target_body
            )
            containerColor =
                MaterialTheme.colorScheme.errorContainer
            contentColor =
                MaterialTheme.colorScheme.onErrorContainer
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = stringResource(
                    R.string.dashboard_next_review,
                    reviewDueAt
                ),
                style = MaterialTheme.typography.bodySmall
            )
            OutlinedButton(
                onClick = onReview,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(
                        R.string.dashboard_review_questionnaire
                    )
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CaloriePlanCard(
    setup: UserSetup,
    decimalFormatter: DecimalFormat,
    dateFormatter: DateTimeFormatter
) {
    val goal = setup.goal

    if (
        goal.dailyCalorieTarget == null ||
        goal.estimatedMaintenanceCalories == null ||
        goal.initialSurplusCalories == null
    ) {
        MetricCard(
            label = stringResource(
                R.string.dashboard_daily_target
            ),
            value = stringResource(R.string.not_available),
            supportingText = stringResource(
                R.string.dashboard_no_automatic_target_body
            )
        )
        return
    }

    Card {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.dashboard_daily_target
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = stringResource(
                    R.string.dashboard_calories_value,
                    goal.dailyCalorieTarget
                ),
                style = MaterialTheme.typography.headlineMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CompactMetric(
                    label = stringResource(
                        R.string.dashboard_maintenance
                    ),
                    value = stringResource(
                        R.string.dashboard_calories_value,
                        goal.estimatedMaintenanceCalories
                    ),
                    modifier = Modifier.weight(1f)
                )
                CompactMetric(
                    label = stringResource(
                        R.string.dashboard_surplus
                    ),
                    value = stringResource(
                        R.string.dashboard_calories_value,
                        goal.initialSurplusCalories
                    ),
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = stringResource(
                    R.string.dashboard_calculation_provenance,
                    dateFormatter.format(goal.calculationDate),
                    decimalFormatter.format(
                        goal.calculationWeightKg
                    )
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun GoalTimelineCard(
    setup: UserSetup,
    dateFormatter: DateTimeFormatter
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.dashboard_timeline_title
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = setup.goal.indicativeTargetDate?.let {
                    stringResource(
                        R.string.dashboard_target_date,
                        dateFormatter.format(it)
                    )
                } ?: stringResource(
                    R.string.dashboard_target_date_unavailable
                ),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = stringResource(
                    R.string.dashboard_last_weight_date,
                    dateFormatter.format(
                        setup.latestWeight.measuredAt
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                    )
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AiAccessCard(
    setup: UserSetup
) {
    val policy = setup.aiDataAccessPolicy
    val title = if (policy.enabled) {
        stringResource(R.string.dashboard_ai_enabled_title)
    } else {
        stringResource(R.string.dashboard_ai_disabled_title)
    }
    val body = if (policy.enabled) {
        stringResource(
            R.string.dashboard_ai_enabled_body,
            policy.scopes.size
        )
    } else {
        stringResource(R.string.dashboard_ai_disabled_body)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
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
            if (policy.enabled) {
                Text(
                    text = aiScopeSummary(policy.scopes),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun aiScopeSummary(
    scopes: Set<AiDataScope>
): String {
    val labels = AiDataScope.entries
        .filter(scopes::contains)
        .map { scope ->
            stringResource(
                when (scope) {
                    AiDataScope.PROFILE ->
                        R.string.ai_scope_profile

                    AiDataScope.GOAL ->
                        R.string.ai_scope_goal

                    AiDataScope.LATEST_WEIGHT ->
                        R.string.ai_scope_latest_weight

                    AiDataScope.SAFETY_SUMMARY ->
                        R.string.ai_scope_safety
                }
            )
        }

    return labels.joinToString(separator = " · ")
}

@Composable
private fun DataControlsCard(
    isDeleting: Boolean,
    deleteFailed: Boolean,
    onRequestDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(
                    R.string.dashboard_data_title
                ),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(
                    R.string.dashboard_data_body
                ),
                style = MaterialTheme.typography.bodyMedium
            )

            if (deleteFailed) {
                Text(
                    text = stringResource(
                        R.string.delete_data_error
                    ),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Button(
                onClick = onRequestDelete,
                enabled = !isDeleting,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                }
                Text(
                    text = stringResource(
                        R.string.dashboard_delete_data
                    )
                )
            }
        }
    }
}

@Composable
private fun CompactMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium
        )
    }
}
