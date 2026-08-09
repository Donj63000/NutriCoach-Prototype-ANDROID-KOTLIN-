// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.`val`.nutrigain.core.data.StoredUserSetup
import com.`val`.nutrigain.core.data.UserSetupRepository
import com.`val`.nutrigain.core.model.UserSetup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface AppStartUiState {
    data object Loading : AppStartUiState
    data object NeedsOnboarding : AppStartUiState

    data class Ready(
        val setup: UserSetup
    ) : AppStartUiState

    data object LocalDataError : AppStartUiState
}

@HiltViewModel
class AppStartViewModel @Inject constructor(
    repository: UserSetupRepository
) : ViewModel() {

    val uiState = repository
        .observeSetup()
        .map<StoredUserSetup, AppStartUiState> { storedSetup ->
            when (storedSetup) {
                StoredUserSetup.Empty ->
                    AppStartUiState.NeedsOnboarding

                StoredUserSetup.Incomplete ->
                    AppStartUiState.LocalDataError

                is StoredUserSetup.Ready ->
                    AppStartUiState.Ready(storedSetup.setup)
            }
        }
        .catch {
            /*
             * Les détails techniques ne sont jamais affichés ni journalisés,
             * car ils pourraient révéler l'état du stockage local.
             */
            emit(AppStartUiState.LocalDataError)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(
                stopTimeoutMillis = 5_000
            ),
            initialValue = AppStartUiState.Loading
        )
}
