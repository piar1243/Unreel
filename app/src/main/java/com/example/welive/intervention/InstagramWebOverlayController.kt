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

class InstagramWebOverlayController(
    private val service: AccessibilityService
) {
    private val windowManager = service.getSystemService(WindowManager::class.java)
    private var overlayView: View? = null
    private var isDismissing = false

    val isShowing: Boolean
        get() = overlayView != null

    fun showOrUpdate(
        snapshot: WindowSnapshot,
        onAllowOneMinute: () -> Unit,
        onOpenSettings: () -> Unit
    ) {
        val region = resolveWebsiteContentRegion(snapshot)
        if (region.width <= 0 || region.height < MIN_VISIBLE_BLOCK_HEIGHT_DP.dp()) {
            dismiss()
            return
        }

        val view = overlayView
        if (view == null) {
            val newView = buildOverlay(onAllowOneMinute, onOpenSettings)
            overlayView = newView
            isDismissing = false
            windowManager.addView(newView, overlayLayoutParams(region))
            newView.alpha = 1f
            return
        }

        holdSolid()
        windowManager.updateViewLayout(view, overlayLayoutParams(region))
    }

    fun holdSolid() {
        val view = overlayView ?: return
        view.animate().cancel()
        isDismissing = false
        view.alpha = 1f
    }

    fun dismiss() {
        val view = overlayView ?: return
        if (isDismissing) return
        isDismissing = true
        view.animate()
            .alpha(0f)
            .setDuration(140L)
            .withEndAction {
                runCatching { windowManager.removeView(view) }
                if (overlayView === view) {
                    overlayView = null
                }
                isDismissing = false
            }
            .start()
    }

    private fun buildOverlay(
        onAllowOneMinute: () -> Unit,
        onOpenSettings: () -> Unit
    ): View {
        return LinearLayout(service).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(28.dp(), 28.dp(), 28.dp(), 28.dp())
            setBackgroundColor(Color.rgb(5, 5, 6))
            isClickable = true
            isLongClickable = true
            isFocusable = false

            val accent = View(service).apply {
                setBackgroundColor(Color.rgb(0, 229, 160))
            }
            addView(accent, LinearLayout.LayoutParams(80.dp(), 4.dp()).apply {
                bottomMargin = 24.dp()
            })

            addView(TextView(service).apply {
                text = "Instagram Website Blocked"
                setTextColor(Color.WHITE)
                textSize = 26f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                gravity = Gravity.CENTER
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))

            addView(TextView(service).apply {
                text = "Browser controls stay available."
                setTextColor(Color.rgb(190, 190, 196))
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(0, 10.dp(), 0, 24.dp())
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))

            val actions = LinearLayout(service).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
            }
            addView(actions, LinearLayout.LayoutParams(
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
    }

    private fun resolveWebsiteContentRegion(snapshot: WindowSnapshot): ScreenRegion {
        val displayMetrics = service.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val browserChromeBottom = snapshot.nodeFeatures
            .asSequence()
            .filter { it.isVisibleToUser }
            .filter { it.boundsTop in 0..(screenHeight / 3) }
            .filter { it.looksLikeBrowserChrome() }
            .map { it.boundsBottom }
            .filter { it > 0 }
            .maxOrNull()

        val topInset = maxOf(WEBSITE_BLOCK_TOP_DISTANCE_DP.dp(), (browserChromeBottom ?: 0) + 8.dp())
            .coerceAtMost(screenHeight - MIN_BLOCK_HEIGHT_DP.dp())
        val bottomInset = WEBSITE_BLOCK_BOTTOM_DISTANCE_DP.dp()
        val keyboardTop = visibleKeyboardTop()
        val naturalBottom = screenHeight - bottomInset
        val bottom = if (keyboardTop != null) {
            (keyboardTop - KEYBOARD_TOP_GAP_DP.dp()).coerceAtMost(naturalBottom)
        } else {
            naturalBottom.coerceAtLeast(topInset + MIN_BLOCK_HEIGHT_DP.dp())
        }

        return ScreenRegion(
            left = 0,
            top = topInset,
            right = screenWidth,
            bottom = bottom.coerceIn(topInset, screenHeight)
        )
    }

    private fun visibleKeyboardTop(): Int? {
        val displayHeight = service.resources.displayMetrics.heightPixels
        return service.windows
            .asSequence()
            .filter { window -> window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD }
            .mapNotNull { window ->
                val bounds = Rect()
                window.getBoundsInScreen(bounds)
                bounds.takeIf {
                    !it.isEmpty &&
                        it.top > 0 &&
                        it.top < displayHeight &&
                        it.height() > MIN_KEYBOARD_HEIGHT_DP.dp()
                }?.top
            }
            .minOrNull()
    }

    private fun WindowNodeFeature.looksLikeBrowserChrome(): Boolean {
        val normalizedId = viewId.orEmpty().lowercase()
        return BROWSER_CHROME_ID_MARKERS.any { marker -> normalizedId.contains(marker) }
    }

    private fun overlayLayoutParams(region: ScreenRegion): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            region.width,
            region.height,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            android.graphics.PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = region.left
            y = region.top
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
        // Tune these two values to move the Instagram website blocker edges.
        const val WEBSITE_BLOCK_TOP_DISTANCE_DP = 60
        const val WEBSITE_BLOCK_BOTTOM_DISTANCE_DP = 54
        const val MIN_BLOCK_HEIGHT_DP = 270
        const val MIN_VISIBLE_BLOCK_HEIGHT_DP = 96
        const val MIN_KEYBOARD_HEIGHT_DP = 120
        const val KEYBOARD_TOP_GAP_DP = 8

        val BROWSER_CHROME_ID_MARKERS = listOf(
            "url_bar",
            "location_bar",
            "search_box",
            "toolbar",
            "tab_switcher",
            "menu_button",
            "home_button"
        )
    }
}
