// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_data_access_policy")
data class AiDataAccessPolicyEntity(
    @PrimaryKey
    val id: String,
    val enabled: Boolean,
    @ColumnInfo(name = "profile_scope_enabled")
    val profileScopeEnabled: Boolean,
    @ColumnInfo(name = "goal_scope_enabled")
    val goalScopeEnabled: Boolean,
    @ColumnInfo(name = "latest_weight_scope_enabled")
    val latestWeightScopeEnabled: Boolean,
    @ColumnInfo(name = "safety_summary_scope_enabled")
    val safetySummaryScopeEnabled: Boolean,
    @ColumnInfo(name = "consent_version")
    val consentVersion: String?,
    @ColumnInfo(name = "granted_at_epoch_ms")
    val grantedAtEpochMs: Long?,
    @ColumnInfo(name = "revoked_at_epoch_ms")
    val revokedAtEpochMs: Long?,
    @ColumnInfo(name = "updated_at_epoch_ms")
    val updatedAtEpochMs: Long
)
