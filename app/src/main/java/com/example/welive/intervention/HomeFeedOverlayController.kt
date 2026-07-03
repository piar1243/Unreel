package com.example.welive.intervention

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView

class HomeFeedOverlayController(
    private val service: AccessibilityService,
    private val onStoryGestureTap: (x: Int, y: Int) -> Boolean,
    private val onTransparentBandTapStarted: (isNavigationBand: Boolean) -> Unit = {}
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop
    private var overlayView: View? = null
    private var blockPanel: View? = null
    private var titleText: TextView? = null
    private var bodyText: TextView? = null
    private var currentRegion: ScreenRegion? = null
    private var currentBlockerRegion: ScreenRegion? = null
    private var currentStoryTapTargets: List<ScreenRegion> = emptyList()
    private var currentBlockStories = false
    private var isDismissing = false
    private var touchPassthrough = false
    private var restoreTouchBlocking: Runnable? = null
    private var downX = 0f
    private var downY = 0f
    private var moved = false

    val isShowing: Boolean
        get() = overlayView != null

    fun showOrUpdate(
        region: ScreenRegion,
        blockerRegion: ScreenRegion,
        storyTapTargets: List<ScreenRegion>,
        blockStories: Boolean
    ) {
        if (region.width <= 0 || region.height <= 0 || blockerRegion.width <= 0 || blockerRegion.height <= 0) return
        val effectiveBlockerRegion = if (blockStories) {
            region.copy(bottom = blockerRegion.bottom)
        } else {
            blockerRegion
        }
        if (blockStories) {
            restoreTouchBlocking?.let(handler::removeCallbacks)
            restoreTouchBlocking = null
            touchPassthrough = false
        }

        val view = overlayView
        if (view == null) {
            val newView = buildOverlay()
            overlayView = newView
            currentRegion = region
            currentBlockerRegion = blockerRegion
            currentStoryTapTargets = storyTapTargets
            currentBlockStories = blockStories
            updateBlockerCopy(blockStories)
            updateBlockerPanel(region, effectiveBlockerRegion)
            windowManager.addView(newView, overlayLayoutParams(region))
            newView.alpha = 1f
            return
        }

        currentRegion = region
        currentBlockerRegion = blockerRegion
        currentStoryTapTargets = storyTapTargets
        currentBlockStories = blockStories
        holdSolid()
        updateBlockerCopy(blockStories)
        updateBlockerPanel(region, effectiveBlockerRegion)
        windowManager.updateViewLayout(view, overlayLayoutParams(region))
    }

    fun holdSolid() {
        val view = overlayView ?: return
        view.animate().cancel()
        isDismissing = false
        view.alpha = 1f
    }

    fun dismiss() {
        dismissImmediately()
    }

    private fun buildOverlay(): View {
        return FrameLayout(service).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isLongClickable = true
            isFocusable = false
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX
                        downY = event.rawY
                        moved = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!moved) {
                            val deltaX = kotlin.math.abs(event.rawX - downX)
                            val deltaY = kotlin.math.abs(event.rawY - downY)
                            moved = deltaX > touchSlop || deltaY > touchSlop
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) {
                            val x = event.rawX.toInt()
                            val y = event.rawY.toInt()
                            val transparentTapKind = transparentBandTapKind(x, y)
                            if (transparentTapKind != null) {
                                replayTapThroughOverlay(
                                    x = x,
                                    y = y,
                                    isNavigationBand = transparentTapKind == TransparentTapKind.Navigation
                                )
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        moved = false
                        true
                    }
                    else -> true
                }
            }
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS

            blockPanel = buildBlockPanel()
            addView(blockPanel)
        }
    }

    private fun buildBlockPanel(): View {
        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(24.dp(), 20.dp(), 24.dp(), 20.dp())
            setBackgroundColor(Color.rgb(5, 5, 6))

            val accent = View(service).apply {
                setBackgroundColor(Color.rgb(0, 229, 160))
            }
            addView(accent, LinearLayout.LayoutParams(64.dp(), 4.dp()).apply {
                bottomMargin = 18.dp()
            })

            val title = TextView(service).apply {
                text = "Home Feed Blocked"
                setTextColor(Color.WHITE)
                textSize = 24f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
            }
            titleText = title
            addView(title)

            val body = TextView(service).apply {
                text = "Stories stay visible. Dragging is locked."
                setTextColor(Color.rgb(190, 190, 196))
                textSize = 15f
                gravity = Gravity.CENTER
                setPadding(0, 10.dp(), 0, 0)
            }
            bodyText = body
            addView(body)
        }
    }

    private fun updateBlockerPanel(region: ScreenRegion, blockerRegion: ScreenRegion) {
        val panel = blockPanel ?: return
        val topOffset = (blockerRegion.top - region.top).coerceAtLeast(0)
        panel.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            blockerRegion.height
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            topMargin = topOffset
        }
    }

    private fun transparentBandTapKind(x: Int, y: Int): TransparentTapKind? {
        val region = currentRegion ?: return null
        val blockerRegion = currentBlockerRegion ?: return null
        if (!region.contains(x, y)) return null

        if (y < TOP_APP_BAR_PASSTHROUGH_DP.dp()) {
            return TransparentTapKind.Navigation
        }

        if (y > blockerRegion.bottom || y > region.bottom - BOTTOM_NAV_PASSTHROUGH_DP.dp()) {
            return TransparentTapKind.Navigation
        }

        if (currentBlockStories) return null
        if (currentStoryTapTargets.any { target -> target.contains(x, y) }) {
            return TransparentTapKind.Story
        }
        return if (y < blockerRegion.top) TransparentTapKind.Story else null
    }

    private fun dismissImmediately() {
        val view = overlayView ?: return
        view.animate().cancel()
        restoreTouchBlocking?.let(handler::removeCallbacks)
        restoreTouchBlocking = null
        touchPassthrough = false
        runCatching { windowManager.removeView(view) }
        overlayView = null
        blockPanel = null
        titleText = null
        bodyText = null
        currentRegion = null
        currentBlockerRegion = null
        currentStoryTapTargets = emptyList()
        currentBlockStories = false
        isDismissing = false
    }

    private fun updateBlockerCopy(blockStories: Boolean) {
        titleText?.text = "Home Feed Blocked"
        bodyText?.text = if (blockStories) {
            "Stories and feed are blocked."
        } else {
            "Stories and navigation stay available. Dragging is locked."
        }
    }

    private fun replayTapThroughOverlay(
        x: Int,
        y: Int,
        isNavigationBand: Boolean
    ) {
        onTransparentBandTapStarted(isNavigationBand)
        setTouchPassthrough(true)
        val replayDelay = if (isNavigationBand) {
            NAVIGATION_TAP_REPLAY_DELAY_MS
        } else {
            STORY_TAP_REPLAY_DELAY_MS
        }
        handler.postDelayed({ onStoryGestureTap(x, y) }, replayDelay)
        restoreTouchBlocking?.let(handler::removeCallbacks)
        val restoreRunnable = Runnable {
            setTouchPassthrough(false)
            restoreTouchBlocking = null
        }
        restoreTouchBlocking = restoreRunnable
        handler.postDelayed(restoreRunnable, STORY_TAP_RESTORE_DELAY_MS)
    }

    private fun setTouchPassthrough(enabled: Boolean) {
        val view = overlayView ?: return
        val region = currentRegion ?: return
        touchPassthrough = enabled
        runCatching { windowManager.updateViewLayout(view, overlayLayoutParams(region)) }
    }

    private fun overlayLayoutParams(region: ScreenRegion): WindowManager.LayoutParams {
        val touchFlag = if (touchPassthrough) {
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        } else {
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
        }
        return WindowManager.LayoutParams(
            region.width,
            region.height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                touchFlag,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = region.left
            y = region.top
        }
    }

    private fun Int.dp(): Int {
        return (this * service.resources.displayMetrics.density).toInt()
    }

    private companion object {
        const val STORY_TAP_REPLAY_DELAY_MS = 24L
        const val NAVIGATION_TAP_REPLAY_DELAY_MS = 48L
        const val STORY_TAP_RESTORE_DELAY_MS = 220L
        const val TOP_APP_BAR_PASSTHROUGH_DP = 64
        const val BOTTOM_NAV_PASSTHROUGH_DP = 96
    }

    private enum class TransparentTapKind {
        Story,
        Navigation
    }
}
