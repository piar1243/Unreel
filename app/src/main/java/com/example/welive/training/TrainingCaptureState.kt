package com.example.welive.training

import android.content.Context
import com.example.welive.detection.WindowNodeFeature
import com.example.welive.detection.WindowSnapshot
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

object TrainingCaptureState {
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _latestInstagramSnapshot = MutableStateFlow<WindowSnapshot?>(null)
    val latestInstagramSnapshot: StateFlow<WindowSnapshot?> = _latestInstagramSnapshot
    private val _latestSettingsSnapshot = MutableStateFlow<WindowSnapshot?>(null)
    val latestSettingsSnapshot: StateFlow<WindowSnapshot?> = _latestSettingsSnapshot
    @Volatile
    private var storageDirectory: File? = null

    fun initialize(context: Context) {
        val directory = File(context.applicationContext.filesDir, "training").apply { mkdirs() }
        storageDirectory = directory
        if (_latestInstagramSnapshot.value == null) {
            _latestInstagramSnapshot.value = loadSnapshot(File(directory, LATEST_INSTAGRAM_SNAPSHOT_FILE_NAME))
        }
        if (_latestSettingsSnapshot.value == null) {
            _latestSettingsSnapshot.value = loadSnapshot(File(directory, LATEST_SETTINGS_SNAPSHOT_FILE_NAME))
        }
    }

    fun recordInstagramSnapshot(snapshot: WindowSnapshot) {
        _latestInstagramSnapshot.value = snapshot
        persistSnapshot(LATEST_INSTAGRAM_SNAPSHOT_FILE_NAME, snapshot)
    }

    fun recordSettingsSnapshot(snapshot: WindowSnapshot) {
        _latestSettingsSnapshot.value = snapshot
        persistSnapshot(LATEST_SETTINGS_SNAPSHOT_FILE_NAME, snapshot)
    }

    private fun persistSnapshot(fileName: String, snapshot: WindowSnapshot) {
        val directory = storageDirectory ?: return
        ioScope.launch {
            runCatching {
                File(directory, fileName).writeText(snapshot.toJson().toString())
            }
        }
    }

    private fun loadSnapshot(file: File): WindowSnapshot? {
        return runCatching {
            if (!file.exists()) return null
            JSONObject(file.readText()).toSnapshot()
        }.getOrNull()
    }

    private fun WindowSnapshot.toJson(): JSONObject {
        return JSONObject()
            .put("packageName", packageName)
            .put("rootPackageName", rootPackageName)
            .put("eventPackageName", eventPackageName)
            .put("eventType", eventType)
            .put("nodeCount", nodeCount)
            .put("scrollableNodeCount", scrollableNodeCount)
            .put("isMusicActive", isMusicActive)
            .put("texts", JSONArray(texts.toList()))
            .put("contentDescriptions", JSONArray(contentDescriptions.toList()))
            .put("viewIds", JSONArray(viewIds.toList()))
            .put("classNames", JSONArray(classNames.toList()))
            .put("nodeFeatures", JSONArray(nodeFeatures.map { feature ->
                JSONObject()
                    .put("text", feature.text)
                    .put("contentDescription", feature.contentDescription)
                    .put("viewId", feature.viewId)
                    .put("className", feature.className)
                    .put("isClickable", feature.isClickable)
                    .put("isScrollable", feature.isScrollable)
                    .put("isVisibleToUser", feature.isVisibleToUser)
                    .put("boundsLeft", feature.boundsLeft)
                    .put("boundsTop", feature.boundsTop)
                    .put("boundsRight", feature.boundsRight)
                    .put("boundsBottom", feature.boundsBottom)
                    .put("isFocused", feature.isFocused)
                    .put("isEditable", feature.isEditable)
                    .put("isSelected", feature.isSelected)
            }))
    }

    private fun JSONObject.toSnapshot(): WindowSnapshot {
        return WindowSnapshot(
            packageName = optString("packageName"),
            rootPackageName = optString("rootPackageName"),
            eventPackageName = optString("eventPackageName"),
            eventType = optInt("eventType"),
            texts = optStringArray("texts"),
            contentDescriptions = optStringArray("contentDescriptions"),
            viewIds = optStringArray("viewIds"),
            classNames = optStringArray("classNames"),
            nodeCount = optInt("nodeCount"),
            scrollableNodeCount = optInt("scrollableNodeCount"),
            nodeFeatures = optJSONArray("nodeFeatures")?.let(::parseNodeFeatures).orEmpty(),
            isMusicActive = optBoolean("isMusicActive")
        )
    }

    private fun JSONObject.optStringArray(key: String): Set<String> {
        val array = optJSONArray(key) ?: return emptySet()
        return buildSet {
            for (index in 0 until array.length()) {
                add(array.optString(index))
            }
        }
    }

    private fun parseNodeFeatures(array: JSONArray): List<WindowNodeFeature> {
        return buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    WindowNodeFeature(
                        text = item.optString("text").ifBlank { null },
                        contentDescription = item.optString("contentDescription").ifBlank { null },
                        viewId = item.optString("viewId").ifBlank { null },
                        className = item.optString("className").ifBlank { null },
                        isClickable = item.optBoolean("isClickable"),
                        isScrollable = item.optBoolean("isScrollable"),
                        isVisibleToUser = item.optBoolean("isVisibleToUser"),
                        boundsLeft = item.optInt("boundsLeft"),
                        boundsTop = item.optInt("boundsTop"),
                        boundsRight = item.optInt("boundsRight"),
                        boundsBottom = item.optInt("boundsBottom"),
                        isFocused = item.optBoolean("isFocused"),
                        isEditable = item.optBoolean("isEditable"),
                        isSelected = item.optBoolean("isSelected")
                    )
                )
            }
        }
    }

    private const val LATEST_INSTAGRAM_SNAPSHOT_FILE_NAME = "latest_instagram_snapshot.json"
    private const val LATEST_SETTINGS_SNAPSHOT_FILE_NAME = "latest_settings_snapshot.json"
}
