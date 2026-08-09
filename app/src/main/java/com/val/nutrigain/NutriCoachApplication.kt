// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import net.zetetic.database.Logger
import net.zetetic.database.NoopTarget

@HiltAndroidApp
class NutriCoachApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        /*
         * SQLCipher journalise vers Logcat par défaut. Les messages de sa
         * couche Java sont coupés avant l'ouverture d'une donnée locale.
         */
        Logger.setTarget(NoopTarget())

        // La bibliothèque native doit être chargée avant la première base Room.
        System.loadLibrary("sqlcipher")
    }
}
