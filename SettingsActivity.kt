package com.example.silentsos

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import androidx.activity.ComponentActivity

class SettingsActivity : ComponntActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("SilentSOSPrefs", Context.MODE_PRIVATE)

        // Root layout
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(40, 40, 40, 40)
        }

        // Title
        val title = TextView(this).apply {
            text = "SilentSOS Settings"
            textSize = 24f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
        }

        // Countdown setting
        val countdownLabel = TextView(this).apply {
            text = "Cancel Countdown (seconds)"
            textSize = 16f
        }

        val countdownInput = EditText(this).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            setText(prefs.getInt("countdown", 10).toString())
            gravity = Gravity.CENTER
        }

        // Sensitivity spinner
        val sensitivityLabel = TextView(this).apply {
            text = "SOS Sensitivity"
            textSize = 16f
        }

        val spinner = Spinner(this)
        val options = arrayOf("Low", "Medium", "High")
        spinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            options
        )
        spinner.setSelection(prefs.getInt("sensitivity", 1))

        // Save button
        val saveBtn = Button(this).apply {
            text = "Save Settings"
            setOnClickListener {
                val countdown = countdownInput.text.toString().toIntOrNull() ?: 10
                val sensitivity = spinner.selectedItemPosition

                prefs.edit()
                    .putInt("countdown", countdown)
                    .putInt("sensitivity", sensitivity)
                    .apply()

                Toast.makeText(
                    this@SettingsActivity,
                    "Settings Saved",
                    Toast.LENGTH_SHORT
                ).show()

                finish()
            }
        }

        // Add views
        layout.addView(title)
        layout.addView(countdownLabel)
        layout.addView(countdownInput)
        layout.addView(sensitivityLabel)
        layout.addView(spinner)
        layout.addView(saveBtn)

        setContentView(layout)
    }
}