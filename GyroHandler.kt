package com.example.silentsos

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    companion object {
        private const val AUDIO_PERMISSION_CODE = 1001
    }

    // UI
    private lateinit var statusText: TextView

    // Sensors
    private lateinit var sensorHandler: SensorHandler
    private lateinit var gyroHandler: GyroHandler
    private var soundHandler: SoundHandler? = null
    private var micRunning = false

    // Logic
    private val distressDetector = DistressDetector()

    // State
    private var countdownTimer: CountDownTimer? = null
    private var countdownRunning = false
    private var sosAlreadySent = false

    // Live values
    private var lastAccel = 0f
    private var lastGyro = 0f
    private var lastSound = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        statusText = TextView(this).apply {
            text = "SilentSOS Active\n\n🟢 MONITORING"
            textSize = 22f
            setTextColor(Color.GREEN)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }
        setContentView(statusText)

        sensorHandler = SensorHandler(this) { accel ->
            lastAccel = accel
            distressDetector.updateAccelerometer(accel)
            checkForDistress()
            updateMonitoringUI()
        }

        gyroHandler = GyroHandler(this) { gyro ->
            lastGyro = gyro
            distressDetector.updateGyroscope(gyro)
            checkForDistress()
            updateMonitoringUI()
        }

        requestAudioPermission()

        statusText.setOnClickListener {
            cancelCountdown()
        }
    }

    override fun onResume() {
        super.onResume()
        sensorHandler.start()
        gyroHandler.start()
    }

    override fun onPause() {
        super.onPause()
        sensorHandler.stop()
        gyroHandler.stop()
        soundHandler?.stop()
        micRunning = false
    }

    // ---------- Permission ----------
    private fun requestAudioPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_CODE
            )
        } else {
            startMicSafely()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (
            requestCode == AUDIO_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startMicSafely()
        }
    }

    private fun startMicSafely() {
        if (micRunning) return
        micRunning = true

        soundHandler = SoundHandler { amplitude ->
            lastSound = amplitude
            distressDetector.updateSoundLevel(amplitude)
            checkForDistress()
            updateMonitoringUI()
        }

        try {
            soundHandler?.start()
        } catch (e: Exception) {
            micRunning = false
        }
    }

    // ---------- UI ----------
    private fun updateMonitoringUI() {
        if (!countdownRunning) {
            runOnUiThread {
                statusText.clearAnimation()
                statusText.setTextColor(Color.GREEN)
                statusText.text =
                    """
                    🟢 MONITORING

                    Accel : ${"%.2f".format(lastAccel)}
                    Gyro  : ${"%.2f".format(lastGyro)}
                    Sound : $lastSound
                    """.trimIndent()
            }
        }
    }

    // ---------- Detection ----------
    private fun checkForDistress() {
        if (!countdownRunning && !sosAlreadySent &&
            distressDetector.isDistressConfirmed()
        ) {
            startCountdown()
        }
    }

    // ---------- Countdown ----------
    private fun startCountdown() {
        countdownRunning = true

        val blink = AlphaAnimation(0f, 1f).apply {
            duration = 500
            repeatMode = AlphaAnimation.REVERSE
            repeatCount = AlphaAnimation.INFINITE
        }

        runOnUiThread {
            statusText.startAnimation(blink)
        }

        countdownTimer = object : CountDownTimer(10_000, 1_000) {
            override fun onTick(ms: Long) {
                runOnUiThread {
                    statusText.setTextColor(Color.RED)
                    statusText.text =
                        "🚨 DISTRESS DETECTED 🚨\n\nSending SOS in ${ms / 1000}s\n\nTap to cancel"
                }
                playBeep()
            }

            override fun onFinish() {
                countdownRunning = false
                runOnUiThread {
                    triggerSosDemo()
                }
            }
        }.start()
    }

    private fun cancelCountdown() {
        if (countdownRunning) {
            countdownTimer?.cancel()
            countdownRunning = false
            runOnUiThread {
                statusText.clearAnimation()
                statusText.setTextColor(Color.GREEN)
                statusText.text = "🟢 MONITORING\n\nCancelled"
            }
        }
    }

    // ---------- DEMO-SAFE SOS ----------
    private fun triggerSosDemo() {
        sosAlreadySent = true
        statusText.clearAnimation()
        statusText.setTextColor(Color.RED)
        statusText.text =
            """
            🚨 SOS TRIGGERED 🚨

            Demo Mode Active
            Alert would be sent now
            """.trimIndent()
    }

    // ---------- Sound ----------
    private fun playBeep() {
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 80)
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
    }
}