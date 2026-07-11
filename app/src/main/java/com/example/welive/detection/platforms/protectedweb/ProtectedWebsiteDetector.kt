package com.example.welive.detection.platforms.protectedweb

import com.example.welive.detection.ContentDetector
import com.example.welive.detection.ContentSurface
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.Platform
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import com.example.welive.detection.platforms.instagramweb.BrowserPackageConfig
import com.example.welive.protection.ProtectedApp

class ProtectedWebsiteDetector : ContentDetector {
    override fun detect(snapshot: WindowSnapshot): DetectionResult {
        val browserActive = listOf(snapshot.packageName, snapshot.rootPackageName, snapshot.eventPackageName)
            .any(BrowserPackageConfig::isSupported)
        if (!browserActive) return unknown(snapshot, "A supported browser is not active")
        if (snapshot.nodeFeatures.any { it.isActiveBrowserInput() }) {
            return unknown(snapshot, "Browser address entry is active")
        }

        val loadedAddress = snapshot.nodeFeatures.firstNotNullOfOrNull { feature ->
            feature.combinedText().takeIf {
                feature.isBrowserAddressInput() && !feature.isFocused
            }
        } ?: return unknown(snapshot, "No loaded browser address is exposed")

        val app = ProtectedApp.entries
            .filterNot { it == ProtectedApp.INSTAGRAM }
            .firstOrNull { candidate ->
                candidate.domains.any { domain -> loadedAddress.containsDomain(domain) }
            }
            ?: return unknown(snapshot, "Loaded address is not a protected website")

        return DetectionResult(
            platform = Platform.PROTECTED_WEB,
            surface = when (app) {
                ProtectedApp.YOUTUBE -> ContentSurface.PROTECTED_WEBSITE_YOUTUBE
                ProtectedApp.TIKTOK -> ContentSurface.PROTECTED_WEBSITE_TIKTOK
                ProtectedApp.SNAPCHAT -> ContentSurface.PROTECTED_WEBSITE_SNAPCHAT
                ProtectedApp.X -> ContentSurface.PROTECTED_WEBSITE_X
                ProtectedApp.THREADS -> ContentSurface.PROTECTED_WEBSITE_THREADS
                ProtectedApp.REDDIT -> ContentSurface.PROTECTED_WEBSITE_REDDIT
                ProtectedApp.LINKEDIN -> ContentSurface.PROTECTED_WEBSITE_LINKEDIN
                ProtectedApp.INSTAGRAM -> error("Instagram uses its dedicated detector")
            },
            confidence = 0.99f,
            packageName = snapshot.packageName,
            reasons = listOf("Loaded browser address belongs to ${app.displayName}"),
            recommendedAction = InterventionAction.BLOCK_AND_RETURN
        )
    }

    private fun WindowNodeFeature.isBrowserAddressInput(): Boolean {
        val id = viewId.orEmpty().lowercase()
        val clazz = className.orEmpty().lowercase()
        return ADDRESS_MARKERS.any(id::contains) || (isEditable && clazz.contains("edittext"))
    }

    private fun WindowNodeFeature.isActiveBrowserInput(): Boolean {
        return isFocused && isBrowserAddressInput()
    }

    private fun WindowNodeFeature.combinedText(): String {
        return listOfNotNull(text, contentDescription, viewId).joinToString(" ").lowercase()
    }

    private fun String.containsDomain(domain: String): Boolean {
        return Regex("(^|[/:.])${Regex.escape(domain)}([/:?#]|$)").containsMatchIn(this)
    }

    private fun unknown(snapshot: WindowSnapshot, reason: String) = DetectionResult(
        platform = Platform.UNKNOWN,
        surface = ContentSurface.UNKNOWN,
        confidence = 0f,
        packageName = snapshot.packageName,
        reasons = listOf(reason),
        recommendedAction = InterventionAction.NONE
    )

    private companion object {
        val ADDRESS_MARKERS = listOf(
            "address_bar", "url_bar", "location_bar", "omnibox", "search_terms", "toolbar"
        )
    }
}
