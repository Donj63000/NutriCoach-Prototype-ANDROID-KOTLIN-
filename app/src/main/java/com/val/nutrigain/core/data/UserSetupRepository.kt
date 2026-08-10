// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.data

import com.`val`.nutrigain.core.model.AiDataAccessPolicy
import com.`val`.nutrigain.core.model.Goal
import com.`val`.nutrigain.core.model.SafetyProfile
import com.`val`.nutrigain.core.model.UserSetup
import kotlinx.coroutines.flow.Flow

sealed interface StoredUserSetup {
    data object Empty : StoredUserSetup
    data object Incomplete : StoredUserSetup

    data class Ready(
        val setup: UserSetup
    ) : StoredUserSetup
}

interface UserSetupRepository {
    fun observeSetup(): Flow<StoredUserSetup>

    suspend fun saveInitialSetup(setup: UserSetup)

    /**
     * Persiste atomiquement le résultat d'une nouvelle évaluation médicale.
     *
     * La cible calorique fait partie du même commit que le niveau de prudence :
     * une réponse sensible ne peut donc jamais laisser une ancienne cible
     * automatique active.
     */
    suspend fun saveSafetyReview(
        safetyProfile: SafetyProfile,
        goal: Goal
    )

    /**
     * Point d'entrée réservé à la future fonctionnalité d'IA.
     *
     * L'activation ne doit être appelée qu'après un consentement explicite,
     * versionné et révocable présenté par une interface dédiée.
     */
    suspend fun saveAiDataAccessPolicy(
        policy: AiDataAccessPolicy
    )

    /**
     * Supprime en une transaction toutes les données fonctionnelles locales.
     *
     * Le fichier de base chiffré et sa clé peuvent subsister, mais ils ne
     * contiennent plus aucune ligne utilisateur après validation du commit.
     */
    suspend fun deleteAllUserData()
}
