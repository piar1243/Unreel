package com.example.welive.protection

enum class ProtectedApp(
    val displayName: String,
    val packageNames: Set<String>,
    val domains: Set<String>
) {
    INSTAGRAM(
        "Instagram",
        setOf("com.instagram.android"),
        setOf("instagram.com")
    ),
    YOUTUBE(
        "YouTube",
        setOf("com.google.android.youtube"),
        setOf("youtube.com", "youtu.be")
    ),
    TIKTOK(
        "TikTok",
        setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
        setOf("tiktok.com")
    ),
    SNAPCHAT(
        "Snapchat",
        setOf("com.snapchat.android"),
        setOf("snapchat.com")
    ),
    X(
        "X",
        setOf("com.twitter.android"),
        setOf("x.com", "twitter.com")
    ),
    THREADS(
        "Threads",
        setOf("com.instagram.barcelona"),
        setOf("threads.net")
    ),
    REDDIT(
        "Reddit",
        setOf("com.reddit.frontpage"),
        setOf("reddit.com")
    ),
    LINKEDIN(
        "LinkedIn",
        setOf("com.linkedin.android"),
        setOf("linkedin.com")
    );

    companion object {
        fun fromPackage(packageName: String): ProtectedApp? {
            return entries.firstOrNull { packageName in it.packageNames }
        }
    }
}
