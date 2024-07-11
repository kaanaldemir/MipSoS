package com.example.mipsos

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.BufferedReader
import java.io.InputStreamReader

data class ApnInfo(val id: String, val name: String, var signalStrength: Int? = null)

class ApnSettingsActivity : AppCompatActivity() {
    private lateinit var apnsRecyclerView: RecyclerView
    private val apnList = mutableListOf<ApnInfo>()
    private val filteredApnList = mutableListOf<ApnInfo>()
    private val apnAdapter by lazy { ApnAdapter(filteredApnList.map { it.name }.toMutableList()) { apnName ->
        Toast.makeText(this, "Selected APN: $apnName", Toast.LENGTH_SHORT).show()
    } }
    private lateinit var telephonyManager: TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null

    companion object {
        private const val REQUEST_READ_PHONE_STATE = 1
        private const val TAG = "APN_FETCH"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apn_settings)

        apnsRecyclerView = findViewById(R.id.apnsRecyclerView)
        apnsRecyclerView.layoutManager = LinearLayoutManager(this)
        apnsRecyclerView.adapter = apnAdapter

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestReadPhoneStatePermission()
        } else {
            fetchAndDisplayApns()
        }
    }

    private fun requestReadPhoneStatePermission() {
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                fetchAndDisplayApns()
            } else {
                Toast.makeText(this, "Permission to read phone state denied", Toast.LENGTH_SHORT).show()
            }
        }.launch(Manifest.permission.READ_PHONE_STATE)
    }

    @SuppressLint("Range")
    private fun fetchAndDisplayApns() {
        apnList.clear()

        // Attempt to fetch APNs with root access first
        try {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = process.outputStream
            val inputStream = process.inputStream
            outputStream.write("cat /etc/apns-conf.xml\n".toByteArray())
            outputStream.flush()
            outputStream.close()

            val reader = BufferedReader(InputStreamReader(inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                line?.let {
                    if (it.contains("<apn carrier=")) {
                        val idStartIndex = it.indexOf("id=\"") + 4
                        val idEndIndex = it.indexOf("\"", idStartIndex)
                        val id = it.substring(idStartIndex, idEndIndex)

                        val nameStartIndex = it.indexOf("carrier=\"") + 9
                        val nameEndIndex = it.indexOf("\"", nameStartIndex)
                        val name = it.substring(nameStartIndex, nameEndIndex)

                        apnList.add(ApnInfo(id, name))
                    }
                }
            }
            reader.close()
            process.waitFor()

            Log.d(TAG, "Fetched ${apnList.size} APNs (Root)")
            // If successful, no need to try the non-root method
            filterApnsForTurkey()
            switchAndCheckApns(0)
            return
        } catch (e: Exception) {
            Log.e(TAG, "Root access failed or not available", e)
        }

        // If root access fails or is not available, try the non-root method
        val projection = arrayOf(Telephony.Carriers._ID, Telephony.Carriers.NAME, Telephony.Carriers.APN)
        val contentUri = Uri.withAppendedPath(Telephony.Carriers.CONTENT_URI, "current")

        try {
            contentResolver.query(contentUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val id = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers._ID)) ?: continue
                        val name = cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Carriers.NAME)) ?: continue
                        apnList.add(ApnInfo(id, name))
                    } while (cursor.moveToNext())
                } else {
                    Toast.makeText(this, "No APN settings found", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: SecurityException) {
            Toast.makeText(this, "Permission to read phone state is required to access APN information", Toast.LENGTH_LONG).show()
        }

        Log.d(TAG, "Fetched ${apnList.size} APNs (Non-Root)")
        filterApnsForTurkey()
        switchAndCheckApns(0)
    }

    private fun filterApnsForTurkey() {
        val turkishApns = listOf("Turkcell", "Vodafone Internet", "Turk Telekom") // Example
        filteredApnList.clear()
        filteredApnList.addAll(apnList.filter { it.name in turkishApns })

        Log.d(TAG, "Filtered APNs: ${filteredApnList.size}")
        filteredApnList.forEach { apn ->
            Log.d(TAG, "Filtered APN Name: ${apn.name}")
        }

        runOnUiThread {
            apnAdapter.updateApnList(filteredApnList.map { it.name })
            if (filteredApnList.isEmpty()) {
                Toast.makeText(this, "No APNs available after filtering", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun switchAndCheckApns(index: Int) {
        if (index >= filteredApnList.size) {
            updateRecyclerView()
            return
        }

        val apn = filteredApnList[index]
        setApn(apn) {
            getCurrentSignalStrength { signalStrength ->
                apn.signalStrength = signalStrength
                Log.d(TAG, "APN: ${apn.name}, Signal Strength: $signalStrength")
                switchAndCheckApns(index + 1)
            }
        }
    }

    private fun setApn(apn: ApnInfo, callback: () -> Unit) {
        try {
            val uri = Uri.parse("content://telephony/carriers")
            val values = ContentValues()
            values.put(Telephony.Carriers.APN, apn.name)

            val updateUri = Uri.withAppendedPath(uri, apn.id)
            contentResolver.update(updateUri, values, null, null)
            Toast.makeText(this, "Switched to APN: ${apn.name}", Toast.LENGTH_SHORT).show()
            callback()
        } catch (e: Exception) {
            try {
                // Attempt to set APN using root
                val process = Runtime.getRuntime().exec("su")
                val outputStream = process.outputStream
                outputStream.write("settings put global preferred_network_mode ${apn.id}\n".toByteArray())
                outputStream.flush()
                outputStream.close()
                process.waitFor()
                Toast.makeText(this, "Switched to APN (Root): ${apn.name}", Toast.LENGTH_SHORT).show()
            } catch (ex: Exception) {
                Log.e(TAG, "Failed to switch APN", ex)
            }
            callback()
        }
    }

    private fun getCurrentSignalStrength(callback: (Int) -> Unit) {
        phoneStateListener = object : PhoneStateListener() {
            override fun onSignalStrengthsChanged(signalStrength: android.telephony.SignalStrength?) {
                super.onSignalStrengthsChanged(signalStrength)
                if (signalStrength != null) {
                    val strength = signalStrength.level
                    Log.d(TAG, "Signal strength: $strength")
                    callback(strength)
                    // Unregister listener after getting the signal strength
                    telephonyManager.listen(this, PhoneStateListener.LISTEN_NONE)
                }
            }
        }
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
    }

    private fun updateRecyclerView() {
        runOnUiThread {
            filteredApnList.sortWith(compareByDescending<ApnInfo> { it.signalStrength }.thenBy { it.name })
            apnAdapter.updateApnList(filteredApnList.map { it.name })
        }
    }

    override fun onPause() {
        super.onPause()
        phoneStateListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed() // Call the superclass method to handle the back navigation
        finish() // Close the ApnSettingsActivity to return to the existing MainActivity
    }
}
