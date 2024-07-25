package com.mipo.mipsos

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    private lateinit var requestPermissionsLauncher: ActivityResultLauncher<Array<String>>
    private lateinit var appSettingsLauncher: ActivityResultLauncher<Intent>

    private lateinit var locationTextView: TextView
    private lateinit var recordButton: Button
    private lateinit var recordingStatusTextView: TextView
    private lateinit var playbackButton: Button
    private lateinit var sendButton: MaterialButton
    private lateinit var messageEditText: EditText
    private lateinit var sendStatusTextView: TextView
    private lateinit var pickContactsButton: Button
    private lateinit var apnSettingsButton: Button
    private lateinit var autoSendCheckbox: CheckBox

    private lateinit var locationHelper: LocationHelper
    private lateinit var soundRecorder: SoundRecorder
    private lateinit var smsHelper: SmsHelper
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var dialogHelper: DialogHelper
    private lateinit var sharedPrefHelper: SharedPrefHelper

    private var sosTimer: CountDownTimer? = null

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        private const val PICK_CONTACT_REQUEST = 1001
        private const val MESSAGE_PREF_KEY = "sos_message"
    }

    private val emergencyContacts = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        // Initialize UI elements first
        locationTextView = findViewById(R.id.locationTextView)
        recordButton = findViewById(R.id.recordButton)
        recordingStatusTextView = findViewById(R.id.recordingStatusTextView)
        playbackButton = findViewById(R.id.playbackButton)
        messageEditText = findViewById(R.id.messageEditText)
        sendStatusTextView = findViewById(R.id.sendStatusTextView)
        sendButton = findViewById(R.id.sendButton)
        pickContactsButton = findViewById(R.id.pickContactsButton)
        apnSettingsButton = findViewById(R.id.apnSettingsButton)
        autoSendCheckbox = findViewById(R.id.autoSendCheckbox)

        // Initialize sharedPrefHelper
        sharedPrefHelper = SharedPrefHelper(this)

        // Initialize other helpers
        locationHelper = LocationHelper(this, locationTextView)
        soundRecorder = SoundRecorder(this, recordingStatusTextView, recordButton, playbackButton)
        smsHelper = SmsHelper(this, sendStatusTextView)
        dialogHelper = DialogHelper(this)

        // Initialize launchers
        requestPermissionsLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val granted = permissions.entries.all { it.value }
            if (granted) {
                initializeApp()
            } else {
                permissionHelper.handleDeniedPermissions(permissions)
            }
        }

        appSettingsLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            // Re-check permissions when returning from app settings
            permissionHelper.checkAndRequestPermissions()
        }

        // Initialize permissionHelper
        permissionHelper = PermissionHelper(
            this,
            requestPermissionsLauncher,
            appSettingsLauncher,
            this::initializeApp,
            this::handleDeniedPermissions
        )

        emergencyContacts.addAll(getEmergencyContacts())

        updateSendButtonState()
        updateAutoSendCheckboxState()

        pickContactsButton.setOnClickListener {
            val intent = Intent(this, ContactPickerActivity::class.java)
            startActivityForResult(intent, PICK_CONTACT_REQUEST)
        }

        sendButton.setOnClickListener {
            sendSOSMessage()
        }

        messageEditText.setText(sharedPrefHelper.getMessage())

        playbackButton.setOnClickListener {
            soundRecorder.handlePlayback()
        }
        playbackButton.isEnabled = false

        recordButton.setOnClickListener {
            soundRecorder.startRecording()
        }
        apnSettingsButton.setOnClickListener {
            val intent = Intent(this, ApnSettingsActivity::class.java)
            startActivity(intent)
        }

        if (emergencyContacts.isEmpty()) {
            // Show permission explanation dialog before requesting permissions
            dialogHelper.showPermissionExplanationDialog(
                onProceed = { permissionHelper.checkAndRequestPermissions() },
                onExit = { finish() }
            )
        } else {
            // Directly request permissions without showing the explanation dialog
            permissionHelper.checkAndRequestPermissions()
        }

        if (autoSendCheckbox.isChecked && autoSendCheckbox.isEnabled) {
            startSOSTimer()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACT_REQUEST) {
            emergencyContacts.clear()
            emergencyContacts.addAll(getEmergencyContacts())
            updateSendButtonState()
            updateAutoSendCheckboxState()
        }
    }

    private fun updateSendButtonState() {
        if (emergencyContacts.isEmpty()) {
            sendButton.isEnabled = false
            sendButton.text = "Add Contact First"
            sendButton.setStrokeColorResource(android.R.color.darker_gray)
            sendButton.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        } else {
            sendButton.isEnabled = true
            sendButton.text = "Send SOS"
            sendButton.setStrokeColorResource(R.color.secondary_color)
            sendButton.setTextColor(ContextCompat.getColor(this, R.color.secondary_color))
        }
    }

    private fun updateAutoSendCheckboxState() {
        autoSendCheckbox.isEnabled = emergencyContacts.isNotEmpty()
        autoSendCheckbox.isChecked = emergencyContacts.isNotEmpty()
        autoSendCheckbox.alpha = if (emergencyContacts.isEmpty()) 0.5f else 1.0f
    }

    private fun startSOSTimer() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_countdown, null)
        val countdownTextView: TextView = dialogView.findViewById(R.id.countdownTextView)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle("Automatic SOS")
            .setMessage("SOS message will be sent automatically in 10 seconds. You can cancel it.")
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton("Cancel") { dialog, _ ->
                sosTimer?.cancel()
                dialog.dismiss()
            }
            .create()

        sosTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownTextView.text = "Sending SOS in: ${millisUntilFinished / 1000} seconds"
            }

            override fun onFinish() {
                alertDialog.dismiss()
                sendSOSMessage()
            }
        }
        sosTimer?.start()
        alertDialog.show()
    }

    private fun initializeApp() {
        locationHelper.startLocationUpdates()
    }

    private fun getEmergencyContacts(): List<String> {
        return sharedPrefHelper.getEmergencyContacts()
    }

    private fun sendSOSMessage() {
        val message = messageEditText.text.toString()
        val contacts = sharedPrefHelper.getEmergencyContacts()
        val location = locationHelper.getLastLocation()
        val locationMessage = if (location != null && location.latitude != 0.0 && location.longitude != 0.0) {
            "\n\nLocation: https://maps.google.com/?q=${location.latitude},${location.longitude}"
        } else {
            ""
        }

        for (contact in contacts) {
            smsHelper.sendSMS(contact, message + locationMessage)
        }
    }

    private fun handleDeniedPermissions(permissions: Map<String, Boolean>) {
        val deniedPermissions = permissions.filterValues { !it }
        val permanentlyDeniedPermissions = deniedPermissions.keys.filter { permission ->
            !ActivityCompat.shouldShowRequestPermissionRationale(this, permission)
        }

        if (permanentlyDeniedPermissions.isNotEmpty()) {
            dialogHelper.showAppSettingsDialog(
                permanentlyDeniedPermissions,
                this::getPermissionFriendlyNames,
                appSettingsLauncher,
                { finish() }
            )
        } else {
            dialogHelper.showPermissionDeniedDialog(
                deniedPermissions.keys,
                this::getPermissionFriendlyNames,
                { permissionHelper.checkAndRequestPermissions() },
                { finish() }
            )
        }
    }

    private fun getPermissionFriendlyNames(permissions: Set<String>): List<String> {
        val friendlyNames = mutableSetOf<String>()
        var includesLocation = false

        permissions.forEach { permission ->
            when (permission) {
                Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION -> includesLocation = true
                Manifest.permission.RECORD_AUDIO -> friendlyNames.add("Microphone")
                Manifest.permission.SEND_SMS -> friendlyNames.add("SMS")
                else -> friendlyNames.add(permission)
            }
        }

        if (includesLocation) {
            friendlyNames.add("Location")
        }

        return friendlyNames.toList()
    }

    override fun onStop() {
        super.onStop()
        soundRecorder.resetPlayer()
        locationHelper.stopLocationUpdates()
        sharedPrefHelper.saveMessage(messageEditText.text.toString())
        sosTimer?.cancel()
    }
}
