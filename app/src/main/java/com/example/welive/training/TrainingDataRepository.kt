package com.example.welive.training

import android.content.Context
import com.example.welive.detection.WindowSnapshot
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class TrainingDataRepository(private val context: Context) {
    private val trainingDir: File
        get() = File(context.filesDir, "training").apply { mkdirs() }

    private val samplesFile: File
        get() = File(trainingDir, "instagram_snapshots.jsonl")

    val samplesFilePath: String
        get() = samplesFile.absolutePath

    suspend fun saveInstagramSample(label: String, snapshot: WindowSnapshot): Int {
        return withContext(Dispatchers.IO) {
            samplesFile.appendText(snapshot.toJson(label).toString() + "\n")
            countSamplesInternal()
        }
    }

    suspend fun countSamples(): Int {
        return withContext(Dispatchers.IO) { countSamplesInternal() }
    }

    private fun countSamplesInternal(): Int {
        return if (samplesFile.exists()) samplesFile.readLines().count { it.isNotBlank() } else 0
    }

    private fun WindowSnapshot.toJson(label: String): JSONObject {
        return JSONObject()
            .put("label", label)
            .put("capturedAtMillis", System.currentTimeMillis())
            .put("packageName", packageName)
            .put("rootPackageName", rootPackageName)
            .put("eventPackageName", eventPackageName)
            .put("eventType", eventType)
            .put("nodeCount", nodeCount)
            .put("scrollableNodeCount", scrollableNodeCount)
            .put("isMusicActive", isMusicActive)
            .put("texts", texts.toTrainingArray())
            .put("contentDescriptions", contentDescriptions.toTrainingArray())
            .put("viewIds", JSONArray(viewIds.sorted()))
            .put("classNames", JSONArray(classNames.sorted()))
            .put("controlHints", JSONArray(controlHints()))
            .put("nodeFeatures", nodeFeatures.toTrainingJson())
    }

    private fun Collection<String>.toTrainingArray(): JSONArray {
        return JSONArray(
            mapNotNull { it.toTrainingToken() }
                .distinct()
                .sorted()
        )
    }

    private fun String.toTrainingToken(): String? {
        val normalized = trim()
            .lowercase()
            .replace(Regex("\\s+"), " ")
        if (normalized.isBlank()) return null

        val knownUiWords = listOf(
            "reels",
            "like",
            "comment",
            "send",
            "share",
            "repost",
            "save",
            "saved",
            "bookmark",
            "audio",
            "remix",
            "profile",
            "home",
            "feed",
            "search",
            "explore",
            "search results",
            "recent searches",
            "suggested",
            "for you",
            "posts",
            "top posts",
            "grid",
            "thumbnail",
            "direct",
            "message",
            "messages",
            "message…",
            "inbox",
            "sent",
            "sent you",
            "shared",
            "forward",
            "forward message",
            "voice message",
            "audio call",
            "reply",
            "story",
            "followers",
            "following",
            "edit profile",
            "more options",
            "not interested",
            "report"
        )

        return if (knownUiWords.any { normalized.contains(it) }) {
            normalized.take(96)
        } else {
            "<redacted:${normalized.length}>"
        }
    }

    private fun WindowSnapshot.controlHints(): List<String> {
        val haystack = (texts + contentDescriptions + viewIds)
            .joinToString(separator = " ")
            .lowercase()
        return listOf(
            "reels",
            "clips",
            "like",
            "comment",
            "send",
            "share",
            "repost",
            "bookmark",
            "save",
            "audio",
            "remix",
            "more options",
            "explore",
            "search",
            "suggested",
            "posts",
            "grid",
            "message",
            "messages",
            "sent",
            "shared",
            "forward",
            "reply",
            "friend",
            "thread"
        ).filter { haystack.contains(it) }
    }

    private fun List<com.example.welive.detection.WindowNodeFeature>.toTrainingJson(): JSONArray {
        return JSONArray(map { feature ->
            JSONObject()
                .put("text", feature.text?.toTrainingToken())
                .put("contentDescription", feature.contentDescription?.toTrainingToken())
                .put("viewId", feature.viewId)
                .put("className", feature.className)
                .put("isClickable", feature.isClickable)
                .put("isScrollable", feature.isScrollable)
                .put("isVisibleToUser", feature.isVisibleToUser)
                .put("isFocused", feature.isFocused)
                .put("isEditable", feature.isEditable)
                .put("isSelected", feature.isSelected)
                .put("bounds", JSONObject()
                    .put("left", feature.boundsLeft)
                    .put("top", feature.boundsTop)
                    .put("right", feature.boundsRight)
                    .put("bottom", feature.boundsBottom)
                )
        })
    }
}
