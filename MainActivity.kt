package com.example.silentsos

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.silentsos.DistressDetector

class MainActivity : ComponentActivity() {

    // UI component
    private lateinit var statusText: TextView

    // Core components
    private lateinit var sensorHandler: SensorHandler
    private val distressDetector = DistressDetector()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Create simple UI
        statusText = TextView(this)
        statusText.text = "SilentSOS Active\nMonitoring..."
        statusText.textSize = 22f
        setContentView(statusText)

        // 2. Initialize sensor handler
        sensorHandler = SensorHandler(this) { acceleration ->

            // 3. Decision logic
            if (distressDetector.isDistress(acceleration)) {
                updateStatus(
                    "🚨 DISTRESS DETECTED!\nAcceleration: %.2f".format(acceleration)
                )

                // (Later we will trigger SOS here)

            } else {
                updateStatus(
                    "Monitoring...\nAcceleration: %.2f".format(acceleration)
                )
            }
        }
    }

    // Called when app comes to foreground
    override fun onResume() {
        super.onResume()
        sensorHandler.start()
    }

    // Called when app goes to background
    override fun onPause() {
        super.onPause()
        sensorHandler.stop()
    }

    // Helper function to update UI safely
    private fun updateStatus(message: String) {
        runOnUiThread {
            statusText.text = message
        }
    }
}
