package com.example.welive.settings

import com.example.welive.protection.ProtectedApp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppSettingsProtectedAppsTest {
    @Test
    fun totalBlocksMapToTheRequestedAppOnly() {
        val settings = AppSettings(
            blockRedditAppCompletely = true,
            blockLinkedInWebsiteCompletely = true
        )

        assertTrue(settings.isTotalAppBlocked(ProtectedApp.REDDIT))
        assertFalse(settings.isTotalAppBlocked(ProtectedApp.LINKEDIN))
        assertTrue(settings.isTotalWebsiteBlocked(ProtectedApp.LINKEDIN))
        assertFalse(settings.isTotalWebsiteBlocked(ProtectedApp.REDDIT))
    }

    @Test
    fun tikTokMessagesOnlyTakesPrecedenceOverTotalAppBlock() {
        val settings = AppSettings(
            blockTikTokAppCompletely = true,
            blockTikTokShortForm = true
        )

        assertFalse(settings.isTotalAppBlocked(ProtectedApp.TIKTOK))
    }
}
