package com.example.silentsos

class DistressDetector {

    companion object {
        // Impact thresholds
        const val ACCEL_THRESHOLD = 21.0f     // Strong linear impact (unchanged)

        // 🔄 Gyroscope threshold (slightly more sensitive for testing)
        const val GYRO_THRESHOLD = 4.0f       // Sudden rotation

        // 🔊 Sound threshold (reduced to trigger above normal speech)
        // Normal speech ~2000–5000, loud shout/clap ~7000+
        const val SOUND_THRESHOLD = 5000     // Loud voice / clap / bang

        // Time window for sensor fusion
        const val TIME_WINDOW_MS = 2000L      // 2 seconds
    }

    // Sensor state flags
    private var accelTriggered = false
    private var gyroTriggered = false
    private var soundTriggered = false
    private var firstTriggerTime: Long = 0

    // --------- SENSOR UPDATES ---------

    fun updateAccelerometer(acceleration: Float) {
        if (acceleration >= ACCEL_THRESHOLD) {
            registerTrigger { accelTriggered = true }
        }
    }

    fun updateGyroscope(rotation: Float) {
        if (rotation >= GYRO_THRESHOLD) {
            registerTrigger { gyroTriggered = true }
        }
    }

    fun updateSoundLevel(amplitude: Int) {
        if (amplitude >= SOUND_THRESHOLD) {
            registerTrigger { soundTriggered = true }
        }
    }

    // --------- CORE LOGIC ---------

    private fun registerTrigger(onTrigger: () -> Unit) {
        val now = System.currentTimeMillis()

        if (firstTriggerTime == 0L) {
            firstTriggerTime = now
        }

        if (now - firstTriggerTime <= TIME_WINDOW_MS) {
            onTrigger()
        } else {
            reset()
        }
    }

    /**
     * Distress confirmed when:
     * - Accelerometer + Gyroscope
     * OR
     * - Accelerometer + Loud Sound
     */
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
        firstTriggerTime = 0
    }
}