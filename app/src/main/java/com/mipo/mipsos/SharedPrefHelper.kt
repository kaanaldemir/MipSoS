package com.mipo.mipsos

import android.content.Context

class SharedPrefHelper(private val context: Context) {

    private val sharedPref by lazy { context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE) }
    private val defaultMessageEn = "Emergency! Please send help to my location."
    private val defaultMessageTr = "Acil Durum! Lütfen konumuma yardım gönderin."

    fun addEmergencyContact(phoneNumber: String, emergencyContacts: MutableList<String>) {
        emergencyContacts.add(phoneNumber)
        sharedPref.edit().putStringSet("contacts", emergencyContacts.toSet()).apply()
    }

    fun getEmergencyContacts(): List<String> {
        return sharedPref.getStringSet("contacts", emptySet())?.toList() ?: emptyList()
    }

    fun getMessage(): String {
        return sharedPref.getString("sos_message", defaultMessageEn) ?: defaultMessageEn
    }

    fun saveMessage(message: String) {
        sharedPref.edit().putString("sos_message", message).apply()
    }

    fun getAutoSendState(): Boolean {
        return sharedPref.getBoolean("auto_send_state", true)
    }

    fun saveAutoSendState(state: Boolean) {
        sharedPref.edit().putBoolean("auto_send_state", state).apply()
    }

    fun getLanguage(): String {
        return sharedPref.getString("language", "en") ?: "en"
    }

    fun saveLanguage(lang: String) {
        sharedPref.edit().putString("language", lang).apply()
    }

    fun changeDefaultMessageForLanguage(lang: String) {
        val currentMessage = getMessage()
        val newMessage = if (lang == "en" && currentMessage == defaultMessageTr) {
            defaultMessageEn
        } else if (lang == "tr" && currentMessage == defaultMessageEn) {
            defaultMessageTr
        } else {
            currentMessage
        }
        saveMessage(newMessage)
    }
}
