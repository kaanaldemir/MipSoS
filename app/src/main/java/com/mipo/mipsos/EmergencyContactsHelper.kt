package com.mipo.mipsos

import android.content.Context

class EmergencyContactsHelper(private val context: Context) {

    private val sharedPref by lazy { context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE) }

    fun addEmergencyContact(phoneNumber: String, emergencyContacts: MutableList<String>) {
        emergencyContacts.add(phoneNumber)
        sharedPref.edit().putStringSet("contacts", emergencyContacts.toSet()).apply()
    }

    fun getEmergencyContacts(): List<String> {
        return sharedPref.getStringSet("contacts", emptySet())?.toList() ?: emptyList()
    }
}
