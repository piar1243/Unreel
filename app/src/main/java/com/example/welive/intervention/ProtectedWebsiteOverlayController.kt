package com.example.welive.intervention

import android.accessibilityservice.AccessibilityService
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityWindowInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import com.example.welive.protection.ProtectedApp

class ProtectedWebsiteOverlayController(
    private val service: AccessibilityService,
    private val app: ProtectedApp
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null

    val isShowing: Boolean
        get() = overlayView != null

    fun showOrUpdate(snapshot: WindowSnapshot, onOpenSettings: () -> Unit) {
        val region = resolveContentRegion(snapshot)
        if (region.width <= 0 || region.height < MIN_VISIBLE_BLOCK_HEIGHT_DP.dp()) return

        val current = overlayView
        if (current != null) {
            current.alpha = 1f
            runCatching { windowManager.updateViewLayout(current, layoutParams(region)) }
            return
        }

        val view = buildOverlay(onOpenSettings)
        overlayView = view
        windowManager.addView(view, layoutParams(region))
        view.alpha = 1f
    }

    fun holdSolid() {
        overlayView?.alpha = 1f
    }

    fun dismissImmediately() {
        val view = overlayView ?: return
        runCatching { windowManager.removeView(view) }
        if (overlayView === view) overlayView = null
    }

    private fun buildOverlay(onOpenSettings: () -> Unit): View {
        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28.dp(), 28.dp(), 28.dp(), 28.dp())
            setBackgroundColor(Color.rgb(5, 5, 6))
            isClickable = true
            isFocusable = false

            addView(View(service).apply {
                setBackgroundColor(Color.rgb(0, 229, 160))
            }, LinearLayout.LayoutParams(80.dp(), 4.dp()).apply {
                bottomMargin = 24.dp()
            })
            addView(TextView(service).apply {
                text = "${app.displayName} Website Blocked"
                setTextColor(Color.WHITE)
                textSize = 26f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
            })
            addView(TextView(service).apply {
                text = "Browser controls stay available."
                setTextColor(Color.rgb(190, 190, 196))
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 10.dp(), 0, 24.dp())
            })
            addView(Button(service).apply {
                text = "Settings"
                isAllCaps = false
                textSize = 14f
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.rgb(28, 28, 32))
                setOnClickListener {
                    dismissImmediately()
                    onOpenSettings()
                }
            }, LinearLayout.LayoutParams(132.dp(), 48.dp()))
        }
    }

    private fun resolveContentRegion(snapshot: WindowSnapshot): ScreenRegion {
        val width = service.resources.displayMetrics.widthPixels
        val height = service.resources.displayMetrics.heightPixels
        val visibleNodes = snapshot.nodeFeatures.filter { it.isVisibleToUser }
        val topChromeBottom = visibleNodes
            .asSequence()
            .filter { it.boundsTop in 0..(height / 3) && it.looksLikeTopBrowserChrome() }
            .map { it.boundsBottom }
            .filter { it > 0 }
            .maxOrNull()
        val bottomChromeTop = visibleNodes
            .asSequence()
            .filter { it.boundsTop > height * 0.6f && it.looksLikeBottomBrowserChrome() }
            .map { it.boundsTop }
            .filter { it > 0 }
            .minOrNull()

        val top = maxOf(TOP_DISTANCE_DP.dp(), (topChromeBottom ?: 0) + CHROME_GAP_DP.dp())
            .coerceAtMost(height - MIN_BLOCK_HEIGHT_DP.dp())
        val naturalBottom = height - BOTTOM_DISTANCE_DP.dp()
        val keyboardTop = visibleKeyboardTop()
        val bottom = listOfNotNull(
            naturalBottom,
            bottomChromeTop?.minus(CHROME_GAP_DP.dp()),
            keyboardTop?.minus(KEYBOARD_GAP_DP.dp())
        ).minOrNull()?.coerceIn(top, height) ?: naturalBottom

        return ScreenRegion(0, top, width, bottom)
    }

    private fun visibleKeyboardTop(): Int? {
        val displayHeight = service.resources.displayMetrics.heightPixels
        return service.windows
            .asSequence()
            .filter { it.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
            .mapNotNull { window ->
                val bounds = Rect()
                window.getBoundsInScreen(bounds)
                bounds.takeIf {
                    !it.isEmpty && it.top in 1 until displayHeight &&
                        it.height() > MIN_KEYBOARD_HEIGHT_DP.dp()
                }?.top
            }
            .minOrNull()
    }

    private fun WindowNodeFeature.looksLikeTopBrowserChrome(): Boolean {
        val id = viewId.orEmpty().lowercase()
        return TOP_CHROME_MARKERS.any(id::contains)
    }

    private fun WindowNodeFeature.looksLikeBottomBrowserChrome(): Boolean {
        val id = viewId.orEmpty().lowercase()
        return BOTTOM_CHROME_MARKERS.any(id::contains)
    }

    private fun layoutParams(region: ScreenRegion) = WindowManager.LayoutParams(
        region.width,
        region.height,
        WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
        android.graphics.PixelFormat.OPAQUE
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        x = region.left
        y = region.top
    }

    private fun Int.dp(): Int = (this * service.resources.displayMetrics.density).toInt()

    private companion object {
        const val TOP_DISTANCE_DP = 60
        const val BOTTOM_DISTANCE_DP = 54
        const val CHROME_GAP_DP = 8
        const val KEYBOARD_GAP_DP = 8
        const val MIN_BLOCK_HEIGHT_DP = 270
        const val MIN_VISIBLE_BLOCK_HEIGHT_DP = 96
        const val MIN_KEYBOARD_HEIGHT_DP = 120
        val TOP_CHROME_MARKERS = listOf("url_bar", "location_bar", "toolbar_container", "control_container")
        val BOTTOM_CHROME_MARKERS = listOf("bottom_toolbar", "tab_group_ui_container", "bottom_navigation")
    }
}
