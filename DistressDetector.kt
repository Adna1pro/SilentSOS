package com.example.silentsos

class DistressDetector {

    // Runtime-adjustable thresholds (SAFE TUNING)
    var ACCEL_THRESHOLD = 23.0f     // was 21
    var GYRO_THRESHOLD = 2.2f       // was 1.8
    var SOUND_THRESHOLD = 5000    // clap / loud shout only

    private val TIME_WINDOW_MS = 2000L
    private val SOUND_SUSTAIN_MS = 50L

    // Sensor state
    private var accelTriggered = false
    private var gyroTriggered = false
    private var soundTriggered = false

    private var firstTriggerTime = 0L
    private var soundStartTime = 0L

    // --------- SENSITIVITY ----------
    fun setSensitivity(low: Boolean = false, medium: Boolean = false, high: Boolean = false) {
        when {
            low -> {
                ACCEL_THRESHOLD = 26.0f
                GYRO_THRESHOLD = 2.8f
                SOUND_THRESHOLD = 10000
            }
            medium -> {
                ACCEL_THRESHOLD = 23.0f
                GYRO_THRESHOLD = 2.2f
                SOUND_THRESHOLD = 8000
            }
            high -> {
                ACCEL_THRESHOLD = 19.0f
                GYRO_THRESHOLD = 1.5f
                SOUND_THRESHOLD = 6000
            }
        }
    }

    // --------- SENSOR INPUTS ----------
    fun updateAccelerometer(accel: Float) {
        if (accel >= ACCEL_THRESHOLD) {
            registerTrigger { accelTriggered = true }
        }
    }

    fun updateGyroscope(gyro: Float) {
        if (gyro >= GYRO_THRESHOLD) {
            registerTrigger { gyroTriggered = true }
        }
    }

    fun updateSoundLevel(sound: Int) {
        val now = System.currentTimeMillis()

        if (sound >= SOUND_THRESHOLD) {
            if (soundStartTime == 0L) {
                soundStartTime = now
            }
            if (now - soundStartTime >= SOUND_SUSTAIN_MS) {
                registerTrigger { soundTriggered = true }
            }
        } else {
            soundStartTime = 0L
        }
    }

    // --------- CORE LOGIC ----------
    private fun registerTrigger(action: () -> Unit) {
        val now = System.currentTimeMillis()

        if (firstTriggerTime == 0L) firstTriggerTime = now

        if (now - firstTriggerTime <= TIME_WINDOW_MS) {
            action()
        } else {
            reset()
        }
    }

    fun isDistressConfirmed(): Boolean {
        val confirmed =
            (accelTriggered && gyroTriggered) ||
                    (accelTriggered && soundTriggered)

        if (confirmed) {
            reset()
            return true
        }
        return false
    }

    private fun reset() {
        accelTriggered = false
        gyroTriggered = false
        soundTriggered = false
        firstTriggerTime = 0L
        soundStartTime = 0L
    }
}