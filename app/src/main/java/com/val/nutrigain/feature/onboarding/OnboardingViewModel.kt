// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.`val`.nutrigain.core.data.UserSetupRepository
import com.`val`.nutrigain.core.domain.CreateInitialPlanResult
import com.`val`.nutrigain.core.domain.CreateInitialPlanUseCase
import com.`val`.nutrigain.core.domain.HealthQuestionnairePolicy
import com.`val`.nutrigain.core.domain.InitialPlanRequest
import com.`val`.nutrigain.core.domain.PlanField
import com.`val`.nutrigain.core.domain.PlanValidationCode
import com.`val`.nutrigain.core.model.ActivityLevel
import com.`val`.nutrigain.core.model.GainPace
import com.`val`.nutrigain.core.model.HealthAnswer
import com.`val`.nutrigain.core.model.HealthQuestion
import com.`val`.nutrigain.core.model.HealthQuestionnaireAnswers
import com.`val`.nutrigain.core.model.MetabolicSex
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class OnboardingStep {
    PROFILE,
    LIFESTYLE,
    HEALTH,
    PRIVACY
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.PROFILE,
    val birthDate: String = "",
    val heightCm: String = "",
    val currentWeightKg: String = "",
    val targetWeightKg: String = "",
    /*
     * Les choix restent nuls jusqu'à une action explicite. Une valeur par
     * défaut silencieuse serait enregistrée comme si l'utilisateur l'avait
     * réellement choisie.
     */
    val metabolicSex: MetabolicSex? = null,
    val activityLevel: ActivityLevel? = null,
    val pace: GainPace? = null,
    val healthAnswers:
        Map<HealthQuestion, HealthAnswer> = emptyMap(),
    val medicalContextNote: String = "",
    val safetyAcknowledged: Boolean = false,
    val validationIssues:
        Map<PlanField, PlanValidationCode> = emptyMap(),
    val isSaving: Boolean = false,
    val hasUnexpectedError: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val inputParser: OnboardingInputParser,
    private val createInitialPlan: CreateInitialPlanUseCase,
    private val repository: UserSetupRepository
) : ViewModel() {

    private val mutableUiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> =
        mutableUiState.asStateFlow()

    fun updateBirthDate(value: String) {
        val formattedValue =
            OnboardingInputFormatter.formatBirthDate(value) ?: return

        updateTextField(
            value = formattedValue,
            field = PlanField.BIRTH_DATE
        ) { copy(birthDate = it) }
    }

    fun updateHeight(value: String) {
        updateDecimalField(
            value = value,
            field = PlanField.HEIGHT
        ) { copy(heightCm = it) }
    }

    fun updateCurrentWeight(value: String) {
        updateDecimalField(
            value = value,
            field = PlanField.CURRENT_WEIGHT
        ) { copy(currentWeightKg = it) }
    }

    fun updateTargetWeight(value: String) {
        updateDecimalField(
            value = value,
            field = PlanField.TARGET_WEIGHT
        ) { copy(targetWeightKg = it) }
    }

    fun selectMetabolicSex(value: MetabolicSex) {
        updateEditable { current ->
            current.copy(
                metabolicSex = value,
                validationIssues = current.validationIssues -
                    PlanField.METABOLIC_SEX,
                hasUnexpectedError = false
            )
        }
    }

    fun selectActivityLevel(value: ActivityLevel) {
        updateEditable { current ->
            current.copy(
                activityLevel = value,
                validationIssues = current.validationIssues -
                    PlanField.ACTIVITY_LEVEL,
                hasUnexpectedError = false
            )
        }
    }

    fun selectPace(value: GainPace) {
        updateEditable { current ->
            current.copy(
                pace = value,
                validationIssues = current.validationIssues -
                    PlanField.GAIN_PACE,
                hasUnexpectedError = false
            )
        }
    }

    fun updateHealthAnswer(
        question: HealthQuestion,
        answer: HealthAnswer
    ) {
        updateEditable { current ->
            current.copy(
                healthAnswers =
                    current.healthAnswers + (question to answer),
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

        updateTextField(
            value = value,
            field = PlanField.MEDICAL_CONTEXT_NOTE
        ) { copy(medicalContextNote = it) }
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

    fun goToPreviousStep() {
        updateEditable { current ->
            current.copy(
                step = when (current.step) {
                    OnboardingStep.PROFILE ->
                        OnboardingStep.PROFILE

                    OnboardingStep.LIFESTYLE ->
                        OnboardingStep.PROFILE

                    OnboardingStep.HEALTH ->
                        OnboardingStep.LIFESTYLE

                    OnboardingStep.PRIVACY ->
                        OnboardingStep.HEALTH
                },
                validationIssues = emptyMap(),
                hasUnexpectedError = false
            )
        }
    }

    fun goToNextStep() {
        val current = mutableUiState.value
        if (current.isSaving) {
            return
        }

        when (current.step) {
            OnboardingStep.PROFILE -> moveAfterProfile(current)
            OnboardingStep.LIFESTYLE -> moveAfterLifestyle(current)
            OnboardingStep.HEALTH -> moveAfterHealth(current)
            OnboardingStep.PRIVACY -> submit()
        }
    }

    fun submit() {
        val current = mutableUiState.value
        if (current.isSaving) {
            return
        }

        val profileResult = parseProfile(current)
        if (profileResult is ProfileInputParseResult.Invalid) {
            showIssues(
                step = OnboardingStep.PROFILE,
                issues = profileResult.issues
            )
            return
        }

        val selectionIssues = validateSelections(current)
        if (selectionIssues.isNotEmpty()) {
            showIssues(
                step = stepFor(selectionIssues.keys),
                issues = selectionIssues
            )
            return
        }

        val healthAnswers = HealthQuestionnaireAnswers.from(
            current.healthAnswers
        )
        if (healthAnswers == null) {
            showIssues(
                step = OnboardingStep.HEALTH,
                issues = mapOf(
                    PlanField.HEALTH_QUESTIONNAIRE to
                        PlanValidationCode.QUESTIONNAIRE_INCOMPLETE
                )
            )
            return
        }

        if (
            !HealthQuestionnairePolicy.isMedicalContextNoteValid(
                current.medicalContextNote
            )
        ) {
            showIssues(
                step = OnboardingStep.HEALTH,
                issues = mapOf(
                    PlanField.MEDICAL_CONTEXT_NOTE to
                        PlanValidationCode.MEDICAL_NOTE_TOO_LONG
                )
            )
            return
        }

        if (!current.safetyAcknowledged) {
            showIssues(
                step = OnboardingStep.PRIVACY,
                issues = mapOf(
                    PlanField.SAFETY_ACKNOWLEDGEMENT to
                        PlanValidationCode.ACKNOWLEDGEMENT_REQUIRED
                )
            )
            return
        }

        check(profileResult is ProfileInputParseResult.Success)
        val metabolicSex = current.metabolicSex
        val activityLevel = current.activityLevel
        val pace = current.pace

        /*
         * Ces valeurs ont été validées juste au-dessus. Le garde explicite
         * évite néanmoins tout crash si l'état évolue ultérieurement.
         */
        if (
            metabolicSex == null ||
            activityLevel == null ||
            pace == null
        ) {
            val unexpectedSelectionIssues = validateSelections(current)
                .ifEmpty {
                    /*
                     * Ce repli ne devrait jamais être atteint : il protège
                     * néanmoins l'interface si une future évolution de la
                     * validation diverge de l'état attendu ici.
                     */
                    mapOf(
                        PlanField.METABOLIC_SEX to
                            PlanValidationCode.SELECTION_REQUIRED
                    )
                }

            showIssues(
                step = stepFor(unexpectedSelectionIssues.keys),
                issues = unexpectedSelectionIssues
            )
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
                val result = createInitialPlan(
                    InitialPlanRequest(
                        birthDate = profileResult.input.birthDate,
                        heightCm = profileResult.input.heightCm,
                        currentWeightKg =
                            profileResult.input.currentWeightKg,
                        targetWeightKg =
                            profileResult.input.targetWeightKg,
                        metabolicSex = metabolicSex,
                        activityLevel = activityLevel,
                        pace = pace,
                        healthAnswers = healthAnswers,
                        medicalContextNote =
                            current.medicalContextNote,
                        safetyAcknowledged =
                            current.safetyAcknowledged
                    )
                )

                when (result) {
                    is CreateInitialPlanResult.Invalid ->
                        showIssues(
                            step = stepFor(result.issues.keys),
                            issues = result.issues,
                            stopSaving = true
                        )

                    is CreateInitialPlanResult.Success ->
                        repository.saveInitialSetup(result.setup)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                /*
                 * Le détail de l'erreur n'est pas placé dans l'état UI :
                 * il pourrait contenir des informations sur le stockage.
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

    private fun moveAfterProfile(current: OnboardingUiState) {
        val parsed = parseProfile(current)
        val issues = linkedMapOf<PlanField, PlanValidationCode>()

        if (parsed is ProfileInputParseResult.Invalid) {
            issues += parsed.issues
        }
        if (current.metabolicSex == null) {
            issues[PlanField.METABOLIC_SEX] =
                PlanValidationCode.SELECTION_REQUIRED
        }

        if (issues.isNotEmpty()) {
            showIssues(OnboardingStep.PROFILE, issues)
        } else {
            moveTo(OnboardingStep.LIFESTYLE)
        }
    }

    private fun moveAfterLifestyle(current: OnboardingUiState) {
        val issues = linkedMapOf<PlanField, PlanValidationCode>()
        if (current.activityLevel == null) {
            issues[PlanField.ACTIVITY_LEVEL] =
                PlanValidationCode.SELECTION_REQUIRED
        }
        if (current.pace == null) {
            issues[PlanField.GAIN_PACE] =
                PlanValidationCode.SELECTION_REQUIRED
        }

        if (issues.isNotEmpty()) {
            showIssues(OnboardingStep.LIFESTYLE, issues)
        } else {
            moveTo(OnboardingStep.HEALTH)
        }
    }

    private fun moveAfterHealth(current: OnboardingUiState) {
        val issues = linkedMapOf<PlanField, PlanValidationCode>()
        if (
            HealthQuestionnaireAnswers.from(
                current.healthAnswers
            ) == null
        ) {
            issues[PlanField.HEALTH_QUESTIONNAIRE] =
                PlanValidationCode.QUESTIONNAIRE_INCOMPLETE
        }
        if (
            !HealthQuestionnairePolicy.isMedicalContextNoteValid(
                current.medicalContextNote
            )
        ) {
            issues[PlanField.MEDICAL_CONTEXT_NOTE] =
                PlanValidationCode.MEDICAL_NOTE_TOO_LONG
        }

        if (issues.isNotEmpty()) {
            showIssues(OnboardingStep.HEALTH, issues)
        } else {
            moveTo(OnboardingStep.PRIVACY)
        }
    }

    private fun validateSelections(
        state: OnboardingUiState
    ): Map<PlanField, PlanValidationCode> {
        return buildMap {
            if (state.metabolicSex == null) {
                put(
                    PlanField.METABOLIC_SEX,
                    PlanValidationCode.SELECTION_REQUIRED
                )
            }
            if (state.activityLevel == null) {
                put(
                    PlanField.ACTIVITY_LEVEL,
                    PlanValidationCode.SELECTION_REQUIRED
                )
            }
            if (state.pace == null) {
                put(
                    PlanField.GAIN_PACE,
                    PlanValidationCode.SELECTION_REQUIRED
                )
            }
        }
    }

    private fun parseProfile(
        state: OnboardingUiState
    ): ProfileInputParseResult {
        return inputParser.parseProfile(
            birthDateText = state.birthDate,
            heightText = state.heightCm,
            currentWeightText = state.currentWeightKg,
            targetWeightText = state.targetWeightKg
        )
    }

    private fun updateTextField(
        value: String,
        field: PlanField,
        transform: OnboardingUiState.(String) -> OnboardingUiState
    ) {
        updateEditable { current ->
            current
                .transform(value)
                .copy(
                    validationIssues =
                        current.validationIssues - field,
                    hasUnexpectedError = false
                )
        }
    }

    private fun updateDecimalField(
        value: String,
        field: PlanField,
        transform: OnboardingUiState.(String) -> OnboardingUiState
    ) {
        if (
            value.length > MAX_DECIMAL_INPUT_LENGTH ||
            value.any { character ->
                !character.isDigit() &&
                    character != ',' &&
                    character != '.'
            }
        ) {
            return
        }

        updateTextField(
            value = value,
            field = field,
            transform = transform
        )
    }

    private fun moveTo(step: OnboardingStep) {
        mutableUiState.update {
            it.copy(
                step = step,
                validationIssues = emptyMap(),
                hasUnexpectedError = false
            )
        }
    }

    private fun showIssues(
        step: OnboardingStep,
        issues: Map<PlanField, PlanValidationCode>,
        stopSaving: Boolean = false
    ) {
        mutableUiState.update {
            it.copy(
                step = step,
                validationIssues = issues,
                isSaving = if (stopSaving) false else it.isSaving,
                hasUnexpectedError = false
            )
        }
    }

    private fun stepFor(
        fields: Set<PlanField>
    ): OnboardingStep {
        return when {
            fields.any {
                it == PlanField.BIRTH_DATE ||
                    it == PlanField.HEIGHT ||
                    it == PlanField.CURRENT_WEIGHT ||
                    it == PlanField.TARGET_WEIGHT ||
                    it == PlanField.METABOLIC_SEX
            } -> OnboardingStep.PROFILE

            fields.any {
                it == PlanField.ACTIVITY_LEVEL ||
                    it == PlanField.GAIN_PACE
            } -> OnboardingStep.LIFESTYLE

            fields.any {
                it == PlanField.HEALTH_QUESTIONNAIRE ||
                    it == PlanField.MEDICAL_CONTEXT_NOTE
            } -> OnboardingStep.HEALTH

            else -> OnboardingStep.PRIVACY
        }
    }

    private inline fun updateEditable(
        crossinline transform:
            (OnboardingUiState) -> OnboardingUiState
    ) {
        mutableUiState.update { current ->
            if (current.isSaving) {
                current
            } else {
                transform(current)
            }
        }
    }

    private companion object {
        const val MAX_DECIMAL_INPUT_LENGTH = 7
    }
}
