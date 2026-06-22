package com.example.welive.diagnostics

import android.content.Context
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.example.welive.detection.DetectionResult
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import com.example.welive.detection.platforms.instagram.InstagramHomeFeedClassification
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class HomeDebugRecorderState(
    val isRecording: Boolean = false,
    val startedAtMillis: Long = 0L,
    val eventCount: Int = 0,
    val lastSessionFilePath: String? = null,
    val message: String = "Ready"
)

object HomeDebugRecorder {
    private const val MAX_EVENTS = 5_000
    private const val MAX_KEY_NODES = 120
    private const val MAX_TEXT_TOKENS = 48
    private val timestampFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
    private val lock = Any()
    private val _state = MutableStateFlow(HomeDebugRecorderState())
    private var startedAtMillis = 0L
    private var sessionFilePath: String? = null
    private var lines = mutableListOf<String>()

    val state: StateFlow<HomeDebugRecorderState> = _state

    val isRecording: Boolean
        get() = _state.value.isRecording

    fun start(context: Context) {
        val now = System.currentTimeMillis()
        val file = newSessionFile(context)
        synchronized(lock) {
            startedAtMillis = now
            sessionFilePath = file.absolutePath
            lines = mutableListOf()
            _state.value = HomeDebugRecorderState(
                isRecording = true,
                startedAtMillis = now,
                eventCount = 0,
                lastSessionFilePath = file.absolutePath,
                message = "Recording"
            )
        }
        record(
            JSONObject()
                .put("type", "session")
                .put("decision", "start")
                .put("filePath", file.absolutePath)
        )
    }

    suspend fun stop(context: Context): HomeDebugRecorderState {
        val snapshot: List<String>
        val filePath: String
        synchronized(lock) {
            if (!_state.value.isRecording) return _state.value
            recordLocked(
                JSONObject()
                    .put("type", "session")
                    .put("decision", "stop")
            )
            snapshot = lines.toList()
            filePath = sessionFilePath ?: newSessionFile(context).absolutePath
            _state.value = _state.value.copy(
                isRecording = false,
                message = "Saving ${snapshot.size} events"
            )
        }

        return withContext(Dispatchers.IO) {
            val file = File(filePath).apply {
                parentFile?.mkdirs()
                writeText(snapshot.joinToString(separator = "\n", postfix = "\n"))
            }
            synchronized(lock) {
                lines = mutableListOf()
                _state.value = HomeDebugRecorderState(
                    isRecording = false,
                    startedAtMillis = 0L,
                    eventCount = snapshot.size,
                    lastSessionFilePath = file.absolutePath,
                    message = "Saved"
                )
                _state.value
            }
        }
    }

    fun recordRouter(
        phase: String,
        decision: String,
        event: AccessibilityEvent,
        rootPackageName: String,
        snapshot: WindowSnapshot? = null,
        detectorResult: DetectionResult? = null,
        homeFeedClassification: InstagramHomeFeedClassification? = null,
        overlayShowing: Boolean,
        homeFeedOverlayShowing: Boolean,
        webOverlayShowing: Boolean,
        extras: Map<String, Any?> = emptyMap()
    ) {
        if (!isRecording) return
        val sourceNode = runCatching { event.source }.getOrNull()
        record(
            baseRecord("router", decision)
                .put("phase", phase)
                .put("event", event.toDebugJson(sourceNode))
                .put("rootPackageName", rootPackageName)
                .put("overlays", JSONObject()
                    .put("fullscreen", overlayShowing)
                    .put("homeFeed", homeFeedOverlayShowing)
                    .put("web", webOverlayShowing)
                )
                .putIfNotNull("snapshot", snapshot?.toDebugJson())
                .putIfNotNull("detector", detectorResult?.toDebugJson())
                .putIfNotNull("homeFeed", homeFeedClassification?.toDebugJson())
                .put("extras", extras.toJsonObject())
        )
    }

    fun recordRootSelection(
        event: AccessibilityEvent,
        activeRoot: AccessibilityNodeInfo?,
        chosenRoot: AccessibilityNodeInfo?,
        windowPackages: List<String>
    ) {
        if (!isRecording) return
        record(
            baseRecord("service_root", "select_root")
                .put("eventType", event.eventTypeName())
                .put("eventPackageName", event.packageName?.toString().orEmpty())
                .put("eventClassName", event.className?.toString().orEmpty())
                .put("activeRootPackageName", activeRoot.packageNameString())
                .put("chosenRootPackageName", chosenRoot.packageNameString())
                .put("windowPackages", JSONArray(windowPackages.distinct()))
        )
    }

    fun recordExternal(decision: String, extras: Map<String, Any?> = emptyMap()) {
        if (!isRecording) return
        record(
            baseRecord("external", decision)
                .put("extras", extras.toJsonObject())
        )
    }

    private fun record(record: JSONObject) {
        synchronized(lock) {
            if (!_state.value.isRecording) return
            recordLocked(record)
        }
    }

    private fun recordLocked(record: JSONObject) {
        val countBefore = lines.size
        val stamped = record
            .put("capturedAtMillis", System.currentTimeMillis())
            .put("elapsedMillis", elapsedMillis())
            .put("index", countBefore)
        if (lines.size >= MAX_EVENTS) {
            lines.removeAt(0)
        }
        lines += stamped.toString()
        _state.value = _state.value.copy(
            eventCount = lines.size,
            message = if (lines.size >= MAX_EVENTS) "Recording, oldest events rolling off" else "Recording"
        )
    }

    private fun elapsedMillis(): Long {
        return if (startedAtMillis == 0L) 0L else System.currentTimeMillis() - startedAtMillis
    }

    private fun baseRecord(type: String, decision: String): JSONObject {
        return JSONObject()
            .put("type", type)
            .put("decision", decision)
    }

    private fun newSessionFile(context: Context): File {
        val dir = File(context.filesDir, "diagnostics/home_debug_sessions").apply { mkdirs() }
        val name = "home_debug_${LocalDateTime.now().format(timestampFormatter)}.jsonl"
        return File(dir, name)
    }

    private fun AccessibilityEvent.toDebugJson(sourceNode: AccessibilityNodeInfo?): JSONObject {
        return JSONObject()
            .put("type", eventTypeName())
            .put("typeId", eventType)
            .put("packageName", packageName?.toString().orEmpty())
            .put("className", className?.toString().orEmpty())
            .put("text", JSONArray(text.mapNotNull { value -> value?.toString()?.toDebugToken() }))
            .put("contentDescription", contentDescription?.toString()?.toDebugToken())
            .put("source", JSONObject()
                .put("packageName", sourceNode.packageNameString())
                .put("viewId", sourceNode?.viewIdResourceName.orEmpty())
                .put("className", sourceNode?.className?.toString().orEmpty())
                .put("text", sourceNode?.text?.toString()?.toDebugToken())
                .put("contentDescription", sourceNode?.contentDescription?.toString()?.toDebugToken())
                .put("isSelected", sourceNode?.isSelected == true)
                .put("isFocused", sourceNode?.isFocused == true)
                .put("isClickable", sourceNode?.isClickable == true)
            )
    }

    private fun WindowSnapshot.toDebugJson(): JSONObject {
        return JSONObject()
            .put("packageName", packageName)
            .put("rootPackageName", rootPackageName)
            .put("eventPackageName", eventPackageName)
            .put("eventType", eventType)
            .put("eventTypeName", eventType.eventTypeName())
            .put("nodeCount", nodeCount)
            .put("scrollableNodeCount", scrollableNodeCount)
            .put("isMusicActive", isMusicActive)
            .put("visibleFeatureNodeCount", nodeFeatures.count { it.isVisibleToUser })
            .put("selectedIds", JSONArray(nodeFeatures
                .filter { it.isSelected }
                .mapNotNull { it.viewId }
                .distinct()
            ))
            .put("focusedIds", JSONArray(nodeFeatures
                .filter { it.isFocused }
                .mapNotNull { it.viewId }
                .distinct()
            ))
            .put("viewIds", JSONArray(viewIds.sorted().take(90)))
            .put("texts", texts.toDebugTokenArray())
            .put("contentDescriptions", contentDescriptions.toDebugTokenArray())
            .put("keyNodes", nodeFeatures
                .asSequence()
                .filter { it.isDebugKeyNode() }
                .take(MAX_KEY_NODES)
                .map { it.toDebugJson() }
                .toList()
                .let(::JSONArray)
            )
    }

    private fun DetectionResult.toDebugJson(): JSONObject {
        return JSONObject()
            .put("platform", platform.name)
            .put("surface", surface.name)
            .put("confidence", confidence)
            .put("packageName", packageName)
            .put("recommendedAction", recommendedAction.name)
            .put("reasons", JSONArray(reasons))
    }

    private fun InstagramHomeFeedClassification.toDebugJson(): JSONObject {
        return JSONObject()
            .put("state", state.name)
            .put("confidence", confidence)
            .put("reasons", JSONArray(reasons))
    }

    private fun WindowNodeFeature.toDebugJson(): JSONObject {
        return JSONObject()
            .put("viewId", viewId)
            .put("className", className)
            .put("text", text?.toDebugToken())
            .put("contentDescription", contentDescription?.toDebugToken())
            .put("isClickable", isClickable)
            .put("isScrollable", isScrollable)
            .put("isVisibleToUser", isVisibleToUser)
            .put("isFocused", isFocused)
            .put("isSelected", isSelected)
            .put("bounds", JSONObject()
                .put("left", boundsLeft)
                .put("top", boundsTop)
                .put("right", boundsRight)
                .put("bottom", boundsBottom)
            )
    }

    private fun WindowNodeFeature.isDebugKeyNode(): Boolean {
        if (isSelected || isFocused || isScrollable) return true
        val id = viewId?.lowercase().orEmpty()
        val label = listOfNotNull(text, contentDescription).joinToString(" ").lowercase()
        return DEBUG_NODE_MARKERS.any { marker -> id.contains(marker) || label.contains(marker) }
    }

    private fun Collection<String>.toDebugTokenArray(): JSONArray {
        return JSONArray(
            mapNotNull { it.toDebugToken() }
                .distinct()
                .sorted()
                .take(MAX_TEXT_TOKENS)
        )
    }

    private fun Map<String, Any?>.toJsonObject(): JSONObject {
        val json = JSONObject()
        forEach { (key, value) ->
            when (value) {
                null -> json.put(key, JSONObject.NULL)
                is Boolean -> json.put(key, value)
                is Number -> json.put(key, value)
                is Enum<*> -> json.put(key, value.name)
                is Collection<*> -> json.put(key, JSONArray(value.map { it?.toString() }))
                else -> json.put(key, value.toString())
            }
        }
        return json
    }

    private fun JSONObject.putIfNotNull(key: String, value: JSONObject?): JSONObject {
        if (value != null) put(key, value)
        return this
    }

    private fun AccessibilityNodeInfo?.packageNameString(): String {
        return this?.packageName?.toString().orEmpty()
    }

    private fun AccessibilityEvent.eventTypeName(): String {
        return eventType.eventTypeName()
    }

    private fun Int.eventTypeName(): String {
        return runCatching { AccessibilityEvent.eventTypeToString(this) }.getOrElse { toString() }
    }

    private fun String.toDebugToken(): String? {
        val normalized = trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return null
        return if (KNOWN_UI_WORDS.any { normalized.contains(it) }) {
            normalized.take(120)
        } else {
            "<redacted:${normalized.length}>"
        }
    }

    private val KNOWN_UI_WORDS = setOf(
        "activity",
        "audio",
        "back",
        "bookmark",
        "comment",
        "direct",
        "edit profile",
        "explore",
        "feed",
        "follow",
        "followers",
        "following",
        "forward",
        "grid",
        "home",
        "instagram",
        "like",
        "message",
        "messages",
        "more options",
        "posts",
        "profile",
        "reels",
        "remix",
        "repost",
        "save",
        "search",
        "send",
        "share",
        "story",
        "suggested",
        "voice message"
    )

    private val DEBUG_NODE_MARKERS = setOf(
        "action_bar",
        "avatar_image_view",
        "cf_hub_recycler_view",
        "clips",
        "direct",
        "feed",
        "feed_tab",
        "follow",
        "home",
        "inbox",
        "list",
        "media",
        "message",
        "outer_container",
        "profile",
        "recycler",
        "row_feed",
        "search",
        "story",
        "swipeable_tab_view_pager",
        "viewpager"
    )
}
