package com.mipo.mipsos

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.util.*

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
    private lateinit var languageSwitchButton: AppCompatImageButton

    private lateinit var locationHelper: LocationHelper
    private lateinit var soundRecorder: SoundRecorder
    private lateinit var smsHelper: SmsHelper
    private lateinit var permissionHelper: PermissionHelper
    private lateinit var dialogHelper: DialogHelper
    private lateinit var sharedPrefHelper: SharedPrefHelper
    private lateinit var messageHelper: MessageHelper

    private var sosTimer: CountDownTimer? = null

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        private const val PICK_CONTACT_REQUEST = 1001
        private const val MESSAGE_PREF_KEY = "sos_message"
        private const val ENABLE_LOCATION_REQUEST = 1002
    }

    private val emergencyContacts = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPrefHelper = SharedPrefHelper(this)
        messageHelper = MessageHelper(this)
        loadLocale()
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
        languageSwitchButton = findViewById(R.id.languageSwitchButton)

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
                if (!locationHelper.isLocationEnabled()) {
                    promptEnableLocation()
                } else {
                    initializeApp()
                }
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
            {
                if (!locationHelper.isLocationEnabled()) {
                    promptEnableLocation()
                } else {
                    initializeApp()
                }
            },
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

        autoSendCheckbox.setOnCheckedChangeListener { _, isChecked ->
            Log.d("MainActivity", "AutoSendCheckbox state changed to: $isChecked")
            sharedPrefHelper.saveAutoSendState(isChecked)
        }

        languageSwitchButton.setOnClickListener {
            showLanguageChangeDialog()
        }

        // Load user-edited message
        val userEditedMessage = messageHelper.getMessage()
        if (userEditedMessage.isNullOrEmpty()) {
            // Set default message if no user-edited message is found
            val defaultMessage = getString(R.string.default_message)
            messageEditText.setText(defaultMessage)
            messageHelper.saveMessage(defaultMessage)
        } else {
            messageEditText.setText(userEditedMessage)
        }

        // Save user-edited message when text changes
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                messageHelper.saveMessage(s.toString())
            }
        })

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

        if (sharedPrefHelper.getAutoSendState() && autoSendCheckbox.isEnabled) {
            startSOSTimer()
        }

        // Request location update as soon as possible
        locationHelper.getLocation { location ->
            val latitude = location?.latitude ?: 0.0
            val longitude = location?.longitude ?: 0.0
            locationTextView.text = "${getString(R.string.lat)} $latitude ${getString(R.string.longt)} $longitude"
        }
    }

    private fun promptEnableLocation() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.enable_location))
            .setMessage(getString(R.string.enable_location_message))
            .setCancelable(false)
            .setPositiveButton(getString(R.string.enable)) { dialog, _ ->
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivityForResult(intent, ENABLE_LOCATION_REQUEST)
            }
            .setNegativeButton(getString(R.string.proceed_without_location)) { dialog, _ ->
                dialog.dismiss()
                initializeApp()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACT_REQUEST) {
            emergencyContacts.clear()
            emergencyContacts.addAll(getEmergencyContacts())
            updateSendButtonState()
            updateAutoSendCheckboxState()
        } else if (requestCode == ENABLE_LOCATION_REQUEST) {
            if (locationHelper.isLocationEnabled()) {
                initializeApp()
            } else {
                Toast.makeText(this, getString(R.string.location_not_enabled), Toast.LENGTH_LONG).show()
                initializeApp()
            }
        }
    }

    private fun updateSendButtonState() {
        if (emergencyContacts.isEmpty()) {
            sendButton.isEnabled = false
            sendButton.text = getString(R.string.add_contact_first)
            sendButton.setStrokeColorResource(android.R.color.darker_gray)
            sendButton.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
        } else {
            sendButton.isEnabled = true
            sendButton.text = getString(R.string.send_sos)
            sendButton.setStrokeColorResource(R.color.secondary_color)
            sendButton.setTextColor(ContextCompat.getColor(this, R.color.secondary_color))
        }
        Log.d("MainActivity", "SendButton state updated. isEnabled: ${sendButton.isEnabled}")
    }

    private fun updateAutoSendCheckboxState() {
        val autoSendState = sharedPrefHelper.getAutoSendState()
        Log.d("MainActivity", "AutoSendCheckbox initial state: $autoSendState")
        autoSendCheckbox.isEnabled = emergencyContacts.isNotEmpty()
        autoSendCheckbox.isChecked = autoSendState && emergencyContacts.isNotEmpty()
        Log.d("MainActivity", "AutoSendCheckbox isEnabled: ${autoSendCheckbox.isEnabled}, isChecked: ${autoSendCheckbox.isChecked}")
        autoSendCheckbox.alpha = if (emergencyContacts.isEmpty()) 0.5f else 1.0f
    }

    private fun startSOSTimer() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_countdown, null)
        val countdownTextView: TextView = dialogView.findViewById(R.id.countdownTextView)

        val alertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.automatic_sos))
            .setMessage(getString(R.string.automatic_sos_message))
            .setView(dialogView)
            .setCancelable(false)
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                sosTimer?.cancel()
                dialog.dismiss()
            }
            .create()

        sosTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                countdownTextView.text = "${getString(R.string.sending_sos_in)} ${millisUntilFinished / 1000} ${getString(R.string.seconds)}"
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
            "\n\n${getString(R.string.location)}: https://maps.google.com/?q=${location.latitude},${location.longitude}"
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
                Manifest.permission.RECORD_AUDIO -> friendlyNames.add(getString(R.string.microphone))
                Manifest.permission.SEND_SMS -> friendlyNames.add(getString(R.string.sms))
                else -> friendlyNames.add(permission)
            }
        }

        if (includesLocation) {
            friendlyNames.add(getString(R.string.location))
        }

        return friendlyNames.toList()
    }

    override fun onStop() {
        super.onStop()
        soundRecorder.resetPlayer()
        locationHelper.stopLocationUpdates()
        messageHelper.saveMessage(messageEditText.text.toString())
        sosTimer?.cancel()
    }

    private fun showLanguageChangeDialog() {
        val languages = arrayOf("English", "Türkçe", "العربية")
        val languageCodes = arrayOf("en", "tr", "ar")
        val currentLangCode = sharedPrefHelper.getLanguage()
        val currentLangIndex = languageCodes.indexOf(currentLangCode)

        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.change_language))
            .setSingleChoiceItems(languages, currentLangIndex) { dialog, which ->
                val selectedLanguage = languageCodes[which]
                val defaultMessageEn = "Emergency! Please send help to my location."
                val defaultMessageTr = "Acil Durum! Lütfen konumuma yardım gönderin."
                val defaultMessageAr = "حالة طوارئ! يرجى إرسال المساعدة إلى موقعي."

                val currentMessage = messageEditText.text.toString()
                if (currentMessage == defaultMessageEn || currentMessage == defaultMessageTr || currentMessage == defaultMessageAr || currentMessage.isEmpty()) {
                    val newDefaultMessage = when (selectedLanguage) {
                        "en" -> defaultMessageEn
                        "tr" -> defaultMessageTr
                        "ar" -> defaultMessageAr
                        else -> defaultMessageEn
                    }
                    messageEditText.setText(newDefaultMessage)
                    messageHelper.saveMessage(newDefaultMessage)
                }

                sharedPrefHelper.saveLanguage(selectedLanguage)
                dialog.dismiss()
                restartApp()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
        val alertDialog = builder.create()
        alertDialog.show()
    }

    private fun restartApp() {
        val intent = Intent(this, SplashActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun loadLocale() {
        val lang = sharedPrefHelper.getLanguage()
        setLocale(lang)
    }

    private fun setLocale(lang: String) {
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
        sharedPrefHelper.saveLanguage(lang)
    }
}
