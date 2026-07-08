package com.example.welive.detection.platforms.youtube

object YouTubePackageConfig {
    const val PACKAGE_NAME = "com.google.android.youtube"

    fun isYouTubeApp(packageName: String): Boolean {
        return packageName == PACKAGE_NAME
    }
}
