package com.example.welive.intervention

import android.content.Context
import android.media.AudioManager

class HomeFeedAudioController(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var mutedByUnreel = false
    private var wasMutedBeforeBlock = false

    fun muteHomeFeed() {
        val manager = audioManager ?: return
        if (mutedByUnreel) return

        wasMutedBeforeBlock = manager.isStreamMute(AudioManager.STREAM_MUSIC)
        runCatching {
            manager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                0
            )
        }
        mutedByUnreel = true
    }

    fun restore() {
        val manager = audioManager ?: return
        if (!mutedByUnreel) return

        if (!wasMutedBeforeBlock) {
            runCatching {
                manager.adjustStreamVolume(
                    AudioManager.STREAM_MUSIC,
                    AudioManager.ADJUST_UNMUTE,
                    0
                )
            }
        }
        mutedByUnreel = false
        wasMutedBeforeBlock = false
    }
}
