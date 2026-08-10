// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.`val`.nutrigain.core.database.entity.AiDataAccessPolicyEntity
import com.`val`.nutrigain.core.database.entity.GoalEntity
import com.`val`.nutrigain.core.database.entity.SafetyProfileEntity
import com.`val`.nutrigain.core.database.entity.UserProfileEntity
import com.`val`.nutrigain.core.database.entity.WeightEntryEntity

@Database(
    entities = [
        UserProfileEntity::class,
        SafetyProfileEntity::class,
        GoalEntity::class,
        WeightEntryEntity::class,
        AiDataAccessPolicyEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class NutriCoachDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
}
