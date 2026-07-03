package com.example.welive.accessibility

import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
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
import com.example.welive.detection.platforms.instagramweb.BrowserPackageConfig
import com.example.welive.diagnostics.DetectionDiagnostics
import com.example.welive.diagnostics.HomeDebugRecorder
import com.example.welive.intervention.HomeFeedAudioController
import com.example.welive.intervention.HomeFeedOverlayController
import com.example.welive.intervention.InstagramWebOverlayController
import com.example.welive.intervention.OverlayController
import com.example.welive.intervention.ScreenRegion
import com.example.welive.settings.AppSettings
import com.example.welive.training.TrainingCaptureState

class AccessibilityEventRouter(
    private val snapshotReader: WindowSnapshotReader,
    private val detectors: List<ContentDetector>,
    private val overlayController: OverlayController,
    private val homeFeedAudioController: HomeFeedAudioController,
    private val homeFeedOverlayController: HomeFeedOverlayController,
    private val instagramWebOverlayController: InstagramWebOverlayController,
    private val homeFeedClassifier: InstagramHomeFeedClassifier,
    private val homeFeedRegionResolver: InstagramHomeFeedRegionResolver,
    private val performBack: () -> Unit,
    private val onAllowOneMinute: () -> Unit,
    private val onOpenSettings: () -> Unit,
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
    private var cachedHomeFeedRegion: ScreenRegion? = null
    private var cachedHomeFeedBlockerRegion: ScreenRegion? = null
    private var cachedHomeFeedStoryTapTargets: List<ScreenRegion> = emptyList()
    private var homeFeedBlockNeedsFreshGeometry = false

    fun suppressHomeFeedOverlayFor(durationMillis: Long) {
        HomeDebugRecorder.recordExternal(
            decision = "suppress_home_overlay_external",
            extras = mapOf("durationMillis" to durationMillis)
        )
        homeFeedSuppressedUntil = System.currentTimeMillis() + durationMillis
        lastHomeFeedSeenAt = 0L
        clearHomeFeedBlock()
    }

    fun showProvisionalHomeFeedBlock(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?,
        blockStories: Boolean
    ) {
        val now = System.currentTimeMillis()
        val snapshot = snapshotReader.read(event, root)
        HomeDebugRecorder.recordExternal(
            decision = "preload_home_block_on_instagram_open",
            extras = mapOf(
                "blockStories" to blockStories,
                "hasSnapshot" to (snapshot != null),
                "hasCachedRegion" to (cachedHomeFeedRegion != null)
            )
        )
        clearInstagramWebBlock()
        clearFullScreenBlockState(immediateOverlayDismiss = true)
        homeFeedSuppressedUntil = 0L
        lastHomeFeedSeenAt = now
        homeFeedBlockNeedsFreshGeometry = true
        homeFeedAudioController.muteHomeFeed()

        if (snapshot != null) {
            showHomeFeedBlock(snapshot, blockStories)
            if (homeFeedOverlayController.isShowing) {
                homeFeedOverlayController.holdSolid()
                return
            }
        }

        showCachedHomeFeedBlock(blockStories, now)
    }

    fun onEvent(
        event: AccessibilityEvent,
        rootProvider: () -> AccessibilityNodeInfo?,
        settings: AppSettings
    ) {
        val packageName = event.packageName?.toString().orEmpty()
        val now = System.currentTimeMillis()
        val root = rootProvider()
        val rootPackageName = root?.packageName?.toString().orEmpty()
        val instagramIsActive = packageName == InstagramPackageConfig.PACKAGE_NAME ||
            rootPackageName == InstagramPackageConfig.PACKAGE_NAME
        val browserIsActive = BrowserPackageConfig.isSupported(packageName) ||
            BrowserPackageConfig.isSupported(rootPackageName)
        recordHomeDebug(
            phase = "event",
            decision = "received",
            event = event,
            rootPackageName = rootPackageName,
            extras = mapOf(
                "instagramIsActive" to instagramIsActive,
                "browserIsActive" to browserIsActive,
                "packageName" to packageName
            )
        )

        if (packageName == SYSTEM_UI_PACKAGE || rootPackageName == SYSTEM_UI_PACKAGE) {
            recordHomeDebug("event", "ignore_system_ui", event, rootPackageName)
            return
        }

        if (!instagramIsActive && !browserIsActive) {
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
                recordHomeDebug("event", "ignore_app_overlay_event", event, rootPackageName)
                return
            }
            recordHomeDebug("event", "clear_all_non_target", event, rootPackageName)
            clearAllBlockers()
            return
        }

        if (event.isInstagramNavigationEvent()) {
            lastInstagramNavigationAt = now
        }
        val homeFeedEnterNavigationEvent = instagramIsActive && event.isHomeFeedEnterNavigationEvent()
        if (homeFeedEnterNavigationEvent) {
            lastHomeTabNavigationAt = now
            recordHomeDebug("fast_event", "clear_home_suppression_home_tab_event", event, rootPackageName)
            homeFeedSuppressedUntil = 0L
        }
        if (
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

        if (settings.isTemporarilyAllowed(now)) {
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

        val isInstagramReelsSurface = result.surface == ContentSurface.INSTAGRAM_REELS ||
            result.surface == ContentSurface.INSTAGRAM_REELS_FROM_FRIEND
        val isSearchGridSurface = result.surface == ContentSurface.INSTAGRAM_SEARCH_REELS_GRID
        val isInstagramWebSurface = result.surface == ContentSurface.INSTAGRAM_WEB
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
            settings.blockInstagramWebsite
        val nativeShouldBlock = reelsShouldBlock || searchGridShouldBlock
        val shouldBlock = nativeShouldBlock || webShouldBlock
        val homeFeedDetected = homeFeedClassification.state == InstagramHomeFeedState.HOME_FEED ||
            (
                homeFeedClassification.state == InstagramHomeFeedState.UNKNOWN &&
                    result.surface == ContentSurface.INSTAGRAM_HOME_FEED
                )
        val clearNonHomeInstagramSurface = instagramIsActive && (
            homeFeedClassification.state == InstagramHomeFeedState.FOLLOWING_TAB ||
                homeFeedClassification.state == InstagramHomeFeedState.OTHER_SURFACE ||
                result.surface in HOME_FEED_CLEAR_SURFACES
            )
        val homeFeedShouldBlock = instagramIsActive &&
            settings.blockInstagramHomeFeed &&
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
                "nativeShouldBlock" to nativeShouldBlock,
                "homeFeedSuppressedUntilDelta" to (homeFeedSuppressedUntil - now),
                "lastHomeFeedSeenAge" to if (lastHomeFeedSeenAt == 0L) -1L else now - lastHomeFeedSeenAt
            )
        )

        val blockTitle = when {
            isInstagramWebSurface -> "Instagram Website Blocked"
            isSearchGridSurface -> "Search Grid Blocked"
            else -> "Reels Blocked"
        }
        val blockBody = when {
            isInstagramWebSurface -> "Instagram web is blocked."
            isSearchGridSurface -> "You chose to keep this search space clear."
            else -> "You chose to keep this space clear."
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
            handleReverseFromBlockedSurface(
                now = now,
                pulseBlockScreen = settings.pulseBlockScreenOnReverse,
                blockTitle = blockTitle,
                blockBody = blockBody,
                afterBack = {
                    if (
                        settings.blockInstagramHomeFeed &&
                        (searchGridShouldBlock || result.surface == ContentSurface.INSTAGRAM_REELS)
                    ) {
                        showCachedHomeFeedBlock(settings.blockInstagramHomeStories, now)
                    }
                }
            )
            return
        }

        if (!shouldBlock) {
            reverseArmed = true
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
            clearFullScreenBlockState()
            lastInstagramWebBlockAt = now
            instagramWebOverlayController.showOrUpdate(
                snapshot = snapshot,
                onAllowOneMinute = onAllowOneMinute,
                onOpenSettings = onOpenSettings
            )
            instagramWebOverlayController.holdSolid()
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
                decision = if (homeFeedDetected) {
                    "show_home_feed_block"
                } else {
                    "show_guarded_default_home_block"
                },
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
        clearHomeFeedBlock()
        clearInstagramWebBlock()
        homeFeedSuppressedUntil = 0L
        clearFullScreenBlockState()
    }

    private fun clearHomeFeedBlock() {
        homeFeedOverlayController.dismiss()
        homeFeedAudioController.restore()
        lastHomeFeedSeenAt = 0L
        homeFeedBlockNeedsFreshGeometry = false
    }

    private fun clearInstagramWebBlock() {
        instagramWebOverlayController.dismiss()
        lastInstagramWebBlockAt = 0L
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
            webOverlayShowing = instagramWebOverlayController.isShowing,
            extras = extras
        )
    }

    private fun showHomeFeedBlock(
        snapshot: WindowSnapshot,
        blockStories: Boolean
    ) {
        if (
            homeFeedOverlayController.isShowing &&
            cachedHomeFeedRegion != null &&
            !homeFeedBlockNeedsFreshGeometry
        ) {
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
            homeFeedBlockNeedsFreshGeometry = false
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
        pulseBlockScreen: Boolean,
        blockTitle: String,
        blockBody: String,
        afterBack: () -> Unit = {}
    ) {
        val userNavigatedAfterBack = lastInstagramNavigationAt > lastBackAt + 250L
        if (!reverseArmed && userNavigatedAfterBack) {
            reverseArmed = true
        }

        if (!reverseArmed) {
            return
        }

        if (now < reverseCooldownUntil && !userNavigatedAfterBack) {
            return
        }

        reverseCooldownUntil = now + 500L
        reverseArmed = false
        clearFullScreenBlockState(dismissOverlay = !pulseBlockScreen)
        reverseArmed = false
        lastBackAt = now
        if (pulseBlockScreen) {
            overlayController.pulseBlocked(
                onCovered = {
                    performBack()
                    afterBack()
                },
                title = blockTitle,
                body = blockBody
            )
        } else {
            performBack()
            afterBack()
        }
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
    }
}
