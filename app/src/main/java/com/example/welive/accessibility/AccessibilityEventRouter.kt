package com.example.welive.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.welive.analytics.ProtectionEvent
import com.example.welive.detection.ContentDetector
import com.example.welive.detection.ContentSurface
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.InterventionAction
import com.example.welive.detection.Platform
import com.example.welive.detection.WindowSnapshot
import com.example.welive.detection.platforms.instagram.InstagramHomeFeedClassifier
import com.example.welive.detection.platforms.instagram.InstagramHomeFeedClassification
import com.example.welive.detection.platforms.instagram.InstagramHomeFeedState
import com.example.welive.detection.platforms.instagram.InstagramHomeFeedRegionResolver
import com.example.welive.detection.platforms.instagram.InstagramPackageConfig
import com.example.welive.detection.platforms.settings.SettingsPackageConfig
import com.example.welive.detection.platforms.instagramweb.BrowserPackageConfig
import com.example.welive.detection.platforms.youtube.YouTubePackageConfig
import com.example.welive.diagnostics.DetectionDiagnostics
import com.example.welive.diagnostics.HomeDebugRecorder
import com.example.welive.intervention.HomeFeedAudioController
import com.example.welive.intervention.HomeFeedOverlayController
import com.example.welive.intervention.InstagramWebOverlayController
import com.example.welive.intervention.OverlayController
import com.example.welive.intervention.ScreenRegion
import com.example.welive.intervention.YouTubeShortsOverlayController
import com.example.welive.intervention.TikTokOverlayController
import com.example.welive.intervention.TikTokAudioController
import com.example.welive.settings.AppSettings
import com.example.welive.training.TrainingCaptureState
import com.example.welive.protection.ProtectedApp

class AccessibilityEventRouter(
    private val snapshotReader: WindowSnapshotReader,
    private val detectors: List<ContentDetector>,
    private val overlayController: OverlayController,
    private val homeFeedAudioController: HomeFeedAudioController,
    private val homeFeedOverlayController: HomeFeedOverlayController,
    private val instagramWebOverlayController: InstagramWebOverlayController,
    private val youTubeShortsOverlayController: YouTubeShortsOverlayController,
    private val tikTokOverlayController: TikTokOverlayController,
    private val tikTokAudioController: TikTokAudioController,
    private val homeFeedClassifier: InstagramHomeFeedClassifier,
    private val homeFeedRegionResolver: InstagramHomeFeedRegionResolver,
    private val performBack: () -> Unit,
    private val performHome: () -> Unit,
    private val onAllowOneMinute: () -> Unit,
    private val onOpenSettings: () -> Unit,
    private val onProtectionEvent: (ProtectionEvent) -> Unit,
    private val onShortFormExposure: (Long) -> Unit,
    private val appPackageName: String
) {
    private var lastProcessedAt = 0L
    private var lastBackAt = 0L
    private var lastReelsBlockAt = 0L
    private var nonReelsObservedSince = 0L
    private var consecutiveNonReels = 0
    private var backIssuedForCurrentBlock = false
    private var reverseCooldownUntil = 0L
    private var reverseArmed = true
    private var lastInstagramNavigationAt = 0L
    private var lastHomeTabNavigationAt = 0L
    private var lastHomeFeedSeenAt = 0L
    private var homeFeedSuppressedUntil = 0L
    private var lastInstagramWebBlockAt = 0L
    private var lastYouTubeWebBlockAt = 0L
    private var lastYouTubeAppReturnAt = 0L
    private var lastYouTubeShortsBackAt = 0L
    private var youTubeShortsBackIssued = false
    private var lastExternalEntryAt = 0L
    private var lastBrowserWebsiteAt = 0L
    private var lastBrowserWebsiteSurface: ContentSurface? = null
    private var activeShortFormSurface: ContentSurface? = null
    private var shortFormExposureStartedAt = 0L
    private var cachedHomeFeedRegion: ScreenRegion? = null
    private var cachedHomeFeedBlockerRegion: ScreenRegion? = null
    private var cachedHomeFeedStoryTapTargets: List<ScreenRegion> = emptyList()
    private var lastTotalAppReturnAt = 0L
    private var lastTotalWebsiteBackAt = 0L
    private var lastTotalWebsiteSurface: ContentSurface? = null
    private var settingsGuardActive = false

    fun dismissAllNow() {
        clearAllBlockers()
    }

    fun suppressHomeFeedOverlayFor(durationMillis: Long) {
        HomeDebugRecorder.recordExternal(
            decision = "suppress_home_overlay_external",
            extras = mapOf("durationMillis" to durationMillis)
        )
        homeFeedSuppressedUntil = System.currentTimeMillis() + durationMillis
        lastHomeFeedSeenAt = 0L
        clearHomeFeedBlock()
    }

    fun onEvent(
        event: AccessibilityEvent,
        rootProvider: () -> AccessibilityNodeInfo?,
        settings: AppSettings,
        useEventNavigationHints: Boolean = true
    ) {
        val packageName = event.packageName?.toString().orEmpty()
        val now = System.currentTimeMillis()
        val root = rootProvider()
        val rootPackageName = root?.packageName?.toString().orEmpty()
        val instagramIsActive = packageName == InstagramPackageConfig.PACKAGE_NAME ||
            rootPackageName == InstagramPackageConfig.PACKAGE_NAME
        val youtubeIsActive = YouTubePackageConfig.isYouTubeApp(packageName) ||
            YouTubePackageConfig.isYouTubeApp(rootPackageName)
        val browserIsActive = BrowserPackageConfig.isSupported(packageName) ||
            BrowserPackageConfig.isSupported(rootPackageName)
        val settingsIsActive = SettingsPackageConfig.isSupported(packageName) ||
            SettingsPackageConfig.isSupported(rootPackageName)
        val protectedApp = ProtectedApp.fromPackage(rootPackageName)
            ?: ProtectedApp.fromPackage(packageName)
        val protectedNativeIsActive = protectedApp != null
        val externalEntrySource = !browserIsActive &&
            !youtubeIsActive &&
            !settingsIsActive &&
            packageName != appPackageName &&
            rootPackageName != appPackageName &&
            packageName != SYSTEM_UI_PACKAGE &&
            rootPackageName != SYSTEM_UI_PACKAGE
        if (externalEntrySource) {
            lastExternalEntryAt = now
        }
        recordHomeDebug(
            phase = "event",
            decision = "received",
            event = event,
            rootPackageName = rootPackageName,
            extras = mapOf(
                "instagramIsActive" to instagramIsActive,
                "browserIsActive" to browserIsActive,
                "settingsIsActive" to settingsIsActive,
                "packageName" to packageName
            )
        )

        if (
            protectedApp == ProtectedApp.TIKTOK &&
            tikTokOverlayController.isShowing &&
            event.isTikTokMessageNavigationEvent()
        ) {
            tikTokOverlayController.dismissImmediately()
            tikTokAudioController.restore()
            return
        }

        if (settingsIsActive) {
            recordLatestSettingsSnapshot(event, root)
        }

        if (packageName == SYSTEM_UI_PACKAGE || rootPackageName == SYSTEM_UI_PACKAGE) {
            recordHomeDebug("event", "ignore_system_ui", event, rootPackageName)
            return
        }

        if (!instagramIsActive && !youtubeIsActive && !browserIsActive && !settingsIsActive && !protectedNativeIsActive) {
            if (packageName == appPackageName) {
                if (event.className?.toString()?.contains("MainActivity") == true) {
                    recordHomeDebug("event", "clear_blockers_app_activity", event, rootPackageName)
                    clearAllBlockers()
                    return
                }
                if (
                    instagramWebOverlayController.isShowing &&
                    now - lastInstagramWebBlockAt < WEB_OVERLAY_SELF_EVENT_GRACE_MS
                ) {
                    recordHomeDebug("event", "hold_web_overlay_app_event", event, rootPackageName)
                    instagramWebOverlayController.holdSolid()
                }
                if (
                    youTubeShortsOverlayController.isShowing &&
                    now - lastYouTubeWebBlockAt < WEB_OVERLAY_SELF_EVENT_GRACE_MS
                ) {
                    youTubeShortsOverlayController.holdSolid()
                }
                recordHomeDebug("event", "ignore_app_overlay_event", event, rootPackageName)
                return
            }
            recordHomeDebug("event", "clear_all_non_target", event, rootPackageName)
            clearAllBlockers()
            return
        }

        if (protectedApp != null && settings.isTotalAppBlocked(protectedApp)) {
            recordHomeDebug("decision", "total_app_block_${protectedApp.name.lowercase()}", event, rootPackageName)
            clearAllBlockers()
            if (now - lastTotalAppReturnAt >= TOTAL_ACCESS_RETURN_COOLDOWN_MS) {
                lastTotalAppReturnAt = now
                performHome()
            }
            return
        }

        if (!browserIsActive) {
            clearInstagramWebBlock()
            clearYouTubeWebBlock()
        }

        if (useEventNavigationHints && instagramIsActive && event.isInstagramNavigationEvent()) {
            lastInstagramNavigationAt = now
        }
        val homeFeedEnterNavigationEvent = useEventNavigationHints &&
            instagramIsActive &&
            event.isHomeFeedEnterNavigationEvent()
        if (homeFeedEnterNavigationEvent) {
            lastHomeTabNavigationAt = now
            recordHomeDebug("fast_event", "clear_home_suppression_home_tab_event", event, rootPackageName)
            homeFeedSuppressedUntil = 0L
            if (
                settings.blockInstagramHomeFeed &&
                !settings.isTemporarilyAllowed(now) &&
                cachedHomeFeedRegion != null
            ) {
                recordHomeDebug("fast_event", "show_cached_home_block_home_tab_event", event, rootPackageName)
                showCachedHomeFeedBlock(settings.blockInstagramHomeStories, now)
                return
            }
        }
        if (
            useEventNavigationHints &&
            instagramIsActive &&
            homeFeedOverlayController.isShowing &&
            event.isHomeStoryOpenEvent()
        ) {
            recordHomeDebug("fast_event", "clear_home_story_open_event", event, rootPackageName)
            homeFeedSuppressedUntil = now + HOME_FEED_STORY_OPEN_SUPPRESSION_MS
            clearHomeFeedBlock()
            return
        }
        if (
            useEventNavigationHints &&
            instagramIsActive &&
            settings.blockInstagramHomeFeed &&
            event.isHomeFeedExitNavigationEvent() &&
            now - lastHomeTabNavigationAt > HOME_FEED_RETURN_GUARD_WINDOW_MS
        ) {
            recordHomeDebug("fast_event", "suppress_home_exit_navigation_event", event, rootPackageName)
            homeFeedSuppressedUntil = now + HOME_FEED_EXIT_NAVIGATION_SUPPRESSION_MS
            clearHomeFeedBlock()
            return
        }
        val minProcessInterval = if (
            homeFeedOverlayController.isShowing ||
            settings.blockInstagramHomeFeed
        ) {
            HOME_FEED_PROCESS_INTERVAL_MS
        } else {
            DEFAULT_PROCESS_INTERVAL_MS
        }
        if (now - lastProcessedAt < minProcessInterval) {
            recordHomeDebug(
                phase = "throttle",
                decision = "throttled",
                event = event,
                rootPackageName = rootPackageName,
                extras = mapOf(
                    "elapsedSinceLastProcess" to (now - lastProcessedAt),
                    "minProcessInterval" to minProcessInterval
                )
            )
            return
        }
        lastProcessedAt = now

        if (instagramIsActive && settings.isTemporarilyAllowed(now)) {
            recordHomeDebug("settings", "clear_all_temporarily_allowed", event, rootPackageName)
            clearAllBlockers()
            return
        }

        val snapshot = snapshotReader.read(event, root) ?: run {
            recordHomeDebug("snapshot", "snapshot_missing", event, rootPackageName)
            return
        }
        if (snapshot.rootPackageName == InstagramPackageConfig.PACKAGE_NAME) {
            TrainingCaptureState.recordInstagramSnapshot(snapshot)
        }
        if (snapshot.isSettingsSnapshot()) {
            TrainingCaptureState.recordSettingsSnapshot(snapshot)
        }
        val result = detectors
            .asSequence()
            .map { detector -> detector.detect(snapshot) }
            .firstOrNull { it.platform != Platform.UNKNOWN }
            ?: run {
                if (
                    browserIsActive &&
                    instagramWebOverlayController.isShowing &&
                    snapshot.rootPackageName == appPackageName
                ) {
                    recordHomeDebug(
                        phase = "detector",
                        decision = "hold_web_overlay_no_detector_match",
                        event = event,
                        rootPackageName = rootPackageName,
                        snapshot = snapshot
                    )
                    instagramWebOverlayController.holdSolid()
                    return
                }
                if (
                    browserIsActive &&
                    youTubeShortsOverlayController.isShowing &&
                    snapshot.rootPackageName == appPackageName
                ) {
                    youTubeShortsOverlayController.holdSolid()
                    return
                }
                if (browserIsActive) {
                    recordHomeDebug(
                        phase = "detector",
                        decision = "clear_all_browser_no_detector_match",
                        event = event,
                        rootPackageName = rootPackageName,
                        snapshot = snapshot
                    )
                    clearAllBlockers()
                }
                if (protectedNativeIsActive) {
                    if (protectedApp == ProtectedApp.TIKTOK && tikTokOverlayController.isShowing) {
                        tikTokOverlayController.showOrHold(snapshot = null)
                        tikTokAudioController.enforceMute()
                        return
                    }
                    clearAllBlockers()
                }
                recordHomeDebug(
                    phase = "detector",
                    decision = "no_detector_match",
                    event = event,
                    rootPackageName = rootPackageName,
                    snapshot = snapshot
                )
                return
            }
        val homeFeedClassification = homeFeedClassifier.classify(snapshot)

        DetectionDiagnostics.record(result)
        updateShortFormExposure(result.surface, now)

        val isInstagramReelsSurface = result.surface == ContentSurface.INSTAGRAM_REELS ||
            result.surface == ContentSurface.INSTAGRAM_REELS_FROM_FRIEND
        val isSearchGridSurface = result.surface == ContentSurface.INSTAGRAM_SEARCH_REELS_GRID
        val isInstagramWebSurface = result.surface == ContentSurface.INSTAGRAM_WEB
        val isYouTubeAppSurface = result.surface == ContentSurface.YOUTUBE_APP
        val isYouTubeShortsAppSurface = result.surface == ContentSurface.YOUTUBE_SHORTS_APP
        val isYouTubeShortsWebSurface = result.surface == ContentSurface.YOUTUBE_SHORTS_WEB
        val enteredYouTubeFromExternal =
            now - lastExternalEntryAt <= YOUTUBE_FRIEND_ENTRY_WINDOW_MS ||
                (result.surface == ContentSurface.YOUTUBE_SHORTS_WEB &&
                    lastBrowserWebsiteSurface != null &&
                    now - lastBrowserWebsiteAt <= YOUTUBE_FRIEND_ENTRY_WINDOW_MS)
        val allowFriendYouTubeShorts = isYouTubeShortsWebSurface &&
            settings.allowYouTubeFriendShorts &&
            enteredYouTubeFromExternal
        val isUninstallSurface = result.surface == ContentSurface.SETTINGS_UNINSTALL_UNREEL
        val isAccessibilityBlockerSurface = result.surface == ContentSurface.SETTINGS_ACCESSIBILITY_BLOCKER
        val isSettingsGuardSurface = isUninstallSurface || isAccessibilityBlockerSurface
        val protectedWebsiteApp = result.surface.protectedWebsiteApp()
        val protectedWebsiteShouldBlock = protectedWebsiteApp != null &&
            settings.isTotalWebsiteBlocked(protectedWebsiteApp)
        val tikTokShortFormShouldBlock = result.surface == ContentSurface.TIKTOK_SHORTFORM &&
            settings.blockTikTokShortForm
        val friendReelAllowed = result.surface == ContentSurface.INSTAGRAM_REELS_FROM_FRIEND &&
            settings.allowInstagramReelsFromFriends
        val reelsShouldBlock = result.recommendedAction == InterventionAction.BLOCK_AND_RETURN &&
            isInstagramReelsSurface &&
            settings.blockInstagramReels &&
            !friendReelAllowed
        val searchGridShouldBlock = result.recommendedAction == InterventionAction.BLOCK &&
            isSearchGridSurface &&
            settings.blockInstagramSearchGrid
        val webShouldBlock = result.recommendedAction == InterventionAction.BLOCK &&
            isInstagramWebSurface &&
            settings.blockInstagramWebsite &&
            !settings.isTemporarilyAllowed(now)
        val youtubeAppShouldBlock = result.recommendedAction == InterventionAction.BLOCK_AND_RETURN &&
            isYouTubeAppSurface &&
            settings.blockYouTubeApp
        val youtubeShortsWebShouldBlock = result.recommendedAction == InterventionAction.BLOCK &&
            isYouTubeShortsWebSurface &&
            settings.blockYouTubeShortsWebsite &&
            !allowFriendYouTubeShorts
        val youtubeShortsAppShouldBlock = result.recommendedAction == InterventionAction.BLOCK &&
            isYouTubeShortsAppSurface &&
            settings.blockYouTubeShortsInApp
        val settingsGuardShouldBlock = result.recommendedAction == InterventionAction.BLOCK_AND_RETURN &&
            isSettingsGuardSurface &&
            settings.protectAppUninstall
        val nativeShouldBlock = reelsShouldBlock || searchGridShouldBlock
        val shouldBlock = nativeShouldBlock ||
            webShouldBlock ||
            youtubeAppShouldBlock ||
            youtubeShortsWebShouldBlock ||
            youtubeShortsAppShouldBlock ||
            settingsGuardShouldBlock ||
            protectedWebsiteShouldBlock ||
            tikTokShortFormShouldBlock
        val homeFeedDetected = homeFeedClassification.state == InstagramHomeFeedState.HOME_FEED
        val clearNonHomeInstagramSurface = instagramIsActive && (
            homeFeedClassification.state == InstagramHomeFeedState.FOLLOWING_TAB ||
                homeFeedClassification.state == InstagramHomeFeedState.OTHER_SURFACE ||
                result.surface in HOME_FEED_CLEAR_SURFACES
            )
        val homeFeedShouldBlock = instagramIsActive &&
            settings.blockInstagramHomeFeed &&
            homeFeedDetected &&
            !clearNonHomeInstagramSurface
        recordHomeDebug(
            phase = "classification",
            decision = "classified",
            event = event,
            rootPackageName = rootPackageName,
            snapshot = snapshot,
            detectorResult = result,
            homeFeedClassification = homeFeedClassification,
            extras = mapOf(
                "homeFeedDetected" to homeFeedDetected,
                "homeFeedShouldBlock" to homeFeedShouldBlock,
                "clearNonHomeInstagramSurface" to clearNonHomeInstagramSurface,
                "reelsShouldBlock" to reelsShouldBlock,
                "searchGridShouldBlock" to searchGridShouldBlock,
                "webShouldBlock" to webShouldBlock,
                "settingsGuardShouldBlock" to settingsGuardShouldBlock,
                "allowFriendYouTubeShorts" to allowFriendYouTubeShorts,
                "enteredYouTubeFromExternal" to enteredYouTubeFromExternal,
                "nativeShouldBlock" to nativeShouldBlock,
                "homeFeedSuppressedUntilDelta" to (homeFeedSuppressedUntil - now),
                "lastHomeFeedSeenAge" to if (lastHomeFeedSeenAt == 0L) -1L else now - lastHomeFeedSeenAt
            )
        )

        val blockTitle = when {
            isAccessibilityBlockerSurface -> "Permission Change Blocked"
            isUninstallSurface -> "Uninstall Blocked"
            isYouTubeAppSurface -> "YouTube App Blocked"
            isYouTubeShortsAppSurface -> "YouTube Shorts Blocked"
            isYouTubeShortsWebSurface -> "YouTube Shorts Blocked"
            isInstagramWebSurface -> "Instagram Website Blocked"
            isSearchGridSurface -> "Search Grid Blocked"
            else -> "Reels Blocked"
        }
        val blockBody = when {
            isAccessibilityBlockerSurface -> "Unreel protected its accessibility permission."
            isUninstallSurface -> "Unreel protected itself from being removed."
            isYouTubeAppSurface -> "Use youtube.com for intentional viewing."
            isYouTubeShortsAppSurface -> "The YouTube app remains available for everything else."
            isYouTubeShortsWebSurface -> "Long-form YouTube remains available."
            isInstagramWebSurface -> "Instagram web is blocked."
            isSearchGridSurface -> "You chose to keep this search space clear."
            else -> "You chose to keep this space clear."
        }

        if (youtubeAppShouldBlock) {
            clearInstagramWebBlock()
            clearYouTubeWebBlock()
            clearHomeFeedBlock()
            if (now - lastYouTubeAppReturnAt > YOUTUBE_APP_RETURN_COOLDOWN_MS) {
                lastYouTubeAppReturnAt = now
                onProtectionEvent(ProtectionEvent.YOUTUBE_APP)
                overlayController.pulseBlocked(
                    onCovered = performHome,
                    title = blockTitle,
                    body = blockBody
                )
            } else {
                overlayController.holdSolid()
            }
            return
        }

        if (settingsGuardShouldBlock) {
            recordHomeDebug(
                phase = "decision",
                decision = if (isAccessibilityBlockerSurface) {
                    "block_accessibility_permission_surface"
                } else {
                    "block_uninstall_surface"
                },
                event = event,
                rootPackageName = rootPackageName,
                snapshot = snapshot,
                detectorResult = result,
                homeFeedClassification = homeFeedClassification
            )
            clearInstagramWebBlock()
            clearYouTubeWebBlock()
            clearHomeFeedBlock()
            if (!settingsGuardActive) {
                settingsGuardActive = true
                overlayController.pulseBlocked(
                    onCovered = { performBack() },
                    title = blockTitle,
                    body = blockBody
                )
            }
            return
        }

        if (settingsIsActive && settingsGuardActive) {
            settingsGuardActive = false
            overlayController.dismissImmediately()
        }

        if (tikTokOverlayController.isShowing && !tikTokShortFormShouldBlock) {
            if (result.platform == Platform.TIKTOK && result.surface == ContentSurface.UNKNOWN) {
                tikTokOverlayController.showOrHold(snapshot = snapshot)
                return
            }
            tikTokOverlayController.dismissImmediately()
            tikTokAudioController.restore()
            clearFullScreenBlockState(immediateOverlayDismiss = true)
        }

        if (tikTokShortFormShouldBlock) {
            clearHomeFeedBlock()
            clearInstagramWebBlock()
            clearYouTubeWebBlock()
            tikTokOverlayController.showOrHold(snapshot = snapshot)
            tikTokAudioController.enforceMute()
            return
        }

        if (protectedWebsiteShouldBlock) {
            val surfaceChanged = lastTotalWebsiteSurface != result.surface
            clearHomeFeedBlock()
            clearInstagramWebBlock()
            clearYouTubeWebBlock()
            clearFullScreenBlockState(immediateOverlayDismiss = true)
            if (surfaceChanged || now - lastTotalWebsiteBackAt >= TOTAL_ACCESS_RETURN_COOLDOWN_MS) {
                lastTotalWebsiteSurface = result.surface
                lastTotalWebsiteBackAt = now
                performBack()
            }
            return
        } else if (result.surface != lastTotalWebsiteSurface) {
            lastTotalWebsiteSurface = null
        }

        if ((reelsShouldBlock || searchGridShouldBlock) && settings.reverseFromReel) {
            recordHomeDebug(
                phase = "decision",
                decision = "reverse_from_blocked_surface",
                event = event,
                rootPackageName = rootPackageName,
                snapshot = snapshot,
                detectorResult = result,
                homeFeedClassification = homeFeedClassification
            )
            clearHomeFeedBlock()
            val reversed = handleReverseFromBlockedSurface(
                now = now,
                afterBack = {
                    if (
                        settings.blockInstagramHomeFeed &&
                        (searchGridShouldBlock || result.surface == ContentSurface.INSTAGRAM_REELS)
                    ) {
                        showCachedHomeFeedBlock(settings.blockInstagramHomeStories, now)
                    }
                }
            )
            if (reversed) {
                onProtectionEvent(
                    if (searchGridShouldBlock) {
                        ProtectionEvent.INSTAGRAM_SEARCH_GRID
                    } else {
                        ProtectionEvent.INSTAGRAM_REEL
                    }
                )
            }
            return
        }

        if (!shouldBlock) {
            reverseArmed = true
            if (isYouTubeShortsWebSurface) {
                clearYouTubeWebBlock()
                if (allowFriendYouTubeShorts) {
                    lastExternalEntryAt = 0L
                    lastBrowserWebsiteAt = 0L
                    lastBrowserWebsiteSurface = null
                    recordHomeDebug(
                        phase = "decision",
                        decision = "allow_first_youtube_friend_short",
                        event = event,
                        rootPackageName = rootPackageName,
                        snapshot = snapshot,
                        detectorResult = result,
                        homeFeedClassification = homeFeedClassification
                    )
                }
                return
            }
        }

        if (browserIsActive && result.surface.isBrowserWebsiteSurface()) {
            lastBrowserWebsiteAt = now
            lastBrowserWebsiteSurface = result.surface
        }

        if (webShouldBlock) {
            recordHomeDebug(
                phase = "decision",
                decision = "show_web_block",
                event = event,
                rootPackageName = rootPackageName,
                snapshot = snapshot,
                detectorResult = result,
                homeFeedClassification = homeFeedClassification
            )
            clearHomeFeedBlock()
            clearYouTubeWebBlock()
            clearFullScreenBlockState()
            lastInstagramWebBlockAt = now
            instagramWebOverlayController.showOrUpdate(
                snapshot = snapshot,
                onOpenSettings = onOpenSettings
            )
            instagramWebOverlayController.holdSolid()
            return
        }

        if (youtubeShortsWebShouldBlock) {
            clearHomeFeedBlock()
            clearInstagramWebBlock()
            clearFullScreenBlockState()
            lastYouTubeWebBlockAt = now
            youTubeShortsOverlayController.showOrUpdate(
                snapshot = snapshot,
                onOpenSettings = onOpenSettings
            )
            youTubeShortsOverlayController.holdSolid()
            if (
                !youTubeShortsBackIssued &&
                now - lastYouTubeShortsBackAt > YOUTUBE_SHORTS_BACK_COOLDOWN_MS
            ) {
                youTubeShortsBackIssued = true
                lastYouTubeShortsBackAt = now
                onProtectionEvent(ProtectionEvent.YOUTUBE_SHORT)
                overlayController.runAfterOverlayEntrance {
                    performBack()
                }
            }
            return
        }

        if (youtubeShortsAppShouldBlock) {
            clearHomeFeedBlock()
            clearInstagramWebBlock()
            clearFullScreenBlockState()
            youTubeShortsOverlayController.showNativeOrUpdate(
                snapshot = snapshot,
                onOpenSettings = onOpenSettings
            )
            youTubeShortsOverlayController.holdSolid()
            return
        }

        if (nativeShouldBlock) {
            recordHomeDebug(
                phase = "decision",
                decision = "show_native_fullscreen_block",
                event = event,
                rootPackageName = rootPackageName,
                snapshot = snapshot,
                detectorResult = result,
                homeFeedClassification = homeFeedClassification
            )
            clearInstagramWebBlock()
            clearYouTubeWebBlock()
            clearHomeFeedBlock()
            nonReelsObservedSince = 0L
            consecutiveNonReels = 0
            lastReelsBlockAt = now
            val newlyShown = overlayController.showBlocked(
                onAllowOneMinute = onAllowOneMinute,
                onOpenSettings = onOpenSettings,
                title = blockTitle,
                body = blockBody
            )
            if (newlyShown) {
                onProtectionEvent(
                    if (searchGridShouldBlock) {
                        ProtectionEvent.INSTAGRAM_SEARCH_GRID
                    } else {
                        ProtectionEvent.INSTAGRAM_REEL
                    }
                )
            }
            overlayController.holdSolid()
            if (
                (reelsShouldBlock || searchGridShouldBlock) &&
                settings.reverseFromReel &&
                newlyShown &&
                !backIssuedForCurrentBlock &&
                now - lastBackAt > 1_250L
            ) {
                backIssuedForCurrentBlock = true
                lastBackAt = now
                overlayController.runAfterOverlayEntrance {
                    performBack()
                }
            }
            return
        }

        if (now < homeFeedSuppressedUntil && !homeFeedEnterNavigationEvent) {
            recordHomeDebug(
                phase = "decision",
                decision = "clear_home_suppressed",
                event = event,
                rootPackageName = rootPackageName,
                snapshot = snapshot,
                detectorResult = result,
                homeFeedClassification = homeFeedClassification,
                extras = mapOf("suppressedForMillis" to (homeFeedSuppressedUntil - now))
            )
            clearHomeFeedBlock()
            return
        }

        if (homeFeedShouldBlock) {
            recordHomeDebug(
                phase = "decision",
                decision = "show_home_feed_block",
                event = event,
                rootPackageName = rootPackageName,
                snapshot = snapshot,
                detectorResult = result,
                homeFeedClassification = homeFeedClassification
            )
            homeFeedSuppressedUntil = 0L
            lastHomeFeedSeenAt = now
            clearFullScreenBlockState(immediateOverlayDismiss = true)
            homeFeedAudioController.muteHomeFeed()
            showHomeFeedBlock(snapshot, settings.blockInstagramHomeStories)
            homeFeedOverlayController.holdSolid()
            return
        }

        if (homeFeedOverlayController.isShowing && !settings.blockInstagramHomeFeed) {
            recordHomeDebug(
                phase = "decision",
                decision = "clear_home_setting_disabled",
                event = event,
                rootPackageName = rootPackageName,
                snapshot = snapshot,
                detectorResult = result,
                homeFeedClassification = homeFeedClassification
            )
            clearHomeFeedBlock()
        } else if (homeFeedOverlayController.isShowing) {
            when (homeFeedClassification.state) {
                InstagramHomeFeedState.HOME_FEED -> {
                    recordHomeDebug(
                        phase = "decision",
                        decision = "hold_home_confirmed",
                        event = event,
                        rootPackageName = rootPackageName,
                        snapshot = snapshot,
                        detectorResult = result,
                        homeFeedClassification = homeFeedClassification
                    )
                    lastHomeFeedSeenAt = now
                    homeFeedAudioController.muteHomeFeed()
                    showHomeFeedBlock(snapshot, settings.blockInstagramHomeStories)
                    homeFeedOverlayController.holdSolid()
                    return
                }
                InstagramHomeFeedState.UNKNOWN -> {
                    val graceMs = if (now - lastInstagramNavigationAt < HOME_FEED_NAVIGATION_CLEAR_WINDOW_MS) {
                        HOME_FEED_NAVIGATION_GRACE_MS
                    } else {
                        HOME_FEED_GRACE_MS
                    }
                    if (now - lastHomeFeedSeenAt < graceMs) {
                        recordHomeDebug(
                            phase = "decision",
                            decision = "hold_home_unknown_grace",
                            event = event,
                            rootPackageName = rootPackageName,
                            snapshot = snapshot,
                            detectorResult = result,
                            homeFeedClassification = homeFeedClassification,
                            extras = mapOf(
                                "graceMillis" to graceMs,
                                "lastHomeFeedSeenAge" to (now - lastHomeFeedSeenAt)
                            )
                        )
                        showHomeFeedBlock(snapshot, settings.blockInstagramHomeStories)
                        homeFeedOverlayController.holdSolid()
                        return
                    }
                    recordHomeDebug(
                        phase = "decision",
                        decision = "clear_home_unknown_timeout",
                        event = event,
                        rootPackageName = rootPackageName,
                        snapshot = snapshot,
                        detectorResult = result,
                        homeFeedClassification = homeFeedClassification,
                        extras = mapOf("graceMillis" to graceMs)
                    )
                    clearHomeFeedBlock()
                }
                InstagramHomeFeedState.FOLLOWING_TAB,
                InstagramHomeFeedState.OTHER_SURFACE -> {
                    recordHomeDebug(
                        phase = "decision",
                        decision = "clear_home_non_home_classification",
                        event = event,
                        rootPackageName = rootPackageName,
                        snapshot = snapshot,
                        detectorResult = result,
                        homeFeedClassification = homeFeedClassification
                    )
                    clearHomeFeedBlock()
                }
            }
        }

        if (overlayController.isShowing) {
            val confirmedClearSurface = result.surface != ContentSurface.UNKNOWN &&
                !shouldBlock

            if (confirmedClearSurface) {
                consecutiveNonReels += 1
                if (nonReelsObservedSince == 0L) {
                    nonReelsObservedSince = now
                }
            } else {
                consecutiveNonReels = 0
                nonReelsObservedSince = 0L
                overlayController.holdSolid()
            }

            val heldLongEnough = now - lastReelsBlockAt > 900L
            val stableNonReels = nonReelsObservedSince > 0L &&
                now - nonReelsObservedSince > 900L &&
                consecutiveNonReels >= 3
            if (heldLongEnough && stableNonReels) {
                clearFullScreenBlockState()
            } else {
                overlayController.holdSolid()
            }
        }
    }

    private fun clearAllBlockers() {
        endShortFormExposure(System.currentTimeMillis())
        clearHomeFeedBlock()
        clearInstagramWebBlock()
        clearYouTubeWebBlock()
        homeFeedSuppressedUntil = 0L
        settingsGuardActive = false
        tikTokOverlayController.dismissImmediately()
        tikTokAudioController.restore()
        clearFullScreenBlockState(immediateOverlayDismiss = true)
    }

    private fun clearHomeFeedBlock() {
        homeFeedOverlayController.dismiss()
        homeFeedAudioController.restore()
        lastHomeFeedSeenAt = 0L
    }

    private fun clearInstagramWebBlock() {
        instagramWebOverlayController.dismiss()
        lastInstagramWebBlockAt = 0L
    }

    private fun clearYouTubeWebBlock() {
        youTubeShortsOverlayController.dismiss()
        lastYouTubeWebBlockAt = 0L
        youTubeShortsBackIssued = false
    }

    private fun ContentSurface.isBrowserWebsiteSurface(): Boolean {
        return this == ContentSurface.INSTAGRAM_WEB ||
            this == ContentSurface.PROTECTED_WEBSITE_YOUTUBE ||
            this == ContentSurface.PROTECTED_WEBSITE_TIKTOK ||
            this == ContentSurface.PROTECTED_WEBSITE_SNAPCHAT ||
            this == ContentSurface.PROTECTED_WEBSITE_X ||
            this == ContentSurface.PROTECTED_WEBSITE_THREADS ||
            this == ContentSurface.PROTECTED_WEBSITE_REDDIT ||
            this == ContentSurface.PROTECTED_WEBSITE_LINKEDIN
    }

    private fun recordHomeDebug(
        phase: String,
        decision: String,
        event: AccessibilityEvent,
        rootPackageName: String,
        snapshot: WindowSnapshot? = null,
        detectorResult: DetectionResult? = null,
        homeFeedClassification: InstagramHomeFeedClassification? = null,
        extras: Map<String, Any?> = emptyMap()
    ) {
        HomeDebugRecorder.recordRouter(
            phase = phase,
            decision = decision,
            event = event,
            rootPackageName = rootPackageName,
            snapshot = snapshot,
            detectorResult = detectorResult,
            homeFeedClassification = homeFeedClassification,
            overlayShowing = overlayController.isShowing,
            homeFeedOverlayShowing = homeFeedOverlayController.isShowing,
            webOverlayShowing = instagramWebOverlayController.isShowing ||
                youTubeShortsOverlayController.isShowing,
            extras = extras
        )
    }

    private fun recordLatestSettingsSnapshot(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?
    ) {
        val snapshot = snapshotReader.read(event, root) ?: return
        if (snapshot.isSettingsSnapshot()) {
            TrainingCaptureState.recordSettingsSnapshot(snapshot)
        }
    }

    private fun WindowSnapshot.isSettingsSnapshot(): Boolean {
        return SettingsPackageConfig.isSupported(packageName) ||
            SettingsPackageConfig.isSupported(rootPackageName) ||
            SettingsPackageConfig.isSupported(eventPackageName)
    }

    private fun showHomeFeedBlock(
        snapshot: WindowSnapshot,
        blockStories: Boolean
    ) {
        if (cachedHomeFeedRegion != null) {
            showCachedHomeFeedBlock(blockStories, System.currentTimeMillis())
            return
        }

        val freshRegion = homeFeedRegionResolver.resolve(snapshot)
        val freshBlockerRegion = freshRegion?.let { region ->
            homeFeedRegionResolver.resolveBlockerRegion(snapshot) ?: region
        }
        val freshStoryTapTargets = freshRegion?.let {
            homeFeedRegionResolver.resolveStoryTapTargets(snapshot)
        }

        if (freshRegion != null && freshBlockerRegion != null) {
            cachedHomeFeedRegion = freshRegion
            cachedHomeFeedBlockerRegion = freshBlockerRegion
            cachedHomeFeedStoryTapTargets = freshStoryTapTargets.orEmpty()
        }

        val region = freshRegion ?: cachedHomeFeedRegion ?: return
        val blockerRegion = freshBlockerRegion ?: cachedHomeFeedBlockerRegion ?: region
        val storyTapTargets = freshStoryTapTargets ?: cachedHomeFeedStoryTapTargets
        homeFeedOverlayController.showOrUpdate(
            region = region,
            blockerRegion = blockerRegion,
            storyTapTargets = storyTapTargets,
            blockStories = blockStories
        )
    }

    private fun showCachedHomeFeedBlock(
        blockStories: Boolean,
        now: Long
    ) {
        val region = cachedHomeFeedRegion ?: return
        val blockerRegion = cachedHomeFeedBlockerRegion ?: region
        clearFullScreenBlockState(immediateOverlayDismiss = true)
        homeFeedSuppressedUntil = 0L
        lastHomeFeedSeenAt = now
        homeFeedAudioController.muteHomeFeed()
        homeFeedOverlayController.showOrUpdate(
            region = region,
            blockerRegion = blockerRegion,
            storyTapTargets = cachedHomeFeedStoryTapTargets,
            blockStories = blockStories
        )
        homeFeedOverlayController.holdSolid()
    }

    private fun handleReverseFromBlockedSurface(
        now: Long,
        afterBack: () -> Unit = {}
    ): Boolean {
        val userNavigatedAfterBack = lastInstagramNavigationAt > lastBackAt + 250L
        if (!reverseArmed && userNavigatedAfterBack) {
            reverseArmed = true
        }

        if (!reverseArmed) {
            return false
        }

        if (now < reverseCooldownUntil && !userNavigatedAfterBack) {
            return false
        }

        reverseCooldownUntil = now + 500L
        reverseArmed = false
        clearFullScreenBlockState(immediateOverlayDismiss = true)
        reverseArmed = false
        lastBackAt = now
        performBack()
        afterBack()
        return true
    }

    private fun ContentSurface.protectedWebsiteApp(): ProtectedApp? = when (this) {
        ContentSurface.PROTECTED_WEBSITE_YOUTUBE -> ProtectedApp.YOUTUBE
        ContentSurface.PROTECTED_WEBSITE_TIKTOK -> ProtectedApp.TIKTOK
        ContentSurface.PROTECTED_WEBSITE_SNAPCHAT -> ProtectedApp.SNAPCHAT
        ContentSurface.PROTECTED_WEBSITE_X -> ProtectedApp.X
        ContentSurface.PROTECTED_WEBSITE_THREADS -> ProtectedApp.THREADS
        ContentSurface.PROTECTED_WEBSITE_REDDIT -> ProtectedApp.REDDIT
        ContentSurface.PROTECTED_WEBSITE_LINKEDIN -> ProtectedApp.LINKEDIN
        else -> null
    }

    private fun updateShortFormExposure(surface: ContentSurface, now: Long) {
        val trackedSurface = surface.takeIf {
            it == ContentSurface.INSTAGRAM_REELS ||
                it == ContentSurface.INSTAGRAM_REELS_FROM_FRIEND ||
                it == ContentSurface.INSTAGRAM_SEARCH_REELS_GRID ||
                it == ContentSurface.YOUTUBE_SHORTS_WEB
        }
        if (trackedSurface == activeShortFormSurface) return
        endShortFormExposure(now)
        if (trackedSurface != null) {
            activeShortFormSurface = trackedSurface
            shortFormExposureStartedAt = now
        }
    }

    private fun endShortFormExposure(now: Long) {
        val startedAt = shortFormExposureStartedAt
        if (activeShortFormSurface != null && startedAt > 0L && now > startedAt) {
            onShortFormExposure(now - startedAt)
        }
        activeShortFormSurface = null
        shortFormExposureStartedAt = 0L
    }

    private fun clearFullScreenBlockState(
        dismissOverlay: Boolean = true,
        immediateOverlayDismiss: Boolean = false
    ) {
        if (dismissOverlay) {
            if (immediateOverlayDismiss) {
                overlayController.dismissImmediately()
            } else {
                overlayController.dismiss()
            }
        }
        nonReelsObservedSince = 0L
        lastReelsBlockAt = 0L
        consecutiveNonReels = 0
        backIssuedForCurrentBlock = false
        reverseArmed = true
    }

    private fun AccessibilityEvent.isInstagramNavigationEvent(): Boolean {
        return when (eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED,
            AccessibilityEvent.TYPE_VIEW_SELECTED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_TOUCH_INTERACTION_START -> true
            else -> false
        }
    }

    private fun AccessibilityEvent.isHomeFeedExitNavigationEvent(): Boolean {
        if (
            eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SELECTED
        ) {
            return false
        }

        val sourceNode = runCatching { source }.getOrNull()
        val labels = buildList {
            text.forEach { value -> add(value.toString().lowercase()) }
            contentDescription?.toString()?.lowercase()?.let(::add)
            sourceNode?.text?.toString()?.lowercase()?.let(::add)
            sourceNode?.contentDescription?.toString()?.lowercase()?.let(::add)
        }
        val viewId = sourceNode?.viewIdResourceName.orEmpty().lowercase().substringAfterLast('/')
        val idMatch = HOME_FEED_EXIT_NAVIGATION_ID_MARKERS.any { marker -> viewId.contains(marker) }
        val labelMatch = HOME_FEED_EXIT_NAVIGATION_LABEL_MARKERS.any { marker ->
            labels.any { label -> label == marker || label.contains(marker) }
        }

        return when (eventType) {
            AccessibilityEvent.TYPE_VIEW_CLICKED -> idMatch || labelMatch
            AccessibilityEvent.TYPE_VIEW_SELECTED -> idMatch
            else -> false
        }
    }

    private fun AccessibilityEvent.isTikTokMessageNavigationEvent(): Boolean {
        if (
            eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SELECTED
        ) return false

        val sourceNode = runCatching { source }.getOrNull()
        val combined = listOfNotNull(
            sourceNode?.viewIdResourceName,
            sourceNode?.text?.toString(),
            sourceNode?.contentDescription?.toString(),
            text.joinToString(" "),
            contentDescription?.toString()
        ).joinToString(" ").lowercase()
        return TIKTOK_MESSAGE_NAVIGATION_MARKERS.any(combined::contains)
    }

    private fun AccessibilityEvent.isHomeFeedEnterNavigationEvent(): Boolean {
        if (
            eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            eventType != AccessibilityEvent.TYPE_VIEW_SELECTED
        ) {
            return false
        }

        val sourceNode = runCatching { source }.getOrNull()
        val viewId = sourceNode?.viewIdResourceName.orEmpty().lowercase().substringAfterLast('/')
        return viewId.contains("feed_tab")
    }

    private fun AccessibilityEvent.isHomeStoryOpenEvent(): Boolean {
        if (
            eventType != AccessibilityEvent.TYPE_VIEW_CLICKED &&
            eventType != AccessibilityEvent.TYPE_TOUCH_INTERACTION_START
        ) {
            return false
        }

        val sourceNode = runCatching { source }.getOrNull()
        val labels = buildList {
            text.forEach { value -> add(value.toString().lowercase()) }
            contentDescription?.toString()?.lowercase()?.let(::add)
            sourceNode?.text?.toString()?.lowercase()?.let(::add)
            sourceNode?.contentDescription?.toString()?.lowercase()?.let(::add)
        }
        val viewId = sourceNode?.viewIdResourceName.orEmpty().lowercase().substringAfterLast('/')

        return HOME_FEED_STORY_OPEN_ID_MARKERS.any { marker -> viewId.contains(marker) } ||
            HOME_FEED_STORY_OPEN_LABEL_MARKERS.any { marker ->
                labels.any { label -> label == marker || label.contains(marker) }
            }
    }

    private companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        const val DEFAULT_PROCESS_INTERVAL_MS = 70L
        const val HOME_FEED_PROCESS_INTERVAL_MS = 35L
        const val HOME_FEED_GRACE_MS = 650L
        const val HOME_FEED_NAVIGATION_CLEAR_WINDOW_MS = 1_000L
        const val HOME_FEED_NAVIGATION_GRACE_MS = 120L
        const val HOME_FEED_EXIT_NAVIGATION_SUPPRESSION_MS = 450L
        const val HOME_FEED_RETURN_GUARD_WINDOW_MS = 900L
        const val HOME_FEED_STORY_OPEN_SUPPRESSION_MS = 1_500L
        const val WEB_OVERLAY_SELF_EVENT_GRACE_MS = 2_500L
        const val YOUTUBE_APP_RETURN_COOLDOWN_MS = 750L
        const val YOUTUBE_SHORTS_BACK_COOLDOWN_MS = 750L
        const val YOUTUBE_FRIEND_ENTRY_WINDOW_MS = 10_000L
        const val TOTAL_ACCESS_RETURN_COOLDOWN_MS = 650L
        val HOME_FEED_EXIT_NAVIGATION_ID_MARKERS = setOf(
            "action_bar_button_direct",
            "action_bar_inbox",
            "clips_tab",
            "direct_tab",
            "inbox",
            "profile_tab",
            "search_tab"
        )
        val HOME_FEED_EXIT_NAVIGATION_LABEL_MARKERS = setOf(
            "direct",
            "explore",
            "following",
            "messages",
            "messenger",
            "posts",
            "profile",
            "reels",
            "search"
        )
        val HOME_FEED_STORY_OPEN_ID_MARKERS = setOf(
            "avatar_image_view",
            "outer_container",
            "reel_empty_badge",
            "story"
        )
        val HOME_FEED_STORY_OPEN_LABEL_MARKERS = setOf(
            "open story",
            "story"
        )
        val HOME_FEED_CLEAR_SURFACES = setOf(
            ContentSurface.INSTAGRAM_REELS,
            ContentSurface.INSTAGRAM_REELS_FROM_FRIEND,
            ContentSurface.INSTAGRAM_SEARCH_REELS_GRID,
            ContentSurface.INSTAGRAM_STORIES,
            ContentSurface.INSTAGRAM_EXPLORE,
            ContentSurface.INSTAGRAM_PROFILE,
            ContentSurface.INSTAGRAM_DMS
        )
        val TIKTOK_MESSAGE_NAVIGATION_MARKERS = setOf(
            "inbox", "messages_tab", "message_tab", "chat_tab", "dm_tab", "friends_tab", "friends"
        )
    }
}
