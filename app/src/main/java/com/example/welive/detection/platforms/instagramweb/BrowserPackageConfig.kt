package com.example.welive.detection.platforms.instagramweb

object BrowserPackageConfig {
    private val dedicatedBrowserPackages = setOf(
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

    private val embeddedBrowserPackages = setOf(
        "com.google.android.gm",
        "com.google.android.googlequicksearchbox",
        "com.google.android.webview",
        "com.android.webview"
    )

    fun isSupported(packageName: String): Boolean {
        return isDedicatedBrowser(packageName) || isEmbeddedBrowserHost(packageName)
    }

    fun isDedicatedBrowser(packageName: String): Boolean {
        return packageName in dedicatedBrowserPackages
    }

    fun isEmbeddedBrowserHost(packageName: String): Boolean {
        return packageName in embeddedBrowserPackages
    }
}
