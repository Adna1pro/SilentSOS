package com.example.silentsos

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

class SensorHandler(
    context: Context,
    private val onMotionDetected: (Float) -> Unit
) : SensorEventListener {

    // Access to phone sensors
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // Accelerometer sensor
    private val accelerometer =
        sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    // Start listening to sensor
    fun start() {
        sensorManager.registerListener(
            this,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    // Stop listening to sensor (battery saving)
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    // Called automatically when sensor value changes
    override fun onSensorChanged(event: SensorEvent) {
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]

        // Calculate acceleration magnitude
        val magnitude = sqrt(x * x + y * y + z * z)

        // Send value back to MainActivity
        onMotionDetected(magnitude)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this project
    }
}