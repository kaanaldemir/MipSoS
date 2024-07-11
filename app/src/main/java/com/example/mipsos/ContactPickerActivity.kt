package com.example.mipsos

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ContactPickerActivity : AppCompatActivity() {
    private val sharedPref by lazy { getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE) }
    private lateinit var contactsRecyclerView: RecyclerView
    private val emergencyContacts = mutableListOf<String>()
    private val contactsAdapter by lazy { ContactsAdapter(emergencyContacts, this) }


    companion object {
        private const val REQUEST_READ_CONTACTS_PERMISSION = 300
        private const val PICK_CONTACT_REQUEST = 1001
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri = result.data?.data ?: return@registerForActivityResult
            handleContactSelection(contactUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact_picker)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        emergencyContacts.addAll(getEmergencyContacts()) // Load saved contacts
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView)

        // Set up RecyclerView
        contactsRecyclerView.adapter = contactsAdapter
        contactsRecyclerView.layoutManager = LinearLayoutManager(this)
        contactsRecyclerView.adapter?.notifyDataSetChanged() // Refresh RecyclerView
        contactsRecyclerView.adapter = ContactsAdapter(emergencyContacts, this)


        val pickContactsButton: Button = findViewById(R.id.pickContactsButton)
        pickContactsButton.setOnClickListener {
            pickContacts()
        }
    }

    private fun pickContacts() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_CONTACTS), REQUEST_READ_CONTACTS_PERMISSION)
        } else {
            val pickContactIntent = Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI)
            pickContactIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            resultLauncher.launch(pickContactIntent)
        }
    }

    @SuppressLint("Range")
    private fun handleContactSelection(contactUri: Uri) {
        val cursor = contentResolver.query(contactUri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val contactId = it.getString(it.getColumnIndex(ContactsContract.Contacts._ID))
                val name = it.getString(it.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME))
                val hasPhone =
                    it.getString(it.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)).toInt()

                if (hasPhone > 0) {
                    val phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        arrayOf(contactId),
                        null
                    )
                    phoneCursor?.use { phone ->
                        if (phone.moveToFirst()) {
                            val phoneNumber = phone.getString(phone.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
                            if(!emergencyContacts.contains(phoneNumber)){ // Check if the number is already in the list
                                addEmergencyContact(phoneNumber)
                            }
                        }
                    }
                } else {
                    Toast.makeText(this, "Contact has no phone number", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }


    private fun addEmergencyContact(phoneNumber: String) {
        emergencyContacts.add(phoneNumber)
        sharedPref.edit().putStringSet("contacts", emergencyContacts.toSet()).apply()
        Log.d("EmergencyContacts", "Added contact: $phoneNumber")

        // Update the RecyclerView
        contactsRecyclerView.adapter?.notifyItemInserted(emergencyContacts.size - 1)
    }

    @SuppressLint("ApplySharedPref")
    private fun getEmergencyContacts(): Set<String> {
        return sharedPref.getStringSet("contacts", emptySet()) ?: emptySet()
    }

    // ... onRequestPermissionsResult
}


