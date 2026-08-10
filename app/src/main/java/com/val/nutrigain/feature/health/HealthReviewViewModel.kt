// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.`val`.nutrigain.core.data.UserSetupRepository
import com.`val`.nutrigain.core.domain.HealthQuestionnairePolicy
import com.`val`.nutrigain.core.domain.HealthQuestionnaireReviewRequest
import com.`val`.nutrigain.core.domain.PlanField
import com.`val`.nutrigain.core.domain.PlanValidationCode
import com.`val`.nutrigain.core.domain.ReviewHealthQuestionnaireResult
import com.`val`.nutrigain.core.domain.ReviewHealthQuestionnaireUseCase
import com.`val`.nutrigain.core.model.HealthAnswer
import com.`val`.nutrigain.core.model.HealthQuestion
import com.`val`.nutrigain.core.model.HealthQuestionnaireAnswers
import com.`val`.nutrigain.core.model.UserSetup
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HealthReviewUiState(
    val answers:
        Map<HealthQuestion, HealthAnswer> = emptyMap(),
    val medicalContextNote: String = "",
    val safetyAcknowledged: Boolean = false,
    val validationIssues:
        Map<PlanField, PlanValidationCode> = emptyMap(),
    val isSaving: Boolean = false,
    val isReady: Boolean = false,
    val saveCompleted: Boolean = false,
    val hasUnexpectedError: Boolean = false
)

@HiltViewModel
class HealthReviewViewModel @Inject constructor(
    private val reviewHealthQuestionnaire:
        ReviewHealthQuestionnaireUseCase,
    private val repository: UserSetupRepository
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(
        HealthReviewUiState()
    )
    val uiState: StateFlow<HealthReviewUiState> =
        mutableUiState.asStateFlow()

    /*
     * Cette référence reste uniquement en mémoire le temps de l'écran. Elle
     * évite de reconstruire un UserSetup depuis des champs UI partiels.
     */
    private var currentSetup: UserSetup? = null

    fun begin(setup: UserSetup) {
        if (mutableUiState.value.isSaving) {
            return
        }

        currentSetup = setup
        mutableUiState.value = HealthReviewUiState(
            answers = setup.safetyProfile.answers.asMap(),
            medicalContextNote =
                setup.safetyProfile.medicalContextNote.orEmpty(),
            isReady = true
        )
    }

    fun updateAnswer(
        question: HealthQuestion,
        answer: HealthAnswer
    ) {
        updateEditable { current ->
            current.copy(
                answers = current.answers + (question to answer),
                validationIssues = current.validationIssues -
                    PlanField.HEALTH_QUESTIONNAIRE,
                hasUnexpectedError = false
            )
        }
    }

    fun updateMedicalContextNote(value: String) {
        if (
            value.length >
            HealthQuestionnairePolicy.MAX_MEDICAL_NOTE_LENGTH
        ) {
            return
        }

        updateEditable { current ->
            current.copy(
                medicalContextNote = value,
                validationIssues = current.validationIssues -
                    PlanField.MEDICAL_CONTEXT_NOTE,
                hasUnexpectedError = false
            )
        }
    }

    fun setSafetyAcknowledged(value: Boolean) {
        updateEditable { current ->
            current.copy(
                safetyAcknowledged = value,
                validationIssues = current.validationIssues -
                    PlanField.SAFETY_ACKNOWLEDGEMENT,
                hasUnexpectedError = false
            )
        }
    }

    fun save() {
        val state = mutableUiState.value
        val setup = currentSetup
        if (
            state.isSaving ||
            !state.isReady ||
            setup == null
        ) {
            return
        }

        val answers = HealthQuestionnaireAnswers.from(
            state.answers
        )
        val issues = linkedMapOf<PlanField, PlanValidationCode>()

        if (answers == null) {
            issues[PlanField.HEALTH_QUESTIONNAIRE] =
                PlanValidationCode.QUESTIONNAIRE_INCOMPLETE
        }
        if (
            !HealthQuestionnairePolicy.isMedicalContextNoteValid(
                state.medicalContextNote
            )
        ) {
            issues[PlanField.MEDICAL_CONTEXT_NOTE] =
                PlanValidationCode.MEDICAL_NOTE_TOO_LONG
        }
        if (!state.safetyAcknowledged) {
            issues[PlanField.SAFETY_ACKNOWLEDGEMENT] =
                PlanValidationCode.ACKNOWLEDGEMENT_REQUIRED
        }

        if (issues.isNotEmpty() || answers == null) {
            mutableUiState.update {
                it.copy(
                    validationIssues = issues,
                    hasUnexpectedError = false
                )
            }
            return
        }

        mutableUiState.update {
            it.copy(
                isSaving = true,
                validationIssues = emptyMap(),
                hasUnexpectedError = false
            )
        }

        viewModelScope.launch {
            try {
                when (
                    val result = reviewHealthQuestionnaire(
                        currentSetup = setup,
                        request =
                            HealthQuestionnaireReviewRequest(
                                answers = answers,
                                medicalContextNote =
                                    state.medicalContextNote,
                                safetyAcknowledged =
                                    state.safetyAcknowledged
                            )
                    )
                ) {
                    is ReviewHealthQuestionnaireResult.Invalid ->
                        mutableUiState.update {
                            it.copy(
                                validationIssues = result.issues,
                                isSaving = false
                            )
                        }

                    is ReviewHealthQuestionnaireResult.Success -> {
                        repository.saveSafetyReview(
                            safetyProfile =
                                result.updatedSetup.safetyProfile,
                            goal = result.updatedSetup.goal
                        )
                        currentSetup = result.updatedSetup
                        mutableUiState.update {
                            it.copy(
                                isSaving = false,
                                saveCompleted = true
                            )
                        }
                    }
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                /*
                 * Les détails de stockage ne sont ni affichés ni journalisés,
                 * afin de ne pas divulguer d'information médicale.
                 */
                mutableUiState.update {
                    it.copy(
                        isSaving = false,
                        hasUnexpectedError = true
                    )
                }
            }
        }
    }

    fun consumeSaveCompleted() {
        mutableUiState.update {
            it.copy(saveCompleted = false)
        }
    }

    private inline fun updateEditable(
        crossinline transform:
            (HealthReviewUiState) -> HealthReviewUiState
    ) {
        mutableUiState.update { current ->
            if (current.isSaving) {
                current
            } else {
                transform(current)
            }
        }
    }
}
