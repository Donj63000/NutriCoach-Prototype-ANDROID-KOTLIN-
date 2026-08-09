// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.di

import android.content.Context
import androidx.room.Room
import com.`val`.nutrigain.core.database.DatabaseKeyManager
import com.`val`.nutrigain.core.database.NutriCoachDatabase
import com.`val`.nutrigain.core.database.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Module
@InstallIn(SingletonComponent::class)
object PersistenceModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
        keyManager: DatabaseKeyManager
    ): NutriCoachDatabase {
        val passphrase = keyManager.getOrCreatePassphrase(
            databaseAlreadyExists = databaseArtifactsExist(
                context = context,
                databaseName = DATABASE_NAME
            )
        )
        val sqlCipherFactory = SupportOpenHelperFactory(passphrase)

        return Room.databaseBuilder(
            context,
            NutriCoachDatabase::class.java,
            DATABASE_NAME
        )
            .openHelperFactory(sqlCipherFactory)
            .build()
    }

    @Provides
    fun provideProfileDao(
        database: NutriCoachDatabase
    ): ProfileDao = database.profileDao()

    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemDefaultZone()

    private fun databaseArtifactsExist(
        context: Context,
        databaseName: String
    ): Boolean {
        val databaseFile = context.getDatabasePath(databaseName)
        return DATABASE_FILE_SUFFIXES.any { suffix ->
            java.io.File(databaseFile.path + suffix).exists()
        }
    }

    private const val DATABASE_NAME = "nutricoach.db"
    private val DATABASE_FILE_SUFFIXES = listOf(
        "",
        "-journal",
        "-shm",
        "-wal"
    )
}
