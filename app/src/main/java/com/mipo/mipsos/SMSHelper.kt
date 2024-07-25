package com.mipo.mipsos

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.SmsManager
import android.widget.TextView

class SmsHelper(private val context: Context, private val sendStatusTextView: TextView) {

    fun sendSMS(phoneNumber: String, message: String) {
        val sentIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent("SMS_SENT"),
            PendingIntent.FLAG_IMMUTABLE
        )

        val deliveredIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent("SMS_DELIVERED"),
            PendingIntent.FLAG_IMMUTABLE
        )

        val smsManager = SmsManager.getDefault()
        val parts = smsManager.divideMessage(message)
        smsManager.sendMultipartTextMessage(
            phoneNumber,
            null,
            parts,
            arrayListOf(sentIntent, deliveredIntent),
            null
        )

        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (resultCode) {
                    android.app.Activity.RESULT_OK -> sendStatusTextView.text = "SMS sent successfully"
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> sendStatusTextView.text = "Failed to send SMS"
                    SmsManager.RESULT_ERROR_NO_SERVICE -> sendStatusTextView.text = "No service"
                    SmsManager.RESULT_ERROR_NULL_PDU -> sendStatusTextView.text = "Null PDU"
                    SmsManager.RESULT_ERROR_RADIO_OFF -> sendStatusTextView.text = "Radio off"
                }
            }
        }, IntentFilter("SMS_SENT"))

        context.registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (resultCode) {
                    android.app.Activity.RESULT_OK -> sendStatusTextView.text = "SMS delivered"
                    android.app.Activity.RESULT_CANCELED -> sendStatusTextView.text = "SMS not delivered"
                }
            }
        }, IntentFilter("SMS_DELIVERED"))
    }
}
