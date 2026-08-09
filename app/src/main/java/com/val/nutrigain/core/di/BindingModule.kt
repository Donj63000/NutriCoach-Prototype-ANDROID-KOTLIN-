// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.di

import com.`val`.nutrigain.core.data.UserSetupRepository
import com.`val`.nutrigain.core.data.UserSetupRepositoryImpl
import com.`val`.nutrigain.core.domain.IdGenerator
import com.`val`.nutrigain.core.domain.UuidGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {

    @Binds
    @Singleton
    abstract fun bindUserSetupRepository(
        implementation: UserSetupRepositoryImpl
    ): UserSetupRepository

    @Binds
    abstract fun bindIdGenerator(
        implementation: UuidGenerator
    ): IdGenerator
}
