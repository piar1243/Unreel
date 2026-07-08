package com.example.welive.detection.platforms.youtube

import com.example.welive.detection.ContentDetector
import com.example.welive.detection.ContentSurface
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.Platform
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import com.example.welive.detection.platforms.instagramweb.BrowserPackageConfig

class YouTubeDetector : ContentDetector {
    override fun detect(snapshot: WindowSnapshot): DetectionResult {
        if (snapshot.hasYouTubeAppPackage()) {
            return DetectionResult(
                platform = Platform.YOUTUBE,
                surface = ContentSurface.YOUTUBE_APP,
                confidence = 1f,
                packageName = YouTubePackageConfig.PACKAGE_NAME,
                reasons = listOf("Native YouTube app is active"),
                recommendedAction = InterventionAction.BLOCK_AND_RETURN
            )
        }

        if (!snapshot.hasSupportedBrowserPackage()) {
            return unknown(snapshot, "Package is neither YouTube nor a supported browser")
        }

        val activeBrowserInput = snapshot.nodeFeatures.any { it.isActiveBrowserTextInput() }
        val visibleSuggestionUi = snapshot.nodeFeatures.any { it.looksLikeBrowserSuggestionUi() }
        if (activeBrowserInput || visibleSuggestionUi) {
            return unknown(snapshot, "Browser address entry or suggestions are active")
        }

        val loadedShortsAddress = snapshot.nodeFeatures.firstNotNullOfOrNull { feature ->
            feature.combinedText().takeIf {
                feature.isBrowserAddressInput() &&
                    !feature.isFocused &&
                    it.containsYouTubeShortsUrl()
            }
        } ?: return unknown(snapshot, "Loaded browser address is not a YouTube Shorts URL")

        return DetectionResult(
            platform = Platform.YOUTUBE_WEB,
            surface = ContentSurface.YOUTUBE_SHORTS_WEB,
            confidence = 0.99f,
            packageName = snapshot.packageName,
            reasons = listOf("Loaded browser address is YouTube Shorts: $loadedShortsAddress"),
            recommendedAction = InterventionAction.BLOCK
        )
    }

    private fun WindowSnapshot.hasYouTubeAppPackage(): Boolean {
        return listOf(packageName, rootPackageName, eventPackageName)
            .any(YouTubePackageConfig::isYouTubeApp)
    }

    private fun WindowSnapshot.hasSupportedBrowserPackage(): Boolean {
        return listOf(packageName, rootPackageName, eventPackageName)
            .any(BrowserPackageConfig::isSupported)
    }

    private fun WindowNodeFeature.isBrowserAddressInput(): Boolean {
        val normalizedId = viewId.orEmpty().lowercase()
        val normalizedClass = className.orEmpty().lowercase()
        return BROWSER_ADDRESS_MARKERS.any(normalizedId::contains) ||
            ((isEditable || normalizedClass.contains("edittext")) && isFocused)
    }

    private fun WindowNodeFeature.isActiveBrowserTextInput(): Boolean {
        if (!isFocused) return false
        val normalizedId = viewId.orEmpty().lowercase()
        val normalizedClass = className.orEmpty().lowercase()
        return BROWSER_ADDRESS_MARKERS.any(normalizedId::contains) ||
            isEditable ||
            normalizedClass.contains("edittext")
    }

    private fun WindowNodeFeature.looksLikeBrowserSuggestionUi(): Boolean {
        val combined = combinedText()
        return BROWSER_SUGGESTION_MARKERS.any(combined::contains)
    }

    private fun WindowNodeFeature.combinedText(): String {
        return listOfNotNull(text, contentDescription, viewId)
            .joinToString(separator = " ")
            .lowercase()
    }

    private fun String.containsYouTubeShortsUrl(): Boolean {
        return YOUTUBE_SHORTS_URL_MARKERS.any(::contains)
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

    private companion object {
        val YOUTUBE_SHORTS_URL_MARKERS = listOf(
            "youtube.com/shorts",
            "youtu.be/shorts"
        )

        val BROWSER_ADDRESS_MARKERS = listOf(
            "address_bar",
            "edit_text",
            "location",
            "url_bar",
            "location_bar",
            "omnibox",
            "search_box",
            "search_terms"
        )

        val BROWSER_SUGGESTION_MARKERS = listOf(
            "autocomplete",
            "omnibox_suggestion",
            "search suggestion",
            "search suggestions",
            "url_suggestion"
        )
    }
}
