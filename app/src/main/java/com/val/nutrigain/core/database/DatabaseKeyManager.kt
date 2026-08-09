// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain.core.database

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.IOException
import java.nio.ByteBuffer
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

class DatabaseKeyException(
    message: String,
    cause: Throwable? = null
) : IllegalStateException(message, cause)

@Singleton
class DatabaseKeyManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val applicationContext = context.applicationContext
    private val wrappedKeyFile = AtomicFile(
        File(applicationContext.noBackupFilesDir, WRAPPED_KEY_FILE_NAME)
    )
    private val lock = Any()

    fun getOrCreatePassphrase(
        databaseAlreadyExists: Boolean
    ): ByteArray = synchronized(lock) {
        try {
            if (wrappedPassphraseExists()) {
                try {
                    return@synchronized decryptStoredPassphrase()
                } catch (exception: Exception) {
                    if (databaseAlreadyExists) {
                        throw exception
                    }

                    /*
                     * Sans base existante, aucun contenu utilisateur ne dépend
                     * encore de cette clé. Un fichier incomplet laissé par une
                     * interruption peut donc être supprimé puis recréé.
                     */
                    wrappedKeyFile.delete()
                }
            }

            if (databaseAlreadyExists) {
                /*
                 * Générer une nouvelle clé rendrait la base existante
                 * définitivement illisible. On échoue explicitement plutôt
                 * que de masquer une perte de clé par une remise à zéro.
                 */
                throw DatabaseKeyException(
                    "La clé de chiffrement locale est absente alors que la base existe."
                )
            }

            val passphrase = ByteArray(PASSPHRASE_SIZE_BYTES).also {
                SecureRandom().nextBytes(it)
            }
            persistEncryptedPassphrase(passphrase)
            passphrase
        } catch (exception: DatabaseKeyException) {
            throw exception
        } catch (exception: GeneralSecurityException) {
            throw DatabaseKeyException(
                "Impossible d'accéder à la clé de chiffrement Android.",
                exception
            )
        } catch (exception: IOException) {
            throw DatabaseKeyException(
                "Impossible de lire ou d'écrire la clé de la base locale.",
                exception
            )
        }
    }

    private fun wrappedPassphraseExists(): Boolean {
        /*
         * AtomicFile peut conserver un ancien fichier « .bak » après une
         * interruption. openRead() sait le restaurer ; il ne faut donc pas
         * considérer la clé comme perdue lorsque seul ce secours existe.
         */
        val baseFile = wrappedKeyFile.baseFile
        return baseFile.exists() ||
            File(baseFile.path + LEGACY_BACKUP_SUFFIX).exists()
    }

    private fun persistEncryptedPassphrase(passphrase: ByteArray) {
        val masterKey = getOrCreateMasterKey(allowCreation = true)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, masterKey)
        cipher.updateAAD(ASSOCIATED_DATA)

        val ciphertext = cipher.doFinal(passphrase)
        val iv = cipher.iv
        val payload = ByteBuffer
            .allocate(
                HEADER_SIZE_BYTES +
                    iv.size +
                    ciphertext.size
            )
            .put(FORMAT_VERSION)
            .put(iv.size.toByte())
            .put(iv)
            .put(ciphertext)
            .array()

        val output = wrappedKeyFile.startWrite()
        try {
            output.write(payload)
            output.fd.sync()
            wrappedKeyFile.finishWrite(output)
        } catch (exception: Throwable) {
            wrappedKeyFile.failWrite(output)
            throw exception
        }
    }

    private fun decryptStoredPassphrase(): ByteArray {
        val payload = wrappedKeyFile.openRead().use { input ->
            input.readBytes()
        }

        if (payload.size !in MIN_PAYLOAD_SIZE_BYTES..MAX_PAYLOAD_SIZE_BYTES) {
            throw DatabaseKeyException(
                "Le fichier contenant la clé locale est invalide."
            )
        }

        val buffer = ByteBuffer.wrap(payload)
        val version = buffer.get()
        val ivLength = buffer.get().toInt() and 0xFF

        if (
            version != FORMAT_VERSION ||
            ivLength !in MIN_IV_SIZE_BYTES..MAX_IV_SIZE_BYTES ||
            buffer.remaining() <= ivLength
        ) {
            throw DatabaseKeyException(
                "Le format de la clé locale n'est pas reconnu."
            )
        }

        val iv = ByteArray(ivLength)
        buffer.get(iv)
        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val masterKey = getOrCreateMasterKey(allowCreation = false)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            masterKey,
            GCMParameterSpec(GCM_TAG_SIZE_BITS, iv)
        )
        cipher.updateAAD(ASSOCIATED_DATA)

        val passphrase = cipher.doFinal(ciphertext)
        if (passphrase.size != PASSPHRASE_SIZE_BYTES) {
            passphrase.fill(0)
            throw DatabaseKeyException(
                "La longueur de la clé locale déchiffrée est invalide."
            )
        }

        return passphrase
    }

    private fun getOrCreateMasterKey(
        allowCreation: Boolean
    ): SecretKey {
        val keyStore = KeyStore
            .getInstance(ANDROID_KEYSTORE)
            .apply { load(null) }

        val existingKey = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existingKey != null) {
            return existingKey
        }

        if (!allowCreation) {
            throw DatabaseKeyException(
                "La clé principale Android n'est plus disponible."
            )
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or
                    KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                    KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setKeySize(256)
                .setUserAuthenticationRequired(false)
                .build()
        )

        return keyGenerator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "nutricoach.database.master-key.v1"
        const val WRAPPED_KEY_FILE_NAME = "database-key-v1.bin"
        const val LEGACY_BACKUP_SUFFIX = ".bak"
        const val TRANSFORMATION = "AES/GCM/NoPadding"

        const val PASSPHRASE_SIZE_BYTES = 32
        const val GCM_TAG_SIZE_BITS = 128
        const val MIN_IV_SIZE_BYTES = 12
        const val MAX_IV_SIZE_BYTES = 32
        const val HEADER_SIZE_BYTES = 2
        const val MIN_PAYLOAD_SIZE_BYTES = 32
        const val MAX_PAYLOAD_SIZE_BYTES = 1_024

        val FORMAT_VERSION: Byte = 1
        val ASSOCIATED_DATA: ByteArray =
            "com.val.nutrigain/database-key/v1".toByteArray(Charsets.UTF_8)
    }
}
