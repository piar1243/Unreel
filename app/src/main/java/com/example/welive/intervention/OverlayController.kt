package com.example.welive.intervention

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.Typeface
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView

class OverlayController(private val service: AccessibilityService) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var overlayView: View? = null
    private var isDismissing = false
    private var lastShownAt = 0L
    private var pulseReleaseAt = 0L
    private var pendingPulseDismiss: Runnable? = null
    val isShowing: Boolean
        get() = overlayView != null

    fun showBlocked(
        onAllowOneMinute: () -> Unit,
        onOpenSettings: () -> Unit,
        title: String = "Reels Blocked",
        body: String = "You chose to keep this space clear."
    ): Boolean {
        val now = System.currentTimeMillis()
        if (overlayView != null || isDismissing) {
            return false
        }
        if (now - lastShownAt < 500L) {
            return false
        }
        lastShownAt = now

        val view = buildOverlay(
            onAllowOneMinute = onAllowOneMinute,
            onOpenSettings = onOpenSettings,
            showActions = true,
            title = title,
            body = body
        )
        overlayView = view

        windowManager.addView(view, overlayLayoutParams())
        view.alpha = 1f
        view.translationY = 0f

        return true
    }

    fun pulseBlocked(
        onCovered: () -> Unit,
        title: String = "Reels Blocked",
        body: String = "You chose to keep this space clear."
    ) {
        val now = System.currentTimeMillis()
        pulseReleaseAt = now + PULSE_HOLD_DURATION_MS + PULSE_DISMISS_DURATION_MS
        pendingPulseDismiss?.let(handler::removeCallbacks)

        val currentView = overlayView
        if (currentView != null) {
            holdSolid()
            onCovered()
            pendingPulseDismiss = Runnable { dismiss(PULSE_DISMISS_DURATION_MS) }
            handler.postDelayed(pendingPulseDismiss!!, PULSE_HOLD_DURATION_MS)
            return
        }

        val view = buildOverlay(
            onAllowOneMinute = {},
            onOpenSettings = {},
            showActions = false,
            title = title,
            body = body
        )
        overlayView = view
        windowManager.addView(view, overlayLayoutParams())
        view.alpha = 1f
        view.translationY = 0f
        onCovered()
        pendingPulseDismiss = Runnable { dismiss(PULSE_DISMISS_DURATION_MS) }
        handler.postDelayed(pendingPulseDismiss!!, PULSE_HOLD_DURATION_MS)
    }

    fun holdSolid() {
        val view = overlayView ?: return
        view.animate().cancel()
        isDismissing = false
        view.alpha = 1f
        view.translationY = 0f
    }

    fun runAfterOverlayEntrance(action: () -> Unit) {
        handler.postDelayed(action, 120L)
    }

    fun dismiss(durationMillis: Long = DEFAULT_DISMISS_DURATION_MS) {
        val view = overlayView ?: return
        if (isDismissing) return
        val effectiveDuration = if (System.currentTimeMillis() < pulseReleaseAt) {
            maxOf(durationMillis, PULSE_DISMISS_DURATION_MS)
        } else {
            durationMillis
        }
        pendingPulseDismiss?.let(handler::removeCallbacks)
        pendingPulseDismiss = null
        isDismissing = true
        view.animate()
            .alpha(0f)
            .setDuration(effectiveDuration)
            .withEndAction {
                runCatching { windowManager.removeView(view) }
                if (overlayView === view) {
                    overlayView = null
                }
                pulseReleaseAt = 0L
                isDismissing = false
            }
            .start()
    }

    fun dismissImmediately() {
        val view = overlayView ?: return
        view.animate().cancel()
        pendingPulseDismiss?.let(handler::removeCallbacks)
        pendingPulseDismiss = null
        runCatching { windowManager.removeView(view) }
        if (overlayView === view) {
            overlayView = null
        }
        pulseReleaseAt = 0L
        isDismissing = false
    }

    private fun buildOverlay(
        onAllowOneMinute: () -> Unit,
        onOpenSettings: () -> Unit,
        showActions: Boolean,
        title: String,
        body: String
    ): View {
        val coverPanel = LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32.dp(), 32.dp(), 32.dp(), 32.dp())
            setBackgroundColor(Color.rgb(5, 5, 6))
        }

        val accent = View(service).apply {
            setBackgroundColor(Color.rgb(0, 229, 160))
        }
        coverPanel.addView(accent, LinearLayout.LayoutParams(88.dp(), 4.dp()).apply {
            bottomMargin = 28.dp()
        })

        val title = TextView(service).apply {
            text = title
            setTextColor(Color.WHITE)
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            gravity = Gravity.CENTER
        }
        coverPanel.addView(title, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val bodyView = TextView(service).apply {
            text = body
            setTextColor(Color.rgb(190, 190, 196))
            textSize = 17f
            gravity = Gravity.CENTER
            setPadding(0, 12.dp(), 0, 28.dp())
        }
        coverPanel.addView(bodyView, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        if (showActions) {
            val actions = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            coverPanel.addView(actions, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))

            actions.addView(actionButton("Allow 1 min", inverted = true) {
                onAllowOneMinute()
                dismiss()
            })
            actions.addView(actionButton("Settings", inverted = false) {
                onOpenSettings()
                dismiss()
            })
        }

        return coverPanel
    }

    private fun overlayLayoutParams(): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }
    }

    private fun actionButton(
        label: String,
        inverted: Boolean,
        onClick: () -> Unit
    ): Button {
        return Button(service).apply {
            text = label
            isAllCaps = false
            textSize = 14f
            setTextColor(if (inverted) Color.BLACK else Color.WHITE)
            setBackgroundColor(if (inverted) Color.WHITE else Color.rgb(28, 28, 32))
            setOnClickListener { onClick() }
            layoutParams = LinearLayout.LayoutParams(132.dp(), 48.dp()).apply {
                leftMargin = 6.dp()
                rightMargin = 6.dp()
            }
        }
    }

    private fun Int.dp(): Int {
        return (this * service.resources.displayMetrics.density).toInt()
    }

    private companion object {
        const val DEFAULT_DISMISS_DURATION_MS = 180L
        const val PULSE_HOLD_DURATION_MS = 140L
        const val PULSE_DISMISS_DURATION_MS = 600L
    }
}
