package com.example.welive.intervention

import android.content.Context
import android.provider.Settings

class SystemGrayscaleController(
    private val context: Context
) {
    private var appliedByUnreel = false
    private var previousEnabled: Int? = null
    private var previousDaltonizer: Int? = null

    fun enable() {
        if (appliedByUnreel) return
        previousEnabled = getSecureInt(ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 0)
        previousDaltonizer = getSecureInt(ACCESSIBILITY_DISPLAY_DALTONIZER, DALTONIZER_DISABLED)

        val resolver = context.contentResolver
        val enabled = runCatching {
            Settings.Secure.putInt(resolver, ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED, 1)
        }.getOrDefault(false)
        val mode = runCatching {
            Settings.Secure.putInt(resolver, ACCESSIBILITY_DISPLAY_DALTONIZER, DALTONIZER_SIMULATE_MONOCHROMACY)
        }.getOrDefault(false)

        appliedByUnreel = enabled && mode
    }

    fun restore() {
        if (!appliedByUnreel) return
        val resolver = context.contentResolver
        runCatching {
            Settings.Secure.putInt(
                resolver,
                ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED,
                previousEnabled ?: 0
            )
            Settings.Secure.putInt(
                resolver,
                ACCESSIBILITY_DISPLAY_DALTONIZER,
                previousDaltonizer ?: DALTONIZER_DISABLED
            )
        }
        appliedByUnreel = false
        previousEnabled = null
        previousDaltonizer = null
    }

    private fun getSecureInt(name: String, defaultValue: Int): Int {
        return runCatching {
            Settings.Secure.getInt(context.contentResolver, name, defaultValue)
        }.getOrDefault(defaultValue)
    }

    private companion object {
        const val ACCESSIBILITY_DISPLAY_DALTONIZER_ENABLED = "accessibility_display_daltonizer_enabled"
        const val ACCESSIBILITY_DISPLAY_DALTONIZER = "accessibility_display_daltonizer"
        const val DALTONIZER_DISABLED = -1
        const val DALTONIZER_SIMULATE_MONOCHROMACY = 0
    }
}
