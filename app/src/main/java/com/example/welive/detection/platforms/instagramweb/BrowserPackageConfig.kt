package com.example.welive.detection.platforms.instagramweb

object BrowserPackageConfig {
    private val supportedPackages = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.chrome.canary",
        "com.sec.android.app.sbrowser",
        "com.brave.browser",
        "org.mozilla.firefox",
        "org.mozilla.fenix",
        "com.microsoft.emmx",
        "com.duckduckgo.mobile.android",
        "com.opera.browser",
        "com.vivaldi.browser"
    )

    fun isSupported(packageName: String): Boolean {
        return packageName in supportedPackages
    }
}
