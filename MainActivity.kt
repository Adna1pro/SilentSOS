package com.example.silentsos
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.graphics.drawable.ColorDrawable

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

    // Settings (APPLIED)
    private var countdownSeconds = 10
    private var sensitivityLevel = 1   // 0=Low, 1=Medium, 2=High

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

        loadSettings()

        val rootLayout = FrameLayout(this)

        statusText = TextView(this).apply {
            text = "SilentSOS Active\n\n🟢 MONITORING"
            textSize = 26f
            setTextColor(Color.GREEN)
            gravity = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        val settingsIcon = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_preferences)
            scaleX = 1.4f
            scaleY = 1.4f
            setPadding(40, 40, 40, 40)
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.TOP or Gravity.END
            ).apply {
                topMargin = 40
                rightMargin = 40
            }
            setOnClickListener {
                startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
            }
        }

        rootLayout.addView(statusText)
        rootLayout.addView(settingsIcon)
        setContentView(rootLayout)

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

        statusText.setOnClickListener { cancelCountdown() }

        requestAudioPermission()
    }

    override fun onResume() {
        super.onResume()
        loadSettings()          // 🔴 RELOAD when returning from Settings
        applySensitivity()     // 🔴 APPLY sensitivity immediately
        sensorHandler.start()
        gyroHandler.start()
        if (hasAudioPermission()) startMicSafely()
    }

    override fun onPause() {
        super.onPause()
        sensorHandler.stop()
        gyroHandler.stop()
        soundHandler?.stop()
        micRunning = false
    }

    // ---------- SETTINGS ----------
    private fun loadSettings() {
        val prefs = getSharedPreferences("SilentSOSPrefs", Context.MODE_PRIVATE)
        countdownSeconds = prefs.getInt("countdown", 10)
        sensitivityLevel = prefs.getInt("sensitivity", 1)
    }

    private fun applySensitivity() {
        when (sensitivityLevel) {
            0 -> distressDetector.setSensitivity(low = true)
            1 -> distressDetector.setSensitivity(medium = true)
            2 -> distressDetector.setSensitivity(high = true)
        }
    }

    // ---------- PERMISSIONS ----------
    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

    private fun requestAudioPermission() {
        if (!hasAudioPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                AUDIO_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == AUDIO_PERMISSION_CODE &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) startMicSafely()
    }

    // ---------- MICROPHONE ----------
    private fun startMicSafely() {
        if (micRunning) return
        micRunning = true

        soundHandler = SoundHandler { amplitude ->
            lastSound = amplitude
            distressDetector.updateAccelerometer(lastAccel + 0.5f)
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

    // ---------- DETECTION ----------
    private fun checkForDistress() {
        if (!countdownRunning && !sosAlreadySent &&
            distressDetector.isDistressConfirmed()
        ) startCountdown()
    }

    // ---------- COUNTDOWN (FIXED) ----------
    private fun startCountdown() {
        countdownRunning = true

        val blinkText = AlphaAnimation(0f, 1f).apply {
            duration = 500
            repeatMode = AlphaAnimation.REVERSE
            repeatCount = AlphaAnimation.INFINITE
        }

        val blinkBg = AlphaAnimation(0.3f, 1f).apply {
            duration = 500
            repeatMode = AlphaAnimation.REVERSE
            repeatCount = AlphaAnimation.INFINITE
        }

        runOnUiThread {
            statusText.setBackgroundColor(Color.RED)
            statusText.startAnimation(blinkText)
        }

        countdownTimer = object : CountDownTimer(countdownSeconds * 1000L, 1000) {
            override fun onTick(ms: Long) {
                runOnUiThread {
                    statusText.setTextColor(Color.WHITE)
                    statusText.text =
                        "🚨 DISTRESS DETECTED 🚨\n\nSending SOS in ${ms / 1000}s\n\nTap to cancel"
                }
                playBeep()
            }

            override fun onFinish() {
                countdownRunning = false
                runOnUiThread { triggerSosDemo() }
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
                statusText.setBackgroundColor(Color.TRANSPARENT)
            }
        }
    }

    // ---------- DEMO SAFE ----------
    private fun triggerSosDemo() {
        sosAlreadySent = true
        statusText.clearAnimation()
        statusText.setTextColor(Color.RED)
        statusText.text =
            """
            🚨 SOS TRIGGERED 🚨

            Demo Mode Active
            """.trimIndent()
    }

    private fun playBeep() {
        val tone = ToneGenerator(AudioManager.STREAM_ALARM, 80)
        tone.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 150)
    }
}