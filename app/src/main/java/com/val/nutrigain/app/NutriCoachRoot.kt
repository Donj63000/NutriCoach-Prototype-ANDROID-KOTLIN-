// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.`val`.nutrigain.R
import com.`val`.nutrigain.feature.dashboard.DashboardRoute
import com.`val`.nutrigain.feature.health.HealthReviewRoute
import com.`val`.nutrigain.feature.onboarding.OnboardingRoute

private enum class ReadyDestination {
    DASHBOARD,
    HEALTH_REVIEW
}

@Composable
fun NutriCoachRoot(
    viewModel: AppStartViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var destination by rememberSaveable {
        mutableStateOf(ReadyDestination.DASHBOARD)
    }

    /*
     * Après une suppression complète, une future recréation de profil doit
     * toujours revenir au tableau de bord et non rouvrir un ancien écran de
     * révision conservé par rememberSaveable.
     */
    LaunchedEffect(state is AppStartUiState.Ready) {
        if (state !is AppStartUiState.Ready) {
            destination = ReadyDestination.DASHBOARD
        }
    }

    when (val current = state) {
        AppStartUiState.Loading -> LoadingScreen()
        AppStartUiState.NeedsOnboarding -> OnboardingRoute()
        is AppStartUiState.Ready -> {
            when (destination) {
                ReadyDestination.DASHBOARD ->
                    DashboardRoute(
                        setup = current.setup,
                        onReviewHealth = {
                            destination =
                                ReadyDestination.HEALTH_REVIEW
                        }
                    )

                ReadyDestination.HEALTH_REVIEW ->
                    HealthReviewRoute(
                        setup = current.setup,
                        onBack = {
                            destination =
                                ReadyDestination.DASHBOARD
                        },
                        onSaved = {
                            destination =
                                ReadyDestination.DASHBOARD
                        }
                    )
            }
        }

        AppStartUiState.LocalDataError -> LocalDataErrorScreen()
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(text = stringResource(R.string.loading))
        }
    }
}

@Composable
private fun LocalDataErrorScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(R.string.local_data_error),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
