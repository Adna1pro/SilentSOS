package com.example.silentsos

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.SmsManager

class SosManager(private val context: Context) {

    // Send SOS SMS
    fun sendSosSms(phoneNumber: String, message: String) {
        val smsManager = SmsManager.getDefault()
        smsManager.sendTextMessage(
            phoneNumber,
            null,
            message,
            null,
            null
        )
    }

    // Initiate emergency call
    fun makeEmergencyCall(phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_CALL).apply {
            data = Uri.parse("tel:$phoneNumber")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(callIntent)
    }
}