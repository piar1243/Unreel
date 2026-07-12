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

        val protectedApps = ProtectedApp.entries.filterNot { it == ProtectedApp.INSTAGRAM }
        val match = snapshot.nodeFeatures
            .asSequence()
            .filter { it.isVisibleToUser && it.looksLikeLoadedAddress(snapshot) }
            .map { it.combinedText() }
            .mapNotNull { address ->
                protectedApps.firstOrNull { candidate ->
                    candidate.domains.any { domain -> address.containsDomain(domain) }
                }?.let { app -> app to address }
            }
            .firstOrNull()
            ?: return unknown(snapshot, "No loaded browser address is exposed")
        val app = match.first

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
        return ADDRESS_MARKERS.any(id::contains)
    }

    private fun WindowNodeFeature.looksLikeLoadedAddress(snapshot: WindowSnapshot): Boolean {
        if (isFocused) return false
        if (isBrowserAddressInput()) return true

        val displayHeight = snapshot.nodeFeatures.maxOfOrNull { it.boundsBottom } ?: return false
        val combined = combinedText()
        val isTopChrome = boundsTop <= displayHeight * 0.28f && boundsBottom <= displayHeight * 0.38f
        val hasUrlShape = combined.contains("http://") || combined.contains("https://")
        return isTopChrome && hasUrlShape && ProtectedApp.entries.any { app ->
            app != ProtectedApp.INSTAGRAM && app.domains.any { domain -> combined.containsDomain(domain) }
        }
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
