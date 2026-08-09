// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.DataInputStream
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedDatabaseInstrumentedTest {

    @Test
    fun baseChiffreePeutEtreRouverteSansEnteteSQLiteLisible() = runBlocking {
        val context =
            ApplicationProvider.getApplicationContext<Context>()
        val databaseName =
            "nutricoach-encryption-test-${UUID.randomUUID()}.db"
        val databaseFile = context.getDatabasePath(databaseName)
        val passphrase = ByteArray(PASSPHRASE_SIZE_BYTES).also {
            SecureRandom().nextBytes(it)
        }

        try {
            val database = openDatabase(
                context = context,
                databaseName = databaseName,
                passphrase = passphrase
            )
            try {
                /*
                 * La première requête force la création physique de la base ;
                 * construire l'objet Room seul ne suffit pas.
                 */
                val stored = database
                    .profileDao()
                    .observeCurrentSetup()
                    .first()

                assertNull(stored.profile)
            } finally {
                database.close()
            }

            val plainSQLiteHeader =
                "SQLite format 3\u0000".toByteArray(StandardCharsets.US_ASCII)
            val actualHeader = ByteArray(plainSQLiteHeader.size)
            DataInputStream(databaseFile.inputStream()).use { input ->
                input.readFully(actualHeader)
            }

            assertFalse(
                "Une base SQLCipher ne doit pas exposer l'entête SQLite.",
                actualHeader.contentEquals(plainSQLiteHeader)
            )

            val reopenedDatabase = openDatabase(
                context = context,
                databaseName = databaseName,
                passphrase = passphrase
            )
            try {
                assertNull(
                    reopenedDatabase
                        .profileDao()
                        .observeCurrentSetup()
                        .first()
                        .profile
                )
            } finally {
                reopenedDatabase.close()
            }
        } finally {
            passphrase.fill(0)
            context.deleteDatabase(databaseName)
        }
    }

    private fun openDatabase(
        context: Context,
        databaseName: String,
        passphrase: ByteArray
    ): NutriCoachDatabase {
        /*
         * SupportOpenHelperFactory efface par défaut le tableau reçu après
         * l'ouverture. Une copie protège la clé nécessaire au test de réouverture.
         */
        val factory = SupportOpenHelperFactory(passphrase.copyOf())

        return Room.databaseBuilder(
            context,
            NutriCoachDatabase::class.java,
            databaseName
        )
            .openHelperFactory(factory)
            .build()
    }

    private companion object {
        const val PASSPHRASE_SIZE_BYTES = 32
    }
}
