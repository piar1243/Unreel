package com.example.welive.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.welive.MainActivity
import com.example.welive.diagnostics.HomeDebugRecorder
import com.example.welive.detection.platforms.instagram.InstagramDetector
import com.example.welive.detection.platforms.instagram.InstagramHomeFeedClassifier
import com.example.welive.detection.platforms.instagram.InstagramHomeFeedRegionResolver
import com.example.welive.detection.platforms.instagram.InstagramPackageConfig
import com.example.welive.detection.platforms.instagramweb.BrowserPackageConfig
import com.example.welive.detection.platforms.instagramweb.InstagramWebDetector
import com.example.welive.intervention.HomeFeedAudioController
import com.example.welive.intervention.HomeFeedOverlayController
import com.example.welive.intervention.InstagramWebOverlayController
import com.example.welive.intervention.OverlayController
import com.example.welive.intervention.SystemGrayscaleController
import com.example.welive.settings.AppSettings
import com.example.welive.settings.UserRulesRepository
import java.time.LocalDate
import java.time.ZonedDateTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WeLiveAccessibilityService : AccessibilityService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var settingsRepository: UserRulesRepository
    private lateinit var overlayController: OverlayController
    private lateinit var homeFeedAudioController: HomeFeedAudioController
    private lateinit var homeFeedOverlayController: HomeFeedOverlayController
    private lateinit var instagramWebOverlayController: InstagramWebOverlayController
    private lateinit var systemGrayscaleController: SystemGrayscaleController
    private lateinit var eventRouter: AccessibilityEventRouter
    private var currentSettings = AppSettings()
    private var nativeInstagramVisibleForGrayscale = false
    private var nativeInstagramSessionActiveForOpenLimit = false
    private var nativeInstagramSessionActiveForHomePreload = false
    private var lastOpenLimitReturnAt = 0L
    private var lastScheduleReturnAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        settingsRepository = UserRulesRepository(applicationContext)
        overlayController = OverlayController(this)
        homeFeedAudioController = HomeFeedAudioController(this)
        systemGrayscaleController = SystemGrayscaleController(applicationContext)
        homeFeedOverlayController = HomeFeedOverlayController(
            this,
            onStoryGestureTap = { x, y -> performStoryGestureTap(x, y) },
            onTransparentBandTapStarted = { isNavigationBand ->
                if (::eventRouter.isInitialized) {
                    val suppressionMillis = if (isNavigationBand) {
                        HOME_NAVIGATION_SUPPRESSION_MS
                    } else {
                        STORY_OPEN_SUPPRESSION_MS
                    }
                    eventRouter.suppressHomeFeedOverlayFor(suppressionMillis)
                }
            }
        )
        instagramWebOverlayController = InstagramWebOverlayController(this)
        val audioManager = getSystemService(AudioManager::class.java)
        eventRouter = AccessibilityEventRouter(
            snapshotReader = WindowSnapshotReader(
                musicActiveProvider = { audioManager?.isMusicActive == true }
            ),
            detectors = listOf(
                InstagramDetector(),
                InstagramWebDetector()
            ),
            overlayController = overlayController,
            homeFeedAudioController = homeFeedAudioController,
            homeFeedOverlayController = homeFeedOverlayController,
            instagramWebOverlayController = instagramWebOverlayController,
            homeFeedClassifier = InstagramHomeFeedClassifier(),
            homeFeedRegionResolver = InstagramHomeFeedRegionResolver(),
            performBack = { performGlobalAction(GLOBAL_ACTION_BACK) },
            onAllowOneMinute = {
                serviceScope.launch { settingsRepository.allowTemporarily(60_000L) }
            },
            onOpenSettings = { openAppSettings() },
            appPackageName = packageName
        )

        serviceScope.launch {
            settingsRepository.settings.collectLatest { settings ->
                currentSettings = settings
                if (!settings.grayscaleInstagramApp) {
                    systemGrayscaleController.restore()
                } else if (nativeInstagramVisibleForGrayscale) {
                    systemGrayscaleController.enable()
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || !::eventRouter.isInitialized) return
        val root = rootForEvent(event)
        if (enforceInstagramAccessSchedule(event, root)) {
            systemGrayscaleController.restore()
            return
        }
        if (enforceInstagramOpenLimit(event, root)) {
            systemGrayscaleController.restore()
            return
        }
        preloadHomeFeedBlockOnInstagramOpen(event, root)
        updateInstagramGrayscale(event, root)
        eventRouter.onEvent(
            event = event,
            rootProvider = { root },
            settings = currentSettings
        )
        scheduleInstagramNavigationChecks(event)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (::overlayController.isInitialized) {
            overlayController.dismiss()
        }
        if (::homeFeedOverlayController.isInitialized) {
            homeFeedOverlayController.dismiss()
        }
        if (::instagramWebOverlayController.isInitialized) {
            instagramWebOverlayController.dismiss()
        }
        if (::homeFeedAudioController.isInitialized) {
            homeFeedAudioController.restore()
        }
        if (::systemGrayscaleController.isInitialized) {
            systemGrayscaleController.restore()
        }
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun updateInstagramGrayscale(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?
    ) {
        if (!::systemGrayscaleController.isInitialized) return
        val eventPackageName = event.packageName?.toString().orEmpty()
        val rootPackageName = root?.packageName?.toString().orEmpty()
        val eventClassName = event.className?.toString().orEmpty()
        val instagramIsActive = eventPackageName == InstagramPackageConfig.PACKAGE_NAME ||
            rootPackageName == InstagramPackageConfig.PACKAGE_NAME

        if (instagramIsActive) {
            nativeInstagramVisibleForGrayscale = true
            if (currentSettings.grayscaleInstagramApp) {
                systemGrayscaleController.enable()
            } else {
                systemGrayscaleController.restore()
            }
            return
        }

        val isWeLiveEvent = eventPackageName == packageName || rootPackageName == packageName
        if (isWeLiveEvent && !eventClassName.contains("MainActivity")) {
            return
        }

        if (eventPackageName == SYSTEM_UI_PACKAGE || rootPackageName == SYSTEM_UI_PACKAGE) {
            return
        }

        nativeInstagramVisibleForGrayscale = false
        systemGrayscaleController.restore()
    }

    private fun preloadHomeFeedBlockOnInstagramOpen(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?
    ) {
        val eventPackageName = event.packageName?.toString().orEmpty()
        val rootPackageName = root?.packageName?.toString().orEmpty()
        val eventClassName = event.className?.toString().orEmpty()
        val instagramIsActive = eventPackageName == InstagramPackageConfig.PACKAGE_NAME ||
            rootPackageName == InstagramPackageConfig.PACKAGE_NAME

        if (instagramIsActive) {
            if (!nativeInstagramSessionActiveForHomePreload) {
                nativeInstagramSessionActiveForHomePreload = true
                val settings = currentSettings
                if (
                    settings.blockInstagramHomeFeed &&
                    settings.preloadHomeFeedBlockOnInstagramOpen &&
                    !settings.isTemporarilyAllowed()
                ) {
                    eventRouter.showProvisionalHomeFeedBlock(
                        event = event,
                        root = root,
                        blockStories = settings.blockInstagramHomeStories
                    )
                }
            }
            return
        }

        val isWeLiveOverlayEvent = (eventPackageName == packageName || rootPackageName == packageName) &&
            !eventClassName.contains("MainActivity")
        val isSystemEvent = eventPackageName == SYSTEM_UI_PACKAGE || rootPackageName == SYSTEM_UI_PACKAGE
        if (!isWeLiveOverlayEvent && !isSystemEvent) {
            nativeInstagramSessionActiveForHomePreload = false
        }
    }

    private fun enforceInstagramOpenLimit(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?
    ): Boolean {
        val eventPackageName = event.packageName?.toString().orEmpty()
        val rootPackageName = root?.packageName?.toString().orEmpty()
        val eventClassName = event.className?.toString().orEmpty()
        val instagramIsActive = eventPackageName == InstagramPackageConfig.PACKAGE_NAME ||
            rootPackageName == InstagramPackageConfig.PACKAGE_NAME

        if (!instagramIsActive) {
            val isWeLiveOverlayEvent = (eventPackageName == packageName || rootPackageName == packageName) &&
                !eventClassName.contains("MainActivity")
            val isSystemEvent = eventPackageName == SYSTEM_UI_PACKAGE || rootPackageName == SYSTEM_UI_PACKAGE
            if (!isWeLiveOverlayEvent && !isSystemEvent) {
                nativeInstagramSessionActiveForOpenLimit = false
            }
            return false
        }

        val settings = currentSettings
        if (!settings.limitInstagramOpensPerDay) {
            nativeInstagramSessionActiveForOpenLimit = true
            return false
        }

        if (nativeInstagramSessionActiveForOpenLimit) {
            return false
        }

        val todayKey = LocalDate.now().toString()
        if (settings.hasInstagramOpensRemaining(todayKey)) {
            nativeInstagramSessionActiveForOpenLimit = true
            serviceScope.launch {
                settingsRepository.recordInstagramAppOpen(todayKey)
            }
            return false
        }

        val now = System.currentTimeMillis()
        if (now - lastOpenLimitReturnAt > OPEN_LIMIT_RETURN_COOLDOWN_MS) {
            lastOpenLimitReturnAt = now
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
        return true
    }

    private fun enforceInstagramAccessSchedule(
        event: AccessibilityEvent,
        root: AccessibilityNodeInfo?
    ): Boolean {
        val eventPackageName = event.packageName?.toString().orEmpty()
        val rootPackageName = root?.packageName?.toString().orEmpty()
        val instagramIsActive = eventPackageName == InstagramPackageConfig.PACKAGE_NAME ||
            rootPackageName == InstagramPackageConfig.PACKAGE_NAME
        if (!instagramIsActive) {
            return false
        }

        val settings = currentSettings
        if (!settings.limitInstagramToSchedule || settings.isTemporarilyAllowed()) {
            return false
        }

        if (settings.isWithinInstagramSchedule(ZonedDateTime.now())) {
            return false
        }

        val now = System.currentTimeMillis()
        if (now - lastScheduleReturnAt > SCHEDULE_RETURN_COOLDOWN_MS) {
            lastScheduleReturnAt = now
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
        return true
    }

    private fun openAppSettings() {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(Settings.EXTRA_SETTINGS_EMBEDDED_DEEP_LINK_INTENT_URI, "welive://settings")
        }
        startActivity(intent)
    }

    private fun performStoryGestureTap(screenX: Int, screenY: Int): Boolean {
        return dispatchTap(screenX, screenY)
    }

    private fun scheduleInstagramNavigationChecks(event: AccessibilityEvent) {
        val eventPackageName = event.packageName?.toString().orEmpty()
        val rootPackageName = rootForEvent(event)?.packageName?.toString().orEmpty()
        val instagramIsActive = eventPackageName == InstagramPackageConfig.PACKAGE_NAME ||
            rootPackageName == InstagramPackageConfig.PACKAGE_NAME
        if (!instagramIsActive || !event.isInstagramNavigationEvent()) return

        NAVIGATION_RECHECK_DELAYS_MS.forEach { delayMillis ->
            val eventCopy = AccessibilityEvent.obtain(event)
            mainHandler.postDelayed(
                {
                    if (::eventRouter.isInitialized) {
                        eventRouter.onEvent(
                            event = eventCopy,
                            rootProvider = { rootForEvent(eventCopy) },
                            settings = currentSettings
                        )
                    }
                    eventCopy.recycle()
                },
                delayMillis
            )
        }
    }

    private fun rootForEvent(event: AccessibilityEvent): AccessibilityNodeInfo? {
        val activeRoot = rootInActiveWindow
        val eventPackageName = event.packageName?.toString().orEmpty()
        val eventClassName = event.className?.toString().orEmpty()
        if (eventPackageName == packageName && eventClassName.contains("MainActivity")) {
            HomeDebugRecorder.recordRootSelection(
                event = event,
                activeRoot = activeRoot,
                chosenRoot = activeRoot,
                windowPackages = collectWindowPackages()
            )
            return activeRoot
        }

        val preferredPackages = preferredRootPackages(eventPackageName, eventClassName)
        if (preferredPackages.isEmpty()) {
            HomeDebugRecorder.recordRootSelection(
                event = event,
                activeRoot = activeRoot,
                chosenRoot = activeRoot,
                windowPackages = collectWindowPackages()
            )
            return activeRoot
        }

        if (activeRoot.packageNameString() in preferredPackages) {
            HomeDebugRecorder.recordRootSelection(
                event = event,
                activeRoot = activeRoot,
                chosenRoot = activeRoot,
                windowPackages = collectWindowPackages()
            )
            return activeRoot
        }

        val chosenRoot = windows
            .asSequence()
            .mapNotNull { window -> runCatching { window.root }.getOrNull() }
            .firstOrNull { root -> root.packageNameString() in preferredPackages }
            ?: activeRoot
        HomeDebugRecorder.recordRootSelection(
            event = event,
            activeRoot = activeRoot,
            chosenRoot = chosenRoot,
            windowPackages = collectWindowPackages()
        )
        return chosenRoot
    }

    private fun preferredRootPackages(
        eventPackageName: String,
        eventClassName: String
    ): Set<String> {
        return when {
            eventPackageName == InstagramPackageConfig.PACKAGE_NAME -> setOf(InstagramPackageConfig.PACKAGE_NAME)
            BrowserPackageConfig.isSupported(eventPackageName) -> setOf(eventPackageName)
            eventPackageName == packageName && !eventClassName.contains("MainActivity") -> setOf(
                InstagramPackageConfig.PACKAGE_NAME
            )
            else -> emptySet()
        }
    }

    private fun AccessibilityNodeInfo?.packageNameString(): String {
        return this?.packageName?.toString().orEmpty()
    }

    private fun collectWindowPackages(): List<String> {
        if (!HomeDebugRecorder.isRecording) return emptyList()
        return windows.mapNotNull { window ->
            runCatching { window.root?.packageName?.toString() }.getOrNull()
        }
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

    private fun dispatchTap(screenX: Int, screenY: Int): Boolean {
        val tapPath = Path().apply {
            moveTo(screenX.toFloat(), screenY.toFloat())
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(tapPath, 0L, 80L))
            .build()
        return dispatchGesture(gesture, null, mainHandler)
    }

    private companion object {
        const val SYSTEM_UI_PACKAGE = "com.android.systemui"
        const val OPEN_LIMIT_RETURN_COOLDOWN_MS = 750L
        const val SCHEDULE_RETURN_COOLDOWN_MS = 750L
        const val HOME_NAVIGATION_SUPPRESSION_MS = 450L
        const val STORY_OPEN_SUPPRESSION_MS = 1_500L
        val NAVIGATION_RECHECK_DELAYS_MS = longArrayOf(24L, 64L, 120L, 220L, 360L, 600L, 900L)
    }
}
