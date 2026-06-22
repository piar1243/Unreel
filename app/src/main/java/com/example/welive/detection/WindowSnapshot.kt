package com.example.welive.detection

data class WindowNodeFeature(
    val text: String?,
    val contentDescription: String?,
    val viewId: String?,
    val className: String?,
    val isClickable: Boolean,
    val isScrollable: Boolean,
    val isVisibleToUser: Boolean,
    val boundsLeft: Int,
    val boundsTop: Int,
    val boundsRight: Int,
    val boundsBottom: Int,
    val isFocused: Boolean = false,
    val isEditable: Boolean = false,
    val isSelected: Boolean = false
)

data class WindowSnapshot(
    val packageName: String,
    val rootPackageName: String,
    val eventPackageName: String,
    val eventType: Int,
    val texts: Set<String>,
    val contentDescriptions: Set<String>,
    val viewIds: Set<String>,
    val classNames: Set<String>,
    val nodeCount: Int,
    val scrollableNodeCount: Int,
    val nodeFeatures: List<WindowNodeFeature> = emptyList(),
    val isMusicActive: Boolean = false
) {
    val searchableText: String by lazy {
        (texts + contentDescriptions + viewIds)
            .joinToString(separator = " ")
            .lowercase()
    }

    fun containsAny(vararg needles: String): Boolean {
        return needles.any { searchableText.contains(it.lowercase()) }
    }

    fun containsViewIdAny(vararg needles: String): Boolean {
        return containsIn(viewIds, needles.toList())
    }

    fun containsTextOrDescriptionAny(vararg needles: String): Boolean {
        return containsIn(texts + contentDescriptions, needles.toList())
    }

    private fun containsIn(values: Collection<String>, needles: List<String>): Boolean {
        val normalizedValues = values.map { it.lowercase() }
        return needles.any { needle ->
            val normalizedNeedle = needle.lowercase()
            normalizedValues.any { value -> value.contains(normalizedNeedle) }
        }
    }
}
