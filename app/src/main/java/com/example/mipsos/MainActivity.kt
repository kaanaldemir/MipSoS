package com.example.mipsos


import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var locationTextView: TextView
    private lateinit var recordButton: Button
    private lateinit var recordingStatusTextView: TextView
    private var mediaRecorder: MediaRecorder? = null
    private var lastLocation: Location? = null
    private lateinit var playbackButton: Button
    private var isPlaying = false
    private var mediaPlayer: MediaPlayer? = null
    private var recordingFilePath: String? = null
    private lateinit var sendButton: Button
    private lateinit var messageEditText: EditText
    private lateinit var sendStatusTextView: TextView
    private lateinit var pickContactsButton: Button
    private lateinit var apnSettingsButton: Button

    private val handler = Handler(Looper.getMainLooper())
    private val locationRunnable = object : Runnable {
        override fun run() {
            getLocation { location ->
                val latitude = location?.latitude ?: 0.0
                val longitude = location?.longitude ?: 0.0
                Log.d("LocationUpdate", "Lat: $latitude Long: $longitude")
            }
            handler.postDelayed(this, 5 * 60 * 1000) // 5 minutes
        }
    }

    companion object {
        private const val REQUEST_RECORD_AUDIO_PERMISSION = 200
        private const val PICK_CONTACT_REQUEST = 1001
        private const val MESSAGE_PREF_KEY = "sos_message"
    }

    private val emergencyContacts = mutableListOf<String>()
    private val sharedPref by lazy { getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE) }

    private fun addEmergencyContact(phoneNumber: String) {
        emergencyContacts.add(phoneNumber)
        sharedPref.edit().putStringSet("contacts", emergencyContacts.toSet()).apply()
    }

    private fun getEmergencyContacts(): List<String> {
        return sharedPref.getStringSet("contacts", emptySet())?.toList() ?: emptyList()
    }

    private fun getRecordingFilePath(): String {
        val context = applicationContext
        val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
        return if (storageDir != null && storageDir.exists()) {
            File(storageDir, "audio_recording.3gp").absolutePath
        } else {
            context.filesDir.absolutePath + "/audio_recording.3gp"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        emergencyContacts.addAll(getEmergencyContacts())

        locationTextView = findViewById(R.id.locationTextView)
        recordButton = findViewById(R.id.recordButton)
        recordingStatusTextView = findViewById(R.id.recordingStatusTextView)
        playbackButton = findViewById(R.id.playbackButton)
        messageEditText = findViewById(R.id.messageEditText)
        sendStatusTextView = findViewById(R.id.sendStatusTextView)
        sendButton = findViewById(R.id.sendButton)
        pickContactsButton = findViewById(R.id.pickContactsButton)
        apnSettingsButton = findViewById(R.id.apnSettingsButton)

        pickContactsButton.setOnClickListener {
            val intent = Intent(this, ContactPickerActivity::class.java)
            startActivityForResult(intent, PICK_CONTACT_REQUEST)
        }

        sendButton.setOnClickListener {
            sendSOSMessage()
        }

        messageEditText.setText(sharedPref.getString(MESSAGE_PREF_KEY, ""))

        val smsPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        smsPermissionRequest.launch(Manifest.permission.SEND_SMS)

        playbackButton.setOnClickListener {
            handlePlayback()
        }
        playbackButton.isEnabled = false

        recordButton.setOnClickListener {
            if (mediaRecorder == null) {
                startRecording()
            } else {
                stopRecording()
            }
        }
        apnSettingsButton.setOnClickListener {
            val intent = Intent(this, ApnSettingsActivity::class.java)
            startActivity(intent)
        }

        val locationPermissionRequest = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
            ) {
                getLocation {
                    handler.post(locationRunnable)
                }
            } else {
                locationTextView.text = "Please Enable Location!"
            }
        }

        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )


        // In onCreate()
        val apnSettingsButton: Button = findViewById(R.id.apnSettingsButton)

        apnSettingsButton.setOnClickListener {
            val intent = Intent(this, ApnSettingsActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onStop() {
        super.onStop()
        resetMediaPlayer()
        handler.removeCallbacks(locationRunnable)
        sharedPref.edit().putString(MESSAGE_PREF_KEY, messageEditText.text.toString()).apply()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startRecording()
            } else {
                Toast.makeText(this, "Audio recording permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(callback: (Location?) -> Unit) {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                val latitude = location.latitude
                val longitude = location.longitude

                locationTextView.text = "Lat: $latitude Long: $longitude"

                lastLocation = location
                locationManager.removeUpdates(this)
                callback(location)
            }

            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {
                locationTextView.text = "Searching location..."
            }

            override fun onProviderDisabled(provider: String) {
                locationTextView.text = "Please Enable Location!"
            }
        }

        val lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (lastKnownLocation != null) {
            locationListener.onLocationChanged(lastKnownLocation)
            callback(lastKnownLocation)
        } else {
            locationTextView.text = "Searching location..."
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                60000, // 1 minute
                10f,
                locationListener
            )
            locationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER,
                60000, // 1 minute
                10f,
                locationListener
            )
        }
    }

    private fun startRecording() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                REQUEST_RECORD_AUDIO_PERMISSION
            )
            return
        }

        recordingFilePath = getRecordingFilePath()
        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
            setOutputFile(recordingFilePath)
            setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)

            try {
                prepare()
                start()
                recordingStatusTextView.text = "Recording started"
                recordButton.text = "Stop"
                playbackButton.isEnabled = false // Disable playback button during recording
            } catch (e: IOException) {
                Log.e("AudioRecording", "prepare() failed: ${e.message}")
                recordingStatusTextView.text = "Recording failed"
                recordButton.text = "Record Audio"
                playbackButton.isEnabled = true // Enable playback button if recording fails
            }
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null

        recordingStatusTextView.text = "Not Recording"
        recordButton.text = "Record Audio"
        playbackButton.isEnabled = true // Re-enable playback button after recording stops

        recordingFilePath?.let { filePath ->
            if (File(filePath).exists()) {
                Toast.makeText(this, "Recording saved at: $filePath", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this, "Error: Recording file not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun resetMediaPlayer() {
        mediaPlayer?.reset()
        mediaPlayer?.release()
        mediaPlayer = null
        isPlaying = false
        playbackButton.text = "Play Recording"
    }

    private fun handlePlayback() {
        if (isPlaying) {
            mediaPlayer?.stop()
            resetMediaPlayer()
        } else {
            recordingFilePath?.let { filePath ->
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(filePath)
                    setOnCompletionListener {
                        resetMediaPlayer()
                    }
                    prepare()
                    start()
                }
                playbackButton.text = "Stop Playback"
                isPlaying = true
            }
        }
    }

    private fun sendSOSMessage() {
        val message = messageEditText.text.toString()
        val contacts = getEmergencyContacts()
        val latitude = lastLocation?.latitude ?: 0.0
        val longitude = lastLocation?.longitude ?: 0.0

        val locationMessage = "$message\n\nLocation: https://maps.google.com/?q=$latitude,$longitude"

        for (contact in contacts) {
            sendSMS(contact, locationMessage)
        }
    }

    private fun sendSMS(phoneNumber: String, message: String) {
        val sentIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent("SMS_SENT"),
            PendingIntent.FLAG_IMMUTABLE
        )

        val deliveredIntent = PendingIntent.getBroadcast(
            this,
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

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (resultCode) {
                    RESULT_OK -> sendStatusTextView.text = "SMS sent successfully"
                    SmsManager.RESULT_ERROR_GENERIC_FAILURE -> sendStatusTextView.text = "Failed to send SMS"
                    SmsManager.RESULT_ERROR_NO_SERVICE -> sendStatusTextView.text = "No service"
                    SmsManager.RESULT_ERROR_NULL_PDU -> sendStatusTextView.text = "Null PDU"
                    SmsManager.RESULT_ERROR_RADIO_OFF -> sendStatusTextView.text = "Radio off"
                }
            }
        }, IntentFilter("SMS_SENT"))

        registerReceiver(object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (resultCode) {
                    RESULT_OK -> sendStatusTextView.text = "SMS delivered"
                    RESULT_CANCELED -> sendStatusTextView.text = "SMS not delivered"
                }
            }
        }, IntentFilter("SMS_DELIVERED"))
    }
}
