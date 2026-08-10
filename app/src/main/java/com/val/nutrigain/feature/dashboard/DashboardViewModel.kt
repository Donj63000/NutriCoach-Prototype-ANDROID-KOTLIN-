// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.`val`.nutrigain.core.data.UserSetupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DashboardActionUiState(
    val isDeleting: Boolean = false,
    val deleteFailed: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: UserSetupRepository
) : ViewModel() {

    private val mutableActionState = MutableStateFlow(
        DashboardActionUiState()
    )
    val actionState: StateFlow<DashboardActionUiState> =
        mutableActionState.asStateFlow()

    fun deleteAllUserData() {
        if (mutableActionState.value.isDeleting) {
            return
        }

        mutableActionState.value = DashboardActionUiState(
            isDeleting = true
        )

        viewModelScope.launch {
            try {
                repository.deleteAllUserData()
                /*
                 * Ce ViewModel est attaché à l'activité dans la navigation
                 * actuelle. Il peut donc survivre au retour vers l'onboarding.
                 * Réinitialiser l'action après le commit évite qu'un futur
                 * tableau de bord reste définitivement verrouillé.
                 */
                mutableActionState.value = DashboardActionUiState()
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                mutableActionState.update {
                    it.copy(
                        isDeleting = false,
                        deleteFailed = true
                    )
                }
            }
        }
    }

    fun clearDeleteError() {
        mutableActionState.update {
            it.copy(deleteFailed = false)
        }
    }
}
