// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.data

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
}
