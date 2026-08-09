// SPDX-FileCopyrightText: 2026 Valentin GIDON
// SPDX-License-Identifier: AGPL-3.0-only

package com.`val`.nutrigain

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppContextInstrumentedTest {

    @Test
    fun applicationId_isStable() {
        val appContext =
            InstrumentationRegistry.getInstrumentation()
                .targetContext

        assertEquals(
            "com.val.nutrigain",
            appContext.packageName
        )
    }
}
