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
            if (snapshot.looksLikeNativeShortsPlayer()) {
                return DetectionResult(
                    platform = Platform.YOUTUBE,
                    surface = ContentSurface.YOUTUBE_SHORTS_APP,
                    confidence = 0.96f,
                    packageName = YouTubePackageConfig.PACKAGE_NAME,
                    reasons = listOf(
                        "Native YouTube Shorts player markers are visible",
                        "Vertical video action controls are visible"
                    ),
                    recommendedAction = InterventionAction.BLOCK
                )
            }
            return DetectionResult(
                platform = Platform.YOUTUBE,
                surface = ContentSurface.YOUTUBE_APP,
                confidence = 1f,
                packageName = YouTubePackageConfig.PACKAGE_NAME,
                reasons = listOf("Native YouTube app is active"),
                recommendedAction = InterventionAction.BLOCK_AND_RETURN
            )
        }

        if (!snapshot.hasSupportedBrowserPackage() && !snapshot.hasBrowserChromeSignal()) {
            return unknown(snapshot, "Package is neither YouTube nor a supported browser")
        }

        val activeBrowserInput = snapshot.nodeFeatures.any { it.isActiveBrowserTextInput() }
        val visibleSuggestionUi = snapshot.nodeFeatures.any { it.looksLikeBrowserSuggestionUi() }
        if (activeBrowserInput || visibleSuggestionUi) {
            return unknown(snapshot, "Browser address entry or suggestions are active")
        }

        if (snapshot.looksLikeEmbeddedWebShortsPlayer()) {
            return DetectionResult(
                platform = Platform.YOUTUBE_WEB,
                surface = ContentSurface.YOUTUBE_SHORTS_WEB,
                confidence = 0.99f,
                packageName = snapshot.packageName,
                reasons = listOf(
                    "Embedded YouTube Shorts player structure is visible",
                    "Shorts carousel and video controls are visible"
                ),
                recommendedAction = InterventionAction.BLOCK
            )
        }

        val displayHeight = snapshot.nodeFeatures.maxOfOrNull { it.boundsBottom } ?: 0
        val loadedShortsAddress = snapshot.nodeFeatures.firstNotNullOfOrNull { feature ->
            feature.combinedText().takeIf {
                !feature.isFocused &&
                    it.containsYouTubeShortsUrl() &&
                    (feature.isBrowserAddressInput() ||
                        (snapshot.hasBrowserChromeSignal() &&
                            displayHeight > 0 &&
                            feature.boundsTop < displayHeight * 0.34f))
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

    private fun WindowSnapshot.looksLikeNativeShortsPlayer(): Boolean {
        val displayWidth = nodeFeatures.maxOfOrNull { it.boundsRight } ?: return false
        val displayHeight = nodeFeatures.maxOfOrNull { it.boundsBottom } ?: return false
        if (displayWidth <= 0 || displayHeight <= 0) return false

        val visibleNodes = nodeFeatures.filter { it.isVisibleToUser }
        val hasShortsPlayerMarker = visibleNodes.any { feature ->
            val combined = feature.combinedText()
            NATIVE_SHORTS_PLAYER_MARKERS.any(combined::contains)
        }
        val hasShortsLabel = visibleNodes.any { feature ->
            feature.text?.trim()?.equals("shorts", ignoreCase = true) == true ||
                feature.contentDescription?.trim()?.equals("shorts", ignoreCase = true) == true
        }
        val rightSideActions = visibleNodes.mapNotNull { feature ->
            val combined = feature.combinedText()
            val marker = NATIVE_SHORTS_ACTION_MARKERS.firstOrNull(combined::contains)
                ?: return@mapNotNull null
            marker to feature
        }.filter { (_, feature) ->
            feature.boundsLeft > displayWidth * 0.52f &&
                feature.boundsTop > displayHeight * 0.15f
        }
        val distinctActionCount = rightSideActions.map { it.first }.distinct().size
        val actionVerticalSpan = if (rightSideActions.size >= 2) {
            val centers = rightSideActions.map { (_, feature) ->
                (feature.boundsTop + feature.boundsBottom) / 2
            }
            centers.maxOrNull()!! - centers.minOrNull()!!
        } else {
            0
        }
        val hasLargePlayerContainer = visibleNodes.any { feature ->
            val width = feature.boundsRight - feature.boundsLeft
            val height = feature.boundsBottom - feature.boundsTop
            width > displayWidth * 0.68f &&
                height > displayHeight * 0.48f
        }

        val explicitPlayerSignature = hasShortsPlayerMarker && distinctActionCount >= 2
        val structuralPlayerSignature = hasShortsLabel &&
            distinctActionCount >= 3 &&
            actionVerticalSpan > displayHeight * 0.16f &&
            hasLargePlayerContainer
        return explicitPlayerSignature || structuralPlayerSignature
    }

    private fun WindowSnapshot.hasSupportedBrowserPackage(): Boolean {
        return listOf(packageName, rootPackageName, eventPackageName)
            .any(BrowserPackageConfig::isSupported)
    }

    private fun WindowSnapshot.looksLikeEmbeddedWebShortsPlayer(): Boolean {
        val normalizedIds = nodeFeatures
            .asSequence()
            .filter { it.isVisibleToUser }
            .mapNotNull { it.viewId?.lowercase() }
            .toSet()
        val strongMarkerCount = EMBEDDED_SHORTS_ID_MARKERS.count { marker ->
            normalizedIds.any { id -> id.contains(marker) }
        }
        val hasVideoNavigation = nodeFeatures.any { feature ->
            val description = feature.contentDescription.orEmpty().lowercase()
            description == "next video" || description == "previous video"
        }
        return strongMarkerCount >= 2 ||
            (strongMarkerCount >= 1 && hasVideoNavigation)
    }

    private fun WindowSnapshot.hasBrowserChromeSignal(): Boolean {
        return nodeFeatures.any { feature ->
            val id = feature.viewId.orEmpty().lowercase()
            val clazz = feature.className.orEmpty().lowercase()
            id.contains("custom_tab") ||
                id.contains("webview") ||
                clazz.contains("webview") ||
                id.contains("toolbar")
        }
    }

    private fun WindowNodeFeature.isBrowserAddressInput(): Boolean {
        val normalizedId = viewId.orEmpty().lowercase()
        val normalizedClass = className.orEmpty().lowercase()
        return BROWSER_ADDRESS_MARKERS.any(normalizedId::contains) ||
            isEditable ||
            normalizedClass.contains("edittext")
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
            "search_terms",
            "toolbar",
            "custom_tab_url"
        )

        val BROWSER_SUGGESTION_MARKERS = listOf(
            "autocomplete",
            "omnibox_suggestion",
            "search suggestion",
            "search suggestions",
            "url_suggestion"
        )

        val NATIVE_SHORTS_PLAYER_MARKERS = listOf(
            "shorts_player",
            "shorts_video",
            "shorts_viewer",
            "shorts_container",
            "reels_player",
            "reel_watch",
            "reel_player",
            "reel_recycler",
            "reel_pager",
            "reel_video"
        )

        val NATIVE_SHORTS_ACTION_MARKERS = listOf(
            "dislike",
            "like",
            "comment",
            "share",
            "remix",
            "subscribe"
        )

        val EMBEDDED_SHORTS_ID_MARKERS = listOf(
            "player-shorts-container",
            "shorts-video",
            "shorts-moveable-container",
            "carousel-scrollable-wrapper",
            "carousel-items",
            "carousel-item-"
        )
    }
}
