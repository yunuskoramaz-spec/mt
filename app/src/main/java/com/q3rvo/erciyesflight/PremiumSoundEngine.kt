package com.q3rvo.erciyesflight

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/** Small pre-rendered PCM sound bank. Playback reuses tracks and never blocks gameplay. */
class PremiumSoundEngine {
    private val sampleRate = 44_100
    private val tracks = ArrayList<AudioTrack>(8)
    private var released = false

    init {
        val sounds = listOf(
            SoundSpec(0.075f, 620f, 0.22f, 980f),
            SoundSpec(0.105f, 470f, 0.20f, 760f),
            SoundSpec(0.085f, 820f, 0.16f, 1180f),
            SoundSpec(0.16f, 540f, 0.28f, 240f),
            SoundSpec(0.22f, 150f, 0.30f, 70f),
            SoundSpec(0.12f, 350f, 0.20f, 700f),
            SoundSpec(0.13f, 260f, 0.18f, 420f),
            SoundSpec(0.06f, 760f, 0.14f, 520f)
        )
        sounds.forEach { tracks += createTrack(render(it)) }
    }

    fun play(index: Int) {
        if (released || index !in tracks.indices) return
        val track = tracks[index]
        try {
            track.stop()
            track.reloadStaticData()
            track.play()
        } catch (_: IllegalStateException) {
            // Audio must never interrupt gameplay.
        }
    }

    fun release() {
        if (released) return
        released = true
        tracks.forEach { runCatching { it.release() } }
        tracks.clear()
    }

    private fun createTrack(samples: ShortArray): AudioTrack {
        val format = AudioFormat.Builder()
            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
            .setSampleRate(sampleRate)
            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
            .build()
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val track = AudioTrack.Builder()
            .setAudioAttributes(attributes)
            .setAudioFormat(format)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .setBufferSizeInBytes(samples.size * 2)
            .build()
        track.write(samples, 0, samples.size)
        return track
    }

    private fun render(spec: SoundSpec): ShortArray {
        val count = (sampleRate * spec.duration).toInt().coerceAtLeast(64)
        val result = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / sampleRate
            val p = (i + 1f) / count
            val attack = (p / 0.08f).coerceAtMost(1f)
            val decay = (1f - p).coerceAtLeast(0f)
            val envelope = attack * (0.18f + 0.82f * decay * decay)
            val sweep = spec.startHz + (spec.endHz - spec.startHz) * p
            val harmonic = sin(2.0 * PI * sweep * t) * 0.78 + sin(2.0 * PI * sweep * 1.97 * t) * 0.22
            result[i] = (harmonic * envelope * spec.volume * Short.MAX_VALUE)
                .toInt().coerceIn(-32768, 32767).toShort()
        }
        return result
    }

    private data class SoundSpec(
        val duration: Float,
        val startHz: Float,
        val volume: Float,
        val endHz: Float
    )
}
