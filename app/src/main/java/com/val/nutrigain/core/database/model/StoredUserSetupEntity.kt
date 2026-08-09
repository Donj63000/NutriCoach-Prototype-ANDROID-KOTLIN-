// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database.model

import androidx.room.Embedded
import com.`val`.nutrigain.core.database.entity.GoalEntity
import com.`val`.nutrigain.core.database.entity.SafetyProfileEntity
import com.`val`.nutrigain.core.database.entity.UserProfileEntity
import com.`val`.nutrigain.core.database.entity.WeightEntryEntity

/**
 * Projection atomique de l'état nécessaire au démarrage de l'application.
 *
 * Les objets imbriqués sont nuls lorsque leur ligne n'existe pas. Une seule
 * requête évite les états transitoires observables entre plusieurs Flow Room
 * après l'enregistrement transactionnel du questionnaire.
 */
data class StoredUserSetupEntity(
    @Embedded(prefix = "profile_")
    val profile: UserProfileEntity?,
    @Embedded(prefix = "safety_")
    val safetyProfile: SafetyProfileEntity?,
    @Embedded(prefix = "goal_")
    val goal: GoalEntity?,
    @Embedded(prefix = "weight_")
    val latestWeight: WeightEntryEntity?
)
