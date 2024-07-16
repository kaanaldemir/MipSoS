package com.mipo.mipsos

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.telephony.*
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ApnSettingsActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ApnAdapter
    private val apnList = mutableListOf<Apn>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_apn_settings)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ApnAdapter(apnList)
        recyclerView.adapter = adapter

        loadApns()
    }

    private fun loadApns() {
        val uri: Uri = Uri.parse("content://telephony/carriers")
        val projection = arrayOf(
            Telephony.Carriers._ID,
            Telephony.Carriers.NAME,
            Telephony.Carriers.APN
        )

        val cursor: Cursor? = contentResolver.query(uri, projection, null, null, null)
        cursor?.use {
            val idIndex = it.getColumnIndexOrThrow(Telephony.Carriers._ID)
            val nameIndex = it.getColumnIndexOrThrow(Telephony.Carriers.NAME)
            val apnIndex = it.getColumnIndexOrThrow(Telephony.Carriers.APN)

            while (it.moveToNext()) {
                val id = it.getLong(idIndex)
                val name = it.getString(nameIndex)
                val apn = it.getString(apnIndex)
                apnList.add(Apn(id, name, apn))
            }

            getSignalStrength()
            adapter.notifyDataSetChanged()
        }
    }

    private fun getSignalStrength() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Log.d("APNApp", "Permission not granted")
            return
        }

        val cellInfoList = telephonyManager.allCellInfo
        Log.d("APNApp", "Cell Info List Size: ${cellInfoList.size}")

        val signalStrengthMap = mutableMapOf<String, MutableList<Int>>()

        for (cellInfo in cellInfoList) {
            when (cellInfo) {
                is CellInfoLte -> {
                    val cellSignalStrengthLte = cellInfo.cellSignalStrength
                    val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: "Unknown"
                    val mcc = cellInfo.cellIdentity.mccString?.toIntOrNull()
                    val mnc = cellInfo.cellIdentity.mncString?.toIntOrNull()
                    val providerKey = getProviderKey(mcc, mnc, networkName)
                    Log.d("APNApp", "LTE Signal Strength: ${cellSignalStrengthLte.dbm} for $providerKey")
                    Log.d("APNApp", "Cell Identity LTE: ${cellInfo.cellIdentity}")
                    signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthLte.dbm)
                }
                is CellInfoGsm -> {
                    val cellSignalStrengthGsm = cellInfo.cellSignalStrength
                    val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: "Unknown"
                    val mcc = cellInfo.cellIdentity.mccString?.toIntOrNull()
                    val mnc = cellInfo.cellIdentity.mncString?.toIntOrNull()
                    val providerKey = getProviderKey(mcc, mnc, networkName)
                    Log.d("APNApp", "GSM Signal Strength: ${cellSignalStrengthGsm.dbm} for $providerKey")
                    Log.d("APNApp", "Cell Identity GSM: ${cellInfo.cellIdentity}")
                    signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthGsm.dbm)
                }
                is CellInfoCdma -> {
                    val cellSignalStrengthCdma = cellInfo.cellSignalStrength
                    val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: "Unknown"
                    val providerKey = getProviderKey(null, null, networkName)
                    Log.d("APNApp", "CDMA Signal Strength: ${cellSignalStrengthCdma.dbm} for $providerKey")
                    Log.d("APNApp", "Cell Identity CDMA: ${cellInfo.cellIdentity}")
                    signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthCdma.dbm)
                }
                is CellInfoWcdma -> {
                    val cellSignalStrengthWcdma = cellInfo.cellSignalStrength
                    val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: "Unknown"
                    val mcc = cellInfo.cellIdentity.mccString?.toIntOrNull()
                    val mnc = cellInfo.cellIdentity.mncString?.toIntOrNull()
                    val providerKey = getProviderKey(mcc, mnc, networkName)
                    Log.d("APNApp", "WCDMA Signal Strength: ${cellSignalStrengthWcdma.dbm} for $providerKey")
                    Log.d("APNApp", "Cell Identity WCDMA: ${cellInfo.cellIdentity} for $providerKey")
                    signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthWcdma.dbm)
                }
                is CellInfoNr -> {
                    val cellSignalStrengthNr = cellInfo.cellSignalStrength
                    val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: "Unknown"
                    val cellIdentityNr = cellInfo.cellIdentity as android.telephony.CellIdentityNr
                    val mcc = cellIdentityNr.mccString?.toIntOrNull()
                    val mnc = cellIdentityNr.mncString?.toIntOrNull()
                    val providerKey = getProviderKey(mcc, mnc, networkName)
                    Log.d("APNApp", "NR Signal Strength: ${cellSignalStrengthNr.dbm} for $providerKey")
                    Log.d("APNApp", "Cell Identity NR: ${cellInfo.cellIdentity}")
                    signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthNr.dbm)
                }
                else -> {
                    Log.d("APNApp", "Unknown cell info type: ${cellInfo.javaClass}")
                }
            }
        }

        // Assign signal strength to APNs based on the provider key
        for (apn in apnList) {
            val providerKey = getProviderKeyForApn(apn.name)
            val signalStrengths = signalStrengthMap[providerKey]
            if (signalStrengths != null && signalStrengths.isNotEmpty()) {
                apn.signalStrength = signalStrengths.average().toInt()
            } else {
                apn.signalStrength = -120 // Default value for no signal
            }
            if (apn.signalStrength != -120) {
                Log.d("APNApp", "APN: ${apn.name} assigned signal strength: ${apn.signalStrength}")
            }
        }

        apnList.sortByDescending { it.signalStrength }
    }

    private fun getProviderKey(mcc: Int?, mnc: Int?, networkName: String): String {
        return when {
            mcc == 286 && mnc == 1 -> "Turkcell"
            mcc == 286 && mnc == 2 -> "Vodafone Türkiye"
            mcc == 286 && mnc == 3 -> "Türk Telekom"
            else -> networkName
        }
    }

    private fun getProviderKeyForApn(apnName: String): String {
        return when {
            apnName.contains("Turkcell", ignoreCase = true) -> "Turkcell"
            apnName.contains("Vodafone", ignoreCase = true) -> "Vodafone Türkiye"
            apnName.contains("Türk Telekom", ignoreCase = true) -> "Türk Telekom"
            else -> "Unknown"
        }
    }
}
