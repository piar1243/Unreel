package com.example.welive.intervention

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Color
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.FrameLayout
import com.example.welive.detection.WindowSnapshot
import kotlin.math.abs

/** Allows taps on one shared Short while swallowing every drag gesture. */
class YouTubeFriendShortsGuardController(
    private val service: AccessibilityService
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop
    private var overlayView: View? = null
    private var currentRegion: ScreenRegion? = null
    private var touchPassthrough = false
    private var restoreTouchBlocking: Runnable? = null
    private var downX = 0f
    private var downY = 0f
    private var moved = false

    val isShowing: Boolean
        get() = overlayView != null

    fun showOrUpdate(snapshot: WindowSnapshot) {
        showOrUpdateRegion(resolveWebContentRegion(snapshot) ?: return)
    }

    fun showNativeOrUpdate(snapshot: WindowSnapshot) {
        showOrUpdateRegion(resolveNativeContentRegion(snapshot))
    }

    private fun showOrUpdateRegion(region: ScreenRegion) {
        val current = overlayView
        if (current == null) {
            val view = buildGuardView()
            overlayView = view
            currentRegion = region
            windowManager.addView(view, layoutParams(region))
            return
        }
        currentRegion = region
        current.alpha = 1f
        runCatching { windowManager.updateViewLayout(current, layoutParams(region)) }
    }

    fun dismiss() {
        val view = overlayView ?: return
        restoreTouchBlocking?.let(handler::removeCallbacks)
        restoreTouchBlocking = null
        touchPassthrough = false
        runCatching { windowManager.removeView(view) }
        overlayView = null
        currentRegion = null
        moved = false
    }

    private fun buildGuardView(): View {
        return FrameLayout(service).apply {
            setBackgroundColor(Color.TRANSPARENT)
            isClickable = true
            isFocusable = false
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
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
                            moved = abs(event.rawX - downX) > touchSlop ||
                                abs(event.rawY - downY) > touchSlop
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (!moved) replayTap(event.rawX.toInt(), event.rawY.toInt())
                        true
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        moved = false
                        true
                    }
                    else -> true
                }
            }
        }
    }

    private fun replayTap(x: Int, y: Int) {
        setTouchPassthrough(true)
        handler.postDelayed({ dispatchTap(x, y) }, TAP_REPLAY_DELAY_MS)
        restoreTouchBlocking?.let(handler::removeCallbacks)
        val restore = Runnable {
            setTouchPassthrough(false)
            restoreTouchBlocking = null
        }
        restoreTouchBlocking = restore
        handler.postDelayed(restore, TAP_RESTORE_DELAY_MS)
    }

    private fun dispatchTap(x: Int, y: Int) {
        val path = Path().apply { moveTo(x.toFloat(), y.toFloat()) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0L, 70L))
            .build()
        service.dispatchGesture(gesture, null, handler)
    }

    private fun setTouchPassthrough(enabled: Boolean) {
        val view = overlayView ?: return
        val region = currentRegion ?: return
        touchPassthrough = enabled
        runCatching { windowManager.updateViewLayout(view, layoutParams(region)) }
    }

    private fun resolveWebContentRegion(snapshot: WindowSnapshot): ScreenRegion? {
        val displayWidth = service.resources.displayMetrics.widthPixels
        val displayHeight = service.resources.displayMetrics.heightPixels
        val candidates = snapshot.nodeFeatures.filter { feature ->
            val id = feature.viewId.orEmpty().lowercase()
            val clazz = feature.className.orEmpty().lowercase()
            val width = feature.boundsRight - feature.boundsLeft
            val height = feature.boundsBottom - feature.boundsTop
            feature.isVisibleToUser &&
                width > displayWidth * 0.7f &&
                height > displayHeight * 0.4f &&
                (FRIEND_SHORTS_CONTENT_MARKERS.any(id::contains) || clazz.contains("webview"))
        }
        val top = candidates.minOfOrNull { it.boundsTop } ?: return null
        val bottom = candidates.maxOfOrNull { it.boundsBottom } ?: return null
        if (bottom <= top) return null
        return ScreenRegion(0, top, displayWidth, bottom.coerceAtMost(displayHeight))
    }

    private fun resolveNativeContentRegion(snapshot: WindowSnapshot): ScreenRegion {
        val displayWidth = service.resources.displayMetrics.widthPixels
        val displayHeight = service.resources.displayMetrics.heightPixels
        val bottomNavigationTop = snapshot.nodeFeatures
            .asSequence()
            .filter { it.isVisibleToUser }
            .filter { it.boundsTop > displayHeight * 0.72f }
            .filter { it.boundsBottom <= displayHeight && it.boundsBottom > it.boundsTop }
            .filter { feature ->
                val combined = listOfNotNull(
                    feature.viewId,
                    feature.text,
                    feature.contentDescription
                ).joinToString(" ").lowercase()
                NATIVE_BOTTOM_NAV_MARKERS.any(combined::contains)
            }
            .map { it.boundsTop }
            .minOrNull()
            ?: (displayHeight - NATIVE_BOTTOM_CLEARANCE_DP.dp())
        return ScreenRegion(
            left = 0,
            top = 0,
            right = displayWidth,
            bottom = bottomNavigationTop.coerceIn(displayHeight / 2, displayHeight)
        )
    }

    private fun layoutParams(region: ScreenRegion): WindowManager.LayoutParams {
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
        const val TAP_REPLAY_DELAY_MS = 45L
        const val TAP_RESTORE_DELAY_MS = 210L
        const val NATIVE_BOTTOM_CLEARANCE_DP = 120
        val FRIEND_SHORTS_CONTENT_MARKERS = listOf(
            "webview_frame_container",
            "webview_container",
            "player-shorts-container",
            "shorts-video",
            "carousel-scrollable-wrapper"
        )
        val NATIVE_BOTTOM_NAV_MARKERS = listOf(
            "bottom_nav",
            "pivot_bar",
            "navigation",
            "home",
            "shorts",
            "subscriptions",
            "library",
            "you"
        )
    }
}
