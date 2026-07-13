package com.example.welive.detection.platforms.settings

import com.example.welive.detection.ContentDetector
import com.example.welive.detection.ContentSurface
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.Platform
import com.example.welive.detection.WindowSnapshot

class SettingsUninstallDetector(
    private val appName: String = "unreel",
    private val packageName: String = "com.example.welive",
    private val blockingThreshold: Float = 0.84f
) : ContentDetector {
    override fun detect(snapshot: WindowSnapshot): DetectionResult {
        if (
            !SettingsPackageConfig.isSupported(snapshot.packageName) &&
            !SettingsPackageConfig.isSupported(snapshot.rootPackageName) &&
            !SettingsPackageConfig.isSupported(snapshot.eventPackageName)
        ) {
            return unknown(snapshot, "Package is not an uninstall or Settings host")
        }

        val reasons = mutableListOf<String>()
        val packageNames = listOf(
            snapshot.packageName,
            snapshot.rootPackageName,
            snapshot.eventPackageName
        ).map { it.lowercase() }
        val isPackageInstaller = packageNames.any { packageName ->
            packageName.contains("packageinstaller") ||
                packageName.contains("permissioncontroller")
        }
        val isSettingsAppInfoHost = packageNames.any { packageName ->
            packageName == "com.android.settings" ||
                packageName == "com.samsung.android.settings"
        }
        val isPlayStore = packageNames.any { it == "com.android.vending" }
        val nameHit = snapshot.containsTextOrDescriptionAny(appName)
        val packageHit = snapshot.containsAny(packageName)
        val appMarkerHit = nameHit || packageHit
        val uninstallTextHit = snapshot.containsTextOrDescriptionAny(
            "uninstall",
            "remove app"
        )
        val appInfoHit = snapshot.containsTextOrDescriptionAny(
            "app info",
            "app details"
        )
        val uninstallConfirmationHit = snapshot.containsTextOrDescriptionAny(
            "uninstall this app"
        )
        val cancelActionHit = snapshot.containsTextOrDescriptionAny(
            "cancel"
        )
        val packageInstallerConfirmIdHit = snapshot.containsViewIdAny(
            "alerttitle",
            "button1",
            "app_label"
        )
        val settingsUninstallButtonIdHit = snapshot.containsViewIdAny(
            "uninstall_button"
        )
        val settingsAppInfoIdHit = snapshot.containsViewIdAny(
            "entity_header",
            "entity_header_title",
            "floating_bottom_navigation"
        )
        val accessibilityServiceNameHit = snapshot.containsTextOrDescriptionAny(
            "unreel shortform blocker",
            "shortform blocker"
        )
        val accessibilityDetailTextHit = snapshot.containsTextOrDescriptionAny(
            "accessibility button",
            "about unreel shortform blocker",
            "detects shortform surfaces"
        )
        val accessibilitySwitchbarIdHit = snapshot.containsViewIdAny(
            "sesl_switchbar_container",
            "sesl_switchbar_switch"
        )
        val settingsActionBarIdHit = snapshot.containsViewIdAny(
            "com.android.settings:id/action_bar"
        )
        val playStoreDetailsHit = snapshot.containsViewIdAny(
            "details", "hero", "title", "uninstall"
        ) || snapshot.containsTextOrDescriptionAny("open")

        if (isPackageInstaller) reasons += "Package installer is active"
        if (isSettingsAppInfoHost) reasons += "Settings app-info host is active"
        if (isPlayStore) reasons += "Google Play app-details host is active"
        if (nameHit) reasons += "Unreel app marker is visible"
        if (packageHit) reasons += "Unreel package marker is visible"
        if (uninstallTextHit) reasons += "Uninstall action text is visible"
        if (appInfoHit) reasons += "App info surface markers are visible"
        if (uninstallConfirmationHit) reasons += "Uninstall confirmation text is visible"
        if (cancelActionHit) reasons += "Cancel action is visible"
        if (packageInstallerConfirmIdHit) reasons += "Package installer confirmation ids are visible"
        if (settingsUninstallButtonIdHit) reasons += "Settings uninstall button id is visible"
        if (settingsAppInfoIdHit) reasons += "Settings app-info ids are visible"
        if (accessibilityServiceNameHit) reasons += "Unreel accessibility service name is visible"
        if (accessibilityDetailTextHit) reasons += "Accessibility service detail text is visible"
        if (accessibilitySwitchbarIdHit) reasons += "Accessibility service switchbar ids are visible"
        if (settingsActionBarIdHit) reasons += "Settings action bar id is visible"
        if (playStoreDetailsHit) reasons += "Google Play app-details markers are visible"

        val packageInstallerUninstallConfirmation = isPackageInstaller &&
            appMarkerHit &&
            uninstallTextHit &&
            uninstallConfirmationHit &&
            cancelActionHit &&
            packageInstallerConfirmIdHit

        val unreelSettingsAppInfoUninstallPage = isSettingsAppInfoHost &&
            appMarkerHit &&
            appInfoHit &&
            uninstallTextHit &&
            settingsUninstallButtonIdHit &&
            settingsAppInfoIdHit

        val unreelAccessibilityServicePermissionPage = isSettingsAppInfoHost &&
            accessibilityServiceNameHit &&
            appInfoHit &&
            accessibilitySwitchbarIdHit &&
            settingsActionBarIdHit &&
            (accessibilityDetailTextHit || snapshot.containsTextOrDescriptionAny("instagram reels"))

        val unreelPlayStoreUninstallSurface = isPlayStore &&
            appMarkerHit &&
            uninstallTextHit &&
            playStoreDetailsHit &&
            (cancelActionHit || snapshot.containsTextOrDescriptionAny("open"))

        val confidence = when {
            packageInstallerUninstallConfirmation -> 0.99f
            unreelSettingsAppInfoUninstallPage -> 0.97f
            unreelAccessibilityServicePermissionPage -> 0.98f
            unreelPlayStoreUninstallSurface -> 0.98f
            else -> 0f
        }

        if (confidence < blockingThreshold) {
            return DetectionResult(
                platform = Platform.SETTINGS,
                surface = ContentSurface.SETTINGS_SAFE,
                confidence = confidence,
                packageName = snapshot.packageName,
                reasons = if (reasons.isEmpty()) {
                    listOf("No Unreel uninstall markers are visible")
                } else {
                    reasons
                },
                recommendedAction = InterventionAction.NONE
            )
        }

        val surface = if (unreelAccessibilityServicePermissionPage) {
            ContentSurface.SETTINGS_ACCESSIBILITY_BLOCKER
        } else {
            ContentSurface.SETTINGS_UNINSTALL_UNREEL
        }

        return DetectionResult(
            platform = Platform.SETTINGS,
            surface = surface,
            confidence = confidence,
            packageName = snapshot.packageName,
            reasons = reasons,
            recommendedAction = InterventionAction.BLOCK_AND_RETURN
        )
    }

    private fun unknown(snapshot: WindowSnapshot, reason: String): DetectionResult {
        return DetectionResult(
            platform = Platform.UNKNOWN,
            surface = ContentSurface.UNKNOWN,
            confidence = 0f,
            packageName = snapshot.packageName,
            reasons = listOf(reason),
            recommendedAction = InterventionAction.NONE
        )
    }
}
