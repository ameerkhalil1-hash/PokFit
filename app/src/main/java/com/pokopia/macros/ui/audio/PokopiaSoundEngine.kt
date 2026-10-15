package com.pokopia.macros.ui.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.sin
import kotlin.math.sign

/**
 * Programmatic Dual-Persona PCM Sound & Haptic Synthesizer (`PokopiaSoundEngine.kt`).
 *
 * Generates real-time 44.1kHz 16-bit mono PCM waveforms without external .mp3/.ogg files:
 * - Ditto Sounds: Warm sine/triangle wave pitch-bent "bloop-squish" chimes and cozy pentatonic
 *   island marimba arpeggios on food/meal taps.
 * - Porygon Sounds: Crisp 8-bit/16-bit square-wave digital chirps, cybernetic `Conversion`
 *   arpeggios on barcode scan lock, and upbeat retro fanfare when syncing Strava/Pixel Watch data.
 */
@Singleton
class PokopiaSoundEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val sampleRate = 44100

    enum class WaveShape { SINE, TRIANGLE, SQUARE }

    /**
     * Plays Ditto's warm pitch-bent "bloop-squish" chime + soft island haptic tap.
     */
    fun playDittoSquishChime() {
        triggerHaptic(longArrayOf(0, 18, 35, 28), intArrayOf(0, 90, 0, 140))
        audioScope.launch {
            val samples = synthesizePitchBentTone(
                startFreqHz = 310.0,
                endFreqHz = 620.0,
                durationMs = 210,
                wave = WaveShape.TRIANGLE,
                vibratoHz = 12.0,
                vibratoDepthHz = 24.0
            )
            playPcmBuffer(samples)
        }
    }

    /**
     * Plays Ditto's cozy C-major pentatonic island marimba arpeggio when logging meals or hitting goals.
     */
    fun playDittoIslandMarimbaArpeggio() {
        triggerHaptic(longArrayOf(0, 20, 40, 20, 40, 35), intArrayOf(0, 100, 0, 130, 0, 180))
        audioScope.launch {
            // C5 - D5 - E5 - G5 - A5 - C6 cozy marimba progression
            val notes = listOf(523.25, 587.33, 659.25, 783.99, 880.0, 1046.50)
            val combined = ShortArray(sampleRate * 650 / 1000)
            notes.forEachIndexed { idx, freq ->
                val offsetSamples = (idx * 85 * sampleRate) / 1000
                val tone = synthesizePercussiveMarimbaNote(freq, durationMs = 180)
                for (i in tone.indices) {
                    val targetIdx = offsetSamples + i
                    if (targetIdx < combined.size) {
                        val mixed = combined[targetIdx].toInt() + (tone[i].toInt() / 2)
                        combined[targetIdx] = mixed.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    }
                }
            }
            playPcmBuffer(combined)
        }
    }

    /**
     * Plays Porygon's crisp 8-bit/16-bit square-wave `Conversion` lock-on arpeggio on barcode scan.
     */
    fun playPorygonLockOnScan() {
        triggerHaptic(longArrayOf(0, 14, 22, 14, 22, 45), intArrayOf(0, 180, 0, 210, 0, 255))
        audioScope.launch {
            // Cybernetic Porygon Lock-On sequence (A5 -> E6 -> A6 -> C#6 -> A6)
            val freqs = listOf(880.0, 1318.5, 1760.0, 1108.7, 1760.0)
            val stepMs = 55
            val totalSamples = (sampleRate * (freqs.size * stepMs)) / 1000
            val buffer = ShortArray(totalSamples)
            freqs.forEachIndexed { idx, freq ->
                val startSample = (idx * stepMs * sampleRate) / 1000
                val noteSamples = synthesizeSquareChirp(freq, stepMs)
                for (i in noteSamples.indices) {
                    if (startSample + i < buffer.size) {
                        buffer[startSample + i] = noteSamples[i]
                    }
                }
            }
            playPcmBuffer(buffer)
        }
    }

    /**
     * Plays Porygon's upbeat retro cyber-fanfare when syncing Strava / Pixel Watch / Weekly TDEE check-in.
     */
    fun playPorygonWearableSyncFanfare() {
        triggerHaptic(longArrayOf(0, 25, 30, 25, 30, 60), intArrayOf(0, 150, 0, 180, 0, 240))
        audioScope.launch {
            val chordNotes = listOf(587.33, 739.99, 880.00, 1174.66) // D major cyber triad
            val stepMs = 80
            val totalSamples = (sampleRate * (chordNotes.size * stepMs + 140)) / 1000
            val buffer = ShortArray(totalSamples)
            chordNotes.forEachIndexed { idx, freq ->
                val start = (idx * stepMs * sampleRate) / 1000
                val dur = if (idx == chordNotes.lastIndex) 200 else stepMs
                val note = synthesizeSquareChirp(freq, dur)
                for (i in note.indices) {
                    if (start + i < buffer.size) {
                        buffer[start + i] = note[i]
                    }
                }
            }
            playPcmBuffer(buffer)
        }
    }

    private fun synthesizePitchBentTone(
        startFreqHz: Double,
        endFreqHz: Double,
        durationMs: Int,
        wave: WaveShape,
        vibratoHz: Double,
        vibratoDepthHz: Double
    ): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val progress = i.toDouble() / count.toDouble()
            val t = i.toDouble() / sampleRate.toDouble()
            val instFreq = startFreqHz + (endFreqHz - startFreqHz) * progress +
                sin(2.0 * PI * vibratoHz * t) * vibratoDepthHz
            phase += (2.0 * PI * instFreq) / sampleRate.toDouble()
            val rawWave = when (wave) {
                WaveShape.SINE -> sin(phase)
                WaveShape.TRIANGLE -> (2.0 / PI) * kotlin.math.asin(sin(phase))
                WaveShape.SQUARE -> sign(sin(phase))
            }
            val envelope = sin(PI * progress)
            out[i] = (rawWave * envelope * Short.MAX_VALUE * 0.45).toInt().toShort()
        }
        return out
    }

    private fun synthesizePercussiveMarimbaNote(freqHz: Double, durationMs: Int): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate.toDouble()
            // Fundamental + 4th harmonic overtone characteristic of tuned rosewood marimba bars
            val wave = sin(2.0 * PI * freqHz * t) + 0.25 * sin(2.0 * PI * (freqHz * 4.0) * t)
            val decay = exp(-14.0 * t)
            out[i] = (wave * decay * Short.MAX_VALUE * 0.42).toInt()
                .coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return out
    }

    private fun synthesizeSquareChirp(freqHz: Double, durationMs: Int): ShortArray {
        val count = (sampleRate * durationMs) / 1000
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toDouble() / sampleRate.toDouble()
            val progress = i.toDouble() / count.toDouble()
            val sq = if (sin(2.0 * PI * freqHz * t) >= 0.0) 1.0 else -1.0
            val env = (1.0 - progress * 0.65)
            out[i] = (sq * env * Short.MAX_VALUE * 0.28).toInt().toShort()
        }
        return out
    }

    private fun playPcmBuffer(pcm: ShortArray) {
        runCatching {
            val byteSize = pcm.size * 2
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(byteSize.coerceAtLeast(2048))
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            track.write(pcm, 0, pcm.size)
            track.play()
        }
    }

    private fun triggerHaptic(timings: LongArray, amplitudes: IntArray) {
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            }
        }
    }
}
