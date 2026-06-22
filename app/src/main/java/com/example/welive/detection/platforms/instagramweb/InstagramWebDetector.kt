package com.example.welive.detection.platforms.instagramweb

import com.example.welive.detection.ContentDetector
import com.example.welive.detection.ContentSurface
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.Platform
import com.example.welive.detection.WindowSnapshot

class InstagramWebDetector : ContentDetector {
    override fun detect(snapshot: WindowSnapshot): DetectionResult {
        if (
            !BrowserPackageConfig.isSupported(snapshot.packageName) &&
            !BrowserPackageConfig.isSupported(snapshot.eventPackageName) &&
            !BrowserPackageConfig.isSupported(snapshot.rootPackageName)
        ) {
            return unknown(snapshot, "Package is not a supported browser")
        }

        val reasons = mutableListOf<String>()
        val searchableText = snapshot.searchableText
        val activeBrowserInput = snapshot.nodeFeatures.any { feature ->
            feature.isActiveBrowserTextInput()
        }
        if (activeBrowserInput) {
            reasons += "Browser or page text input is active"
        }

        val loadedAddressUrlHit = snapshot.nodeFeatures
            .firstNotNullOfOrNull { feature ->
                feature
                    .combinedText()
                    .takeIf {
                        feature.isBrowserAddressInput() &&
                            !feature.isFocused &&
                            it.containsAny(INSTAGRAM_URL_MARKERS)
                    }
            }
        if (loadedAddressUrlHit != null) {
            reasons += "Loaded browser address is Instagram"
        }

        val visibleSuggestionUi = snapshot.nodeFeatures.any { feature ->
            feature.looksLikeBrowserSuggestionUi()
        }
        if (visibleSuggestionUi) {
            reasons += "Browser suggestions are visible"
        }

        val pageContentText = snapshot.pageContentTextExcludingAddressInput()
        val contentUrlHit = INSTAGRAM_URL_MARKERS.firstOrNull { pageContentText.contains(it) }
        if (contentUrlHit != null) {
            reasons += "Instagram URL marker is visible in page content: $contentUrlHit"
        }
        val pageMarkerHits = INSTAGRAM_SPECIFIC_PAGE_MARKERS
            .filter { pageContentText.contains(it) }
            .take(4)
        if (pageMarkerHits.isNotEmpty()) {
            reasons += "Instagram web page markers are visible: ${pageMarkerHits.joinToString()}"
        }

        val browserChromeHit = BROWSER_ADDRESS_MARKERS.any { marker ->
            snapshot.viewIds.any { viewId -> viewId.lowercase().contains(marker) }
        }
        if (browserChromeHit) {
            reasons += "Browser address/page controls are visible"
        }

        if (activeBrowserInput || visibleSuggestionUi) {
            return unknown(snapshot, reasons.joinToString())
        }

        val confidence = when {
            loadedAddressUrlHit != null && pageMarkerHits.isNotEmpty() -> 0.99f
            loadedAddressUrlHit != null -> 0.96f
            contentUrlHit != null && pageMarkerHits.isNotEmpty() -> 0.92f
            pageMarkerHits.size >= 2 -> 0.90f
            else -> 0f
        }

        if (confidence < BLOCKING_THRESHOLD) {
            return unknown(
                snapshot = snapshot,
                reason = if (reasons.isEmpty()) {
                    "No Instagram web markers are visible"
                } else {
                    reasons.joinToString()
                }
            )
        }

        return DetectionResult(
            platform = Platform.INSTAGRAM_WEB,
            surface = ContentSurface.INSTAGRAM_WEB,
            confidence = confidence,
            packageName = snapshot.packageName,
            reasons = reasons,
            recommendedAction = InterventionAction.BLOCK
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

    private fun WindowSnapshot.pageContentTextExcludingAddressInput(): String {
        val featureText = nodeFeatures
            .filterNot { it.isBrowserAddressInput() || it.looksLikeBrowserSuggestionUi() }
            .flatMap { feature ->
                listOfNotNull(
                    feature.text,
                    feature.contentDescription,
                    feature.viewId
                )
            }
            .joinToString(separator = " ")
            .lowercase()
        return featureText.ifBlank { searchableText }
    }

    private fun com.example.welive.detection.WindowNodeFeature.isBrowserAddressInput(): Boolean {
        val viewId = viewId.orEmpty().lowercase()
        val className = className.orEmpty().lowercase()
        val looksLikeAddressField = BROWSER_ADDRESS_MARKERS.any { marker -> viewId.contains(marker) }
        val looksEditable = isEditable || className.contains("edittext")
        return looksLikeAddressField || (looksEditable && isFocused)
    }

    private fun com.example.welive.detection.WindowNodeFeature.isActiveBrowserTextInput(): Boolean {
        if (!isFocused) return false
        val viewId = viewId.orEmpty().lowercase()
        val className = className.orEmpty().lowercase()
        val looksLikeAddressField = BROWSER_ADDRESS_MARKERS.any { marker -> viewId.contains(marker) }
        val looksEditable = isEditable || className.contains("edittext")
        return looksLikeAddressField || looksEditable
    }

    private fun com.example.welive.detection.WindowNodeFeature.looksLikeBrowserSuggestionUi(): Boolean {
        val combined = combinedText()
        return BROWSER_SUGGESTION_MARKERS.any { marker -> combined.contains(marker) }
    }

    private fun com.example.welive.detection.WindowNodeFeature.combinedText(): String {
        return listOfNotNull(text, contentDescription, viewId)
            .joinToString(separator = " ")
            .lowercase()
    }

    private fun String.containsAny(needles: List<String>): Boolean {
        return needles.any { contains(it) }
    }

    private companion object {
        const val BLOCKING_THRESHOLD = 0.86f

        val INSTAGRAM_URL_MARKERS = listOf(
            "instagram.com",
            "www.instagram.com",
            "m.instagram.com"
        )

        val INSTAGRAM_SPECIFIC_PAGE_MARKERS = listOf(
            "from meta",
            "log in to instagram",
            "sign up for instagram",
            "open instagram",
            "instagram logo",
            "meta verified",
            "threads from instagram"
        )

        val BROWSER_ADDRESS_MARKERS = listOf(
            "address_bar",
            "edit_text",
            "location",
            "url_bar",
            "location_bar",
            "omnibox",
            "search_box",
            "search_terms",
            "toolbar",
            "browser"
        )

        val BROWSER_SUGGESTION_MARKERS = listOf(
            "autocomplete",
            "omnibox_suggestion",
            "search suggestion",
            "search suggestions",
            "suggestion",
            "suggestions",
            "url_suggestion"
        )
    }
}
