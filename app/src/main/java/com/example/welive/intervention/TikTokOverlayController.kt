package com.example.welive.intervention

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot

class TikTokOverlayController(
    private val service: AccessibilityService
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null

    val isShowing: Boolean
        get() = overlayView != null

    fun showOrHold(snapshot: WindowSnapshot? = null) {
        val bottomNavTop = snapshot?.resolveBottomNavigationTop()
            ?: fallbackBottomNavigationTop()
        val current = overlayView
        if (current != null) {
            current.alpha = 1f
            runCatching { windowManager.updateViewLayout(current, layoutParams(bottomNavTop)) }
            return
        }

        val view = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.rgb(5, 5, 6))
            addView(View(service).apply {
                setBackgroundColor(Color.rgb(0, 229, 160))
            }, LinearLayout.LayoutParams(88.dp(), 4.dp()).apply {
                bottomMargin = 28.dp()
            })
            addView(TextView(service).apply {
                text = "TikTok Feed Blocked"
                setTextColor(Color.WHITE)
                textSize = 32f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            addView(TextView(service).apply {
                text = "Messages remain available below."
                setTextColor(Color.rgb(190, 190, 196))
                textSize = 17f
                gravity = Gravity.CENTER
                setPadding(24.dp(), 12.dp(), 24.dp(), 0)
            })
        }

        overlayView = view
        windowManager.addView(view, layoutParams(bottomNavTop))
        view.alpha = 1f
    }

    fun dismissImmediately() {
        val view = overlayView ?: return
        runCatching { windowManager.removeView(view) }
        if (overlayView === view) overlayView = null
    }

    private fun layoutParams(bottomNavTop: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            bottomNavTop.coerceAtLeast(1),
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.OPAQUE
        ).apply {
            gravity = Gravity.TOP
        }
    }

    private fun WindowSnapshot.resolveBottomNavigationTop(): Int? {
        val displayHeight = service.resources.displayMetrics.heightPixels
        val candidates = nodeFeatures
            .asSequence()
            .filter { it.isVisibleToUser }
            .filter { it.boundsTop > displayHeight * 0.62f }
            .filter { it.boundsBottom <= displayHeight && it.boundsBottom > it.boundsTop }
            .filter { it.boundsBottom - it.boundsTop <= 180.dp() }
            .filter { it.looksLikeBottomNavigation() }
            .map { it.boundsTop }
            .toList()
        val detectedNavigationTop = candidates.minOrNull() ?: return null

        // Tune the lower edge when TikTok exposes its navigation bar.
        // Increase this value to leave more of the bottom navigation visible.
        return (detectedNavigationTop - BOTTOM_EDGE_CLEARANCE_DP.dp())
            .coerceIn((displayHeight * 0.68f).toInt(), displayHeight - 1)
    }

    private fun fallbackBottomNavigationTop(): Int {
        return (service.resources.displayMetrics.heightPixels - BOTTOM_NAV_PASSTHROUGH_DP.dp())
            .coerceAtLeast(1)
    }

    private fun WindowNodeFeature.looksLikeBottomNavigation(): Boolean {
        val combined = listOfNotNull(viewId, text, contentDescription)
            .joinToString(" ")
            .lowercase()
        return BOTTOM_NAV_MARKERS.any(combined::contains)
    }

    private fun Int.dp(): Int {
        return (this * service.resources.displayMetrics.density).toInt()
    }

    private companion object {
        const val BOTTOM_NAV_PASSTHROUGH_DP = 168
        const val BOTTOM_EDGE_CLEARANCE_DP = 33
        val BOTTOM_NAV_MARKERS = listOf(
            "bottom_nav", "bottom_tab", "tab_bar", "navigation_bar",
            "inbox", "messages_tab", "message_tab", "profile_tab", "home_tab", "friends_tab", "friends"
        )
    }
}
