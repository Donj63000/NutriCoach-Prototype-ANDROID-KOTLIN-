// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.`val`.nutrigain.R
import com.`val`.nutrigain.core.model.SafetyLevel
import com.`val`.nutrigain.core.model.UserSetup
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

@Composable
fun DashboardScreen(
    setup: UserSetup
) {
    val decimalFormatter = remember {
        DecimalFormat(
            "0.#",
            DecimalFormatSymbols(Locale.FRANCE)
        )
    }

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
            SafetyStatusCard(level = setup.safetyProfile.level)
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
                    decimalFormatter.format(setup.goal.initialBmi)
                ),
                supportingText = stringResource(
                    R.string.dashboard_bmi_limit
                )
            )
        }

        item {
            CaloriePlanCard(setup = setup)
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
    level: SafetyLevel
) {
    val title: String
    val body: String
    val containerColor: Color
    val contentColor: Color

    when (level) {
        SafetyLevel.NORMAL -> {
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

        SafetyLevel.CAUTION -> {
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

        SafetyLevel.PROFESSIONAL_REVIEW_REQUIRED -> {
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
    setup: UserSetup
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
