package com.example.welive.intervention

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView

class LinkedInOverlayController(
    private val service: AccessibilityService
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null

    val isShowing: Boolean
        get() = overlayView != null

    fun showOrHold() {
        val current = overlayView
        if (current != null) {
            current.alpha = 1f
            return
        }

        val view = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28.dp(), 28.dp(), 28.dp(), 28.dp())
            setBackgroundColor(Color.rgb(5, 5, 6))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS

            addView(View(service).apply {
                setBackgroundColor(Color.rgb(0, 229, 160))
            }, LinearLayout.LayoutParams(88.dp(), 4.dp()).apply {
                bottomMargin = 28.dp()
            })
            addView(TextView(service).apply {
                text = "LinkedIn Video Blocked"
                setTextColor(Color.WHITE)
                textSize = 32f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            addView(TextView(service).apply {
                text = "LinkedIn remains available without the short-video feed."
                setTextColor(Color.rgb(190, 190, 196))
                textSize = 17f
                gravity = Gravity.CENTER
                setPadding(24.dp(), 12.dp(), 24.dp(), 0)
            })
        }
        overlayView = view
        windowManager.addView(view, layoutParams())
        view.alpha = 1f
    }

    fun runWhenCovered(action: () -> Unit) {
        val view = overlayView ?: return
        view.postOnAnimation {
            if (overlayView === view) action()
        }
    }

    fun dismissImmediately() {
        val view = overlayView ?: return
        runCatching { windowManager.removeView(view) }
        if (overlayView === view) overlayView = null
    }

    private fun layoutParams() = WindowManager.LayoutParams(
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.MATCH_PARENT,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        android.graphics.PixelFormat.OPAQUE
    ).apply {
        gravity = Gravity.TOP or Gravity.START
    }

    private fun Int.dp(): Int = (this * service.resources.displayMetrics.density).toInt()
}
