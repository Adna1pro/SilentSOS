package com.example.silentsos

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlin.concurrent.thread
import kotlin.math.sqrt

class SoundHandler(
    private val onSoundLevelDetected: (Int) -> Unit
) {

    private var audioRecord: AudioRecord? = null
    private var running = false

    fun start() {
        if (running) return
        running = true

        val sampleRate = 44100
        val bufferSize = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        if (bufferSize <= 0) {
            running = false
            return
        }

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )

        if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
            audioRecord?.release()
            audioRecord = null
            running = false
            return
        }

        audioRecord?.startRecording()

        thread(start = true) {
            val buffer = ShortArray(bufferSize)

            // Smoothing for more stable sound detection
            var smoothRms = 0.0
            val alpha = 0.25 // smoothing factor (0 = no smoothing, 1 = instant)
            while (running) {
                val record = audioRecord ?: break
                val read = record.read(buffer, 0, buffer.size)

                if (read > 0) {
                    var sum = 0.0
                    for (i in 0 until read) {
                        sum += buffer[i] * buffer[i]
                    }
                    val rms = sqrt(sum / read)
                    smoothRms = if (smoothRms == 0.0) rms else (alpha * rms + (1 - alpha) * smoothRms)
                    onSoundLevelDetected(smoothRms.toInt())
                }

                Thread.sleep(250)
            }
        }
    }

    fun stop() {
        running = false
        try {
            audioRecord?.stop()
        } catch (_: Exception) { }

        audioRecord?.release()
        audioRecord = null
    }
}