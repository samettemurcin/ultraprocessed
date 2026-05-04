package com.b2.ultraprocessed.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import java.util.ArrayDeque

class AppSoundManager(context: Context) {
    private val appContext = context.applicationContext
    val startupCueDurationMillis: Long = resolveDurationMillis(AppSoundEvent.Startup.resId)
    private val soundPool = SoundPool.Builder()
        .setMaxStreams(MAX_STREAMS)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()
    private val soundIds = mutableMapOf<AppSoundEvent, Int>()
    private val loadedSampleIds = mutableSetOf<Int>()
    private val pendingEvents = ArrayDeque<AppSoundEvent>()
    private val lock = Any()
    @Volatile
    private var released = false

    init {
        soundPool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status != 0 || released) return@setOnLoadCompleteListener
            synchronized(lock) {
                loadedSampleIds += sampleId
                flushPendingLocked()
            }
        }

        AppSoundEvent.entries.forEach { event ->
            soundIds[event] = soundPool.load(appContext, event.resId, 1)
        }
    }

    fun play(event: AppSoundEvent) {
        if (released) return
        if (event == AppSoundEvent.Startup) {
            playStartupCue()
            return
        }
        val sampleId = soundIds[event] ?: return
        synchronized(lock) {
            if (loadedSampleIds.contains(sampleId)) {
                playSample(sampleId)
            } else {
                pendingEvents.addLast(event)
            }
        }
    }

    fun release() {
        if (released) return
        released = true
        synchronized(lock) {
            pendingEvents.clear()
            loadedSampleIds.clear()
            soundIds.clear()
        }
        soundPool.release()
    }

    private fun flushPendingLocked() {
        if (pendingEvents.isEmpty()) return
        val iterator = pendingEvents.iterator()
        while (iterator.hasNext()) {
            val event = iterator.next()
            val sampleId = soundIds[event] ?: continue
            if (loadedSampleIds.contains(sampleId)) {
                playSample(sampleId)
                iterator.remove()
            }
        }
    }

    private fun playSample(sampleId: Int) {
        soundPool.play(
            sampleId,
            1f,
            1f,
            /* priority = */ 1,
            /* loop = */ 0,
            /* rate = */ 1f,
        )
    }

    private fun playStartupCue() {
        if (released) return
        runCatching {
            MediaPlayer.create(appContext, AppSoundEvent.Startup.resId)?.apply {
                setVolume(1f, 1f)
                setOnCompletionListener {
                    it.release()
                }
                setOnErrorListener { player, _, _ ->
                    player.release()
                    true
                }
                start()
            }
        }
    }

    private fun resolveDurationMillis(resId: Int): Long {
        return runCatching {
            MediaPlayer.create(appContext, resId)?.usePlayer { player ->
                player.duration.toLong().coerceAtLeast(0L)
            } ?: DEFAULT_STARTUP_DURATION_MILLIS
        }.getOrDefault(DEFAULT_STARTUP_DURATION_MILLIS)
    }

    companion object {
        private const val MAX_STREAMS = 3
        private const val DEFAULT_STARTUP_DURATION_MILLIS = 1_200L
    }
}

private inline fun <T> MediaPlayer.usePlayer(block: (MediaPlayer) -> T): T {
    try {
        return block(this)
    } finally {
        release()
    }
}
