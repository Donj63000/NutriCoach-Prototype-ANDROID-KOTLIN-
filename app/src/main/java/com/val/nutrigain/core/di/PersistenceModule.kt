// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.`val`.nutrigain.core.database.DatabaseKeyManager
import com.`val`.nutrigain.core.database.MIGRATION_1_2
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
            /*
             * Toute évolution du schéma contenant des données sensibles doit
             * disposer d’une migration explicite et non destructive.
             */
            .addMigrations(MIGRATION_1_2)
            /*
             * SQLite écrase le contenu des cellules supprimées au lieu de les
             * laisser sur la liste libre. Le fichier reste chiffré par
             * SQLCipher ; cette option réduit en plus la rémanence logique
             * après la suppression d'un profil.
             */
            .addCallback(DATABASE_SECURITY_CALLBACK)
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

    private val DATABASE_SECURITY_CALLBACK =
        object : RoomDatabase.Callback() {
            override fun onOpen(
                database: SupportSQLiteDatabase
            ) {
                database.execSQL("PRAGMA secure_delete = ON")
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
