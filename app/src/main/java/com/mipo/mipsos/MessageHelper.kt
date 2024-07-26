package com.mipo.mipsos

import android.content.Context

class MessageHelper(private val context: Context) {

    private val sharedPref by lazy { context.getSharedPreferences("message_prefs", Context.MODE_PRIVATE) }
    private val defaultMessageEn = "Emergency! Please send help to my location."
    private val defaultMessageTr = "Acil Durum! Lütfen konumuma yardım gönderin."

    fun getMessage(): String {
        return sharedPref.getString("sos_message", defaultMessageEn) ?: defaultMessageEn
    }

    fun saveMessage(message: String) {
        sharedPref.edit().putString("sos_message", message).apply()
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
