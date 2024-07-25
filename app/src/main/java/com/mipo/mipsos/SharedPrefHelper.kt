package com.mipo.mipsos

import android.content.Context

class SharedPrefHelper(private val context: Context) {

    private val sharedPref by lazy { context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE) }
    private val defaultMessage = "Emergency! Please send help to my location."

    fun addEmergencyContact(phoneNumber: String, emergencyContacts: MutableList<String>) {
        emergencyContacts.add(phoneNumber)
        sharedPref.edit().putStringSet("contacts", emergencyContacts.toSet()).apply()
    }

    fun getEmergencyContacts(): List<String> {
        return sharedPref.getStringSet("contacts", emptySet())?.toList() ?: emptyList()
    }

    fun getMessage(): String {
        return sharedPref.getString("sos_message", defaultMessage) ?: defaultMessage
    }

    fun saveMessage(message: String) {
        sharedPref.edit().putString("sos_message", message).apply()
    }
}
