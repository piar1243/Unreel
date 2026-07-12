package com.example.welive.intervention

import android.content.Context
import android.media.AudioManager

/** Keeps TikTok media silent only while the TikTok shortform blocker is visible. */
class TikTokAudioController(context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private var mutedByUnreel = false
    private var wasMutedBeforeBlock = false

    fun enforceMute() {
        val manager = audioManager ?: return
        if (!mutedByUnreel) {
            wasMutedBeforeBlock = manager.isStreamMute(AudioManager.STREAM_MUSIC)
            mutedByUnreel = true
        }
        runCatching {
            manager.adjustStreamVolume(
                AudioManager.STREAM_MUSIC,
                AudioManager.ADJUST_MUTE,
                0
            )
        }
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
