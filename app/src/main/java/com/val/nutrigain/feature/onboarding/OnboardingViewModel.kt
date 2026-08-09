// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.`val`.nutrigain.core.data.UserSetupRepository
import com.`val`.nutrigain.core.domain.CreateInitialPlanResult
import com.`val`.nutrigain.core.domain.CreateInitialPlanUseCase
import com.`val`.nutrigain.core.domain.InitialPlanRequest
import com.`val`.nutrigain.core.domain.PlanField
import com.`val`.nutrigain.core.domain.PlanValidationCode
import com.`val`.nutrigain.core.model.ActivityLevel
import com.`val`.nutrigain.core.model.GainPace
import com.`val`.nutrigain.core.model.MetabolicSex
import com.`val`.nutrigain.core.model.SafetyAnswers
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
    SAFETY
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.PROFILE,
    val birthDate: String = "",
    val heightCm: String = "",
    val currentWeightKg: String = "",
    val targetWeightKg: String = "",
    val metabolicSex: MetabolicSex = MetabolicSex.FEMALE,
    val activityLevel: ActivityLevel = ActivityLevel.LIGHT,
    val pace: GainPace = GainPace.PROGRESSIVE,
    val unintentionalWeightLoss: Boolean = false,
    val pregnantOrBreastfeeding: Boolean = false,
    val eatingDisorderHistory: Boolean = false,
    val significantDigestiveSymptoms: Boolean = false,
    val relevantMedicalCondition: Boolean = false,
    val relevantMedication: Boolean = false,
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
        mutableUiState.update {
            it.copy(
                metabolicSex = value,
                hasUnexpectedError = false
            )
        }
    }

    fun selectActivityLevel(value: ActivityLevel) {
        mutableUiState.update {
            it.copy(
                activityLevel = value,
                hasUnexpectedError = false
            )
        }
    }

    fun selectPace(value: GainPace) {
        mutableUiState.update {
            it.copy(
                pace = value,
                hasUnexpectedError = false
            )
        }
    }

    fun setUnintentionalWeightLoss(value: Boolean) =
        updateSafetyAnswer {
            copy(unintentionalWeightLoss = value)
        }

    fun setPregnantOrBreastfeeding(value: Boolean) =
        updateSafetyAnswer {
            copy(pregnantOrBreastfeeding = value)
        }

    fun setEatingDisorderHistory(value: Boolean) =
        updateSafetyAnswer {
            copy(eatingDisorderHistory = value)
        }

    fun setSignificantDigestiveSymptoms(value: Boolean) =
        updateSafetyAnswer {
            copy(significantDigestiveSymptoms = value)
        }

    fun setRelevantMedicalCondition(value: Boolean) =
        updateSafetyAnswer {
            copy(relevantMedicalCondition = value)
        }

    fun setRelevantMedication(value: Boolean) =
        updateSafetyAnswer {
            copy(relevantMedication = value)
        }

    fun setSafetyAcknowledged(value: Boolean) {
        mutableUiState.update { current ->
            current.copy(
                safetyAcknowledged = value,
                validationIssues = current.validationIssues -
                    PlanField.SAFETY_ACKNOWLEDGEMENT,
                hasUnexpectedError = false
            )
        }
    }

    fun goToPreviousStep() {
        if (mutableUiState.value.isSaving) {
            return
        }

        mutableUiState.update { current ->
            current.copy(
                step = when (current.step) {
                    OnboardingStep.PROFILE ->
                        OnboardingStep.PROFILE

                    OnboardingStep.LIFESTYLE ->
                        OnboardingStep.PROFILE

                    OnboardingStep.SAFETY ->
                        OnboardingStep.LIFESTYLE
                },
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
            OnboardingStep.PROFILE -> {
                when (val parsed = parseProfile(current)) {
                    is ProfileInputParseResult.Invalid ->
                        mutableUiState.update {
                            it.copy(
                                validationIssues = parsed.issues,
                                hasUnexpectedError = false
                            )
                        }

                    is ProfileInputParseResult.Success ->
                        mutableUiState.update {
                            it.copy(
                                step = OnboardingStep.LIFESTYLE,
                                validationIssues = emptyMap(),
                                hasUnexpectedError = false
                            )
                        }
                }
            }

            OnboardingStep.LIFESTYLE ->
                mutableUiState.update {
                    it.copy(
                        step = OnboardingStep.SAFETY,
                        hasUnexpectedError = false
                    )
                }

            OnboardingStep.SAFETY -> submit()
        }
    }

    fun submit() {
        val current = mutableUiState.value
        if (current.isSaving) {
            return
        }

        val parsedProfile = parseProfile(current)
        if (parsedProfile is ProfileInputParseResult.Invalid) {
            mutableUiState.update {
                it.copy(
                    step = OnboardingStep.PROFILE,
                    validationIssues = parsedProfile.issues,
                    hasUnexpectedError = false
                )
            }
            return
        }

        check(parsedProfile is ProfileInputParseResult.Success)

        if (!current.safetyAcknowledged) {
            mutableUiState.update {
                it.copy(
                    step = OnboardingStep.SAFETY,
                    validationIssues = mapOf(
                        PlanField.SAFETY_ACKNOWLEDGEMENT to
                            PlanValidationCode.ACKNOWLEDGEMENT_REQUIRED
                    ),
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
                val result = createInitialPlan(
                    InitialPlanRequest(
                        birthDate = parsedProfile.input.birthDate,
                        heightCm = parsedProfile.input.heightCm,
                        currentWeightKg =
                            parsedProfile.input.currentWeightKg,
                        targetWeightKg =
                            parsedProfile.input.targetWeightKg,
                        metabolicSex = current.metabolicSex,
                        activityLevel = current.activityLevel,
                        pace = current.pace,
                        safetyAnswers = current.toSafetyAnswers(),
                        safetyAcknowledged =
                            current.safetyAcknowledged
                    )
                )

                when (result) {
                    is CreateInitialPlanResult.Invalid ->
                        mutableUiState.update {
                            it.copy(
                                step = stepFor(
                                    result.issues.keys
                                ),
                                validationIssues = result.issues,
                                isSaving = false
                            )
                        }

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
        mutableUiState.update { current ->
            current
                .transform(value)
                .copy(
                    validationIssues =
                        current.validationIssues - field,
                    hasUnexpectedError = false
                )
        }
    }

    private fun updateSafetyAnswer(
        transform: OnboardingUiState.() -> OnboardingUiState
    ) {
        mutableUiState.update {
            it.transform().copy(hasUnexpectedError = false)
        }
    }

    private fun OnboardingUiState.toSafetyAnswers(): SafetyAnswers {
        return SafetyAnswers(
            unintentionalWeightLoss =
                unintentionalWeightLoss,
            pregnantOrBreastfeeding =
                pregnantOrBreastfeeding,
            eatingDisorderHistory =
                eatingDisorderHistory,
            significantDigestiveSymptoms =
                significantDigestiveSymptoms,
            relevantMedicalCondition =
                relevantMedicalCondition,
            relevantMedication = relevantMedication
        )
    }

    private fun stepFor(
        fields: Set<PlanField>
    ): OnboardingStep {
        return if (
            fields.any {
                it == PlanField.BIRTH_DATE ||
                    it == PlanField.HEIGHT ||
                    it == PlanField.CURRENT_WEIGHT ||
                    it == PlanField.TARGET_WEIGHT
            }
        ) {
            OnboardingStep.PROFILE
        } else {
            OnboardingStep.SAFETY
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

    private companion object {
        const val MAX_DECIMAL_INPUT_LENGTH = 7
    }
}
