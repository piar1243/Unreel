package com.example.welive.accessibility

import android.graphics.Rect
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot

class WindowSnapshotReader(
    private val maxNodes: Int = 260,
    private val musicActiveProvider: () -> Boolean = { false }
) {
    fun read(event: AccessibilityEvent, root: AccessibilityNodeInfo?): WindowSnapshot? {
        if (root == null) return null

        val texts = linkedSetOf<String>()
        val descriptions = linkedSetOf<String>()
        val viewIds = linkedSetOf<String>()
        val classNames = linkedSetOf<String>()
        val nodeFeatures = mutableListOf<WindowNodeFeature>()
        var nodeCount = 0
        var scrollableNodeCount = 0

        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.add(root)

        while (stack.isNotEmpty() && nodeCount < maxNodes) {
            val node = stack.removeLast()
            nodeCount += 1

            node.text?.cleanText()?.let(texts::add)
            node.contentDescription?.cleanText()?.let(descriptions::add)
            node.viewIdResourceName?.cleanText()?.let(viewIds::add)
            node.className?.cleanText()?.let(classNames::add)

            if (nodeFeatures.size < maxNodes && node.isUsefulFeatureNode()) {
                nodeFeatures += node.toFeature()
            }

            if (node.isScrollable) {
                scrollableNodeCount += 1
            }

            for (index in 0 until node.childCount) {
                node.getChild(index)?.let(stack::add)
            }
        }

        val rootPackageName = root.packageName?.toString().orEmpty()
        val eventPackageName = event.packageName?.toString().orEmpty()

        return WindowSnapshot(
            packageName = rootPackageName.ifBlank { eventPackageName },
            rootPackageName = rootPackageName,
            eventPackageName = eventPackageName,
            eventType = event.eventType,
            texts = texts,
            contentDescriptions = descriptions,
            viewIds = viewIds,
            classNames = classNames,
            nodeCount = nodeCount,
            scrollableNodeCount = scrollableNodeCount,
            nodeFeatures = nodeFeatures,
            isMusicActive = musicActiveProvider()
        )
    }

    private fun CharSequence.cleanText(): String? {
        val cleaned = toString().trim()
        return cleaned.takeIf { it.isNotBlank() }
    }

    private fun AccessibilityNodeInfo.isUsefulFeatureNode(): Boolean {
        return isClickable ||
            isScrollable ||
            !text.isNullOrBlank() ||
            !contentDescription.isNullOrBlank() ||
            !viewIdResourceName.isNullOrBlank()
    }

    private fun AccessibilityNodeInfo.toFeature(): WindowNodeFeature {
        val bounds = Rect()
        getBoundsInScreen(bounds)
        return WindowNodeFeature(
            text = text?.cleanText(),
            contentDescription = contentDescription?.cleanText(),
            viewId = viewIdResourceName?.cleanText(),
            className = className?.cleanText(),
            isClickable = isClickable,
            isScrollable = isScrollable,
            isVisibleToUser = isVisibleToUser,
            boundsLeft = bounds.left,
            boundsTop = bounds.top,
            boundsRight = bounds.right,
            boundsBottom = bounds.bottom,
            isFocused = isFocused,
            isEditable = isEditable,
            isSelected = isSelected
        )
    }
}
