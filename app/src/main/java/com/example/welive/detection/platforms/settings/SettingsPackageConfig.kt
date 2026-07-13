package com.example.welive.detection.platforms.settings

object SettingsPackageConfig {
    val supportedPackages = setOf(
        "com.android.settings",
        "com.samsung.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.samsung.android.permissioncontroller",
        "com.android.vending"
    )

    private val supportedPackageFragments = setOf(
        ".settings",
        ".packageinstaller",
        ".permissioncontroller"
    )

    fun isSupported(packageName: String): Boolean {
        if (packageName.isBlank()) return false
        val normalized = packageName.lowercase()
        return normalized in supportedPackages ||
            supportedPackageFragments.any { fragment -> normalized.contains(fragment) }
    }
}
