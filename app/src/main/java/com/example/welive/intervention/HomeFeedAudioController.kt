package com.example.welive.intervention

import android.content.Context
import android.media.AudioManager

class HomeFeedAudioController(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var mutedByWeLive = false
    private var wasMutedBeforeBlock = false

    fun muteHomeFeed() {
        val manager = audioManager ?: return
        if (mutedByWeLive) return

        wasMutedBeforeBlock = manager.isStreamMute(AudioManager.STREAM_MUSIC)
        runCatching {
            manager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                0
            )
        }
        mutedByWeLive = true
    }

    fun restore() {
        val manager = audioManager ?: return
        if (!mutedByWeLive) return

        if (!wasMutedBeforeBlock) {
            runCatching {
                manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE,
                    0
                )
            }
        }
        mutedByWeLive = false
        wasMutedBeforeBlock = false
    }
}
