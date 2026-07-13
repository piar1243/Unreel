package com.example.welive.detection.platforms.settings

import com.example.welive.detection.ContentSurface
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.Platform
import com.example.welive.detection.WindowSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsUninstallDetectorTest {
    private val detector = SettingsUninstallDetector()

    @Test
    fun blocksUnreelAppInfoWithSettingsUninstallButton() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.settings",
                texts = setOf("app info", "unreel"),
                descriptions = setOf("uninstall"),
                viewIds = setOf(
                    "com.android.settings:id/entity_header",
                    "com.android.settings:id/entity_header_title",
                    "com.android.settings:id/floating_bottom_navigation",
                    "com.android.settings:id/uninstall_button"
                )
            )
        )

        assertEquals(Platform.SETTINGS, result.platform)
        assertEquals(ContentSurface.SETTINGS_UNINSTALL_UNREEL, result.surface)
        assertEquals(InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
        assertTrue(result.toString(), result.confidence >= 0.97f)
    }

    @Test
    fun blocksPackageInstallerUninstallConfirmationForUnreel() {
        val result = detector.detect(
            snapshot(
                packageName = "com.google.android.packageinstaller",
                texts = setOf("unreel", "uninstall", "cancel", "uninstall this app?"),
                viewIds = setOf(
                    "android:id/alertTitle",
                    "android:id/button1",
                    "android:id/button2",
                    "com.android.packageinstaller:id/app_label"
                )
            )
        )

        assertEquals(Platform.SETTINGS, result.platform)
        assertEquals(ContentSurface.SETTINGS_UNINSTALL_UNREEL, result.surface)
        assertEquals(InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
        assertTrue(result.toString(), result.confidence >= 0.99f)
    }

    @Test
    fun blocksPlayStoreUninstallSurfaceForUnreel() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.vending",
                texts = setOf("Unreel", "Uninstall", "Open"),
                descriptions = setOf("Uninstall Unreel"),
                viewIds = setOf(
                    "com.android.vending:id/details_title",
                    "com.android.vending:id/uninstall_button"
                )
            )
        )

        assertEquals(ContentSurface.SETTINGS_UNINSTALL_UNREEL, result.surface)
        assertEquals(InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
        assertTrue(result.confidence >= 0.98f)
    }

    @Test
    fun ignoresPlayStorePageForAnotherApp() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.vending",
                texts = setOf("Another app", "Uninstall", "Open"),
                viewIds = setOf("com.android.vending:id/uninstall_button")
            )
        )

        assertEquals(ContentSurface.SETTINGS_SAFE, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun blocksUnreelAccessibilityServiceDetailSwitchPage() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.settings",
                texts = setOf(
                    "app info",
                    "unreel shortform blocker",
                    "unreel shortform blocker shortcut",
                    "detects shortform surfaces and blocks instagram reels when enabled."
                ),
                descriptions = setOf(
                    "about unreel shortform blocker detects shortform surfaces and blocks instagram reels when enabled"
                ),
                viewIds = setOf(
                    "com.android.settings:id/action_bar",
                    "com.android.settings:id/recycler_view",
                    "com.android.settings:id/sesl_switchbar_container",
                    "com.android.settings:id/sesl_switchbar_switch"
                )
            )
        )

        assertEquals(Platform.SETTINGS, result.platform)
        assertEquals(ContentSurface.SETTINGS_ACCESSIBILITY_BLOCKER, result.surface)
        assertEquals(InterventionAction.BLOCK_AND_RETURN, result.recommendedAction)
        assertTrue(result.toString(), result.confidence >= 0.98f)
    }

    @Test
    fun ignoresSettingsSearchResultThatMentionsDeleteUnreel() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.settings.intelligence",
                texts = setOf("search", "recent searches", "unreel"),
                descriptions = setOf("delete app", "delete unreel", "more options"),
                viewIds = setOf(
                    "com.android.settings.intelligence:id/search_view",
                    "com.android.settings.intelligence:id/remove_icon",
                    "com.android.settings.intelligence:id/search_src_text"
                )
            )
        )

        assertEquals(Platform.SETTINGS, result.platform)
        assertEquals(ContentSurface.SETTINGS_SAFE, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun ignoresAccessibilityInstalledAppsList() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.settings",
                texts = setOf("installed apps", "unreel shortform blocker"),
                descriptions = setOf("back"),
                viewIds = setOf(
                    "com.android.settings:id/action_bar",
                    "com.android.settings:id/recycler_view",
                    "com.android.settings:id/collapsing_appbar_extended_title"
                )
            )
        )

        assertEquals(Platform.SETTINGS, result.platform)
        assertEquals(ContentSurface.SETTINGS_SAFE, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun ignoresGeneralAccessibilityPage() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.settings",
                texts = setOf("accessibility", "accessibility shortcuts", "installed apps", "recommended for you"),
                descriptions = setOf("more options"),
                viewIds = setOf(
                    "com.android.settings:id/action_bar",
                    "com.android.settings:id/recycler_view"
                )
            )
        )

        assertEquals(Platform.SETTINGS, result.platform)
        assertEquals(ContentSurface.SETTINGS_SAFE, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun ignoresOtherSettingsPages() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.settings",
                texts = setOf("apps", "default apps"),
                descriptions = setOf("search apps", "more options"),
                viewIds = setOf(
                    "com.android.settings:id/apps_list",
                    "com.android.settings:id/search_app_list_menu"
                )
            )
        )

        assertEquals(Platform.SETTINGS, result.platform)
        assertEquals(ContentSurface.SETTINGS_SAFE, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    @Test
    fun ignoresAppInfoForOtherApps() {
        val result = detector.detect(
            snapshot(
                packageName = "com.android.settings",
                texts = setOf("app info", "some other app"),
                descriptions = setOf("uninstall"),
                viewIds = setOf(
                    "com.android.settings:id/entity_header",
                    "com.android.settings:id/entity_header_title",
                    "com.android.settings:id/floating_bottom_navigation",
                    "com.android.settings:id/uninstall_button"
                )
            )
        )

        assertEquals(Platform.SETTINGS, result.platform)
        assertEquals(ContentSurface.SETTINGS_SAFE, result.surface)
        assertEquals(InterventionAction.NONE, result.recommendedAction)
    }

    private fun snapshot(
        packageName: String,
        texts: Set<String>,
        descriptions: Set<String> = emptySet(),
        viewIds: Set<String> = emptySet()
    ): WindowSnapshot {
        return WindowSnapshot(
            packageName = packageName,
            rootPackageName = packageName,
            eventPackageName = packageName,
            eventType = 0,
            texts = texts,
            contentDescriptions = descriptions,
            viewIds = viewIds,
            classNames = emptySet(),
            nodeCount = 20,
            scrollableNodeCount = 1
        )
    }
}
