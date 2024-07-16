package com.mipo.mipsos

import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import android.telephony.NetworkScanRequest
import android.telephony.TelephonyManager
import android.telephony.TelephonyScanManager
import android.telephony.CellInfo
import android.telephony.CellInfoLte
import android.telephony.CellInfoGsm
import android.telephony.CellInfoCdma
import android.telephony.CellInfoWcdma
import android.telephony.CellInfoNr
import android.telephony.CellIdentityNr
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors
import android.telephony.RadioAccessSpecifier

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

            startNetworkScan()
            adapter.notifyDataSetChanged()
        }
    }

    private fun startNetworkScan() {
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Log.d("APNApp", "Permission not granted")
            return
        }

        val radioAccessSpecifiers = arrayOf(
            RadioAccessSpecifier(TelephonyManager.NETWORK_TYPE_GSM, null, null),
            RadioAccessSpecifier(TelephonyManager.NETWORK_TYPE_UMTS, null, null),
            RadioAccessSpecifier(TelephonyManager.NETWORK_TYPE_LTE, null, null)
        )

        val plmnIds = arrayListOf(
            "28601", // Turkcell
            "28602", // Vodafone Türkiye
            "28603"  // Türk Telekom
        )

        val networkScanRequest = NetworkScanRequest(
            NetworkScanRequest.SCAN_TYPE_ONE_SHOT,
            radioAccessSpecifiers,
            1,
            60,
            false,
            1,
            plmnIds
        )

        val networkScanCallback = object : TelephonyScanManager.NetworkScanCallback() {
            override fun onResults(results: List<CellInfo>) {
                super.onResults(results)
                Log.d("APNApp", "Network Scan Results: ${results.size}")

                val signalStrengthMap = mutableMapOf<String, MutableList<Int>>()

                for (cellInfo in results) {
                    when (cellInfo) {
                        is CellInfoLte -> {
                            val cellSignalStrengthLte = cellInfo.cellSignalStrength
                            val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: "Unknown"
                            val mcc = cellInfo.cellIdentity.mccString?.toIntOrNull()
                            val mnc = cellInfo.cellIdentity.mncString?.toIntOrNull()
                            val providerKey = getProviderKey(mcc, mnc, networkName)
                            Log.d("APNApp", "LTE Signal Strength: ${cellSignalStrengthLte.dbm} for $providerKey")
                            signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthLte.dbm)
                        }
                        is CellInfoGsm -> {
                            val cellSignalStrengthGsm = cellInfo.cellSignalStrength
                            val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: "Unknown"
                            val mcc = cellInfo.cellIdentity.mccString?.toIntOrNull()
                            val mnc = cellInfo.cellIdentity.mncString?.toIntOrNull()
                            val providerKey = getProviderKey(mcc, mnc, networkName)
                            Log.d("APNApp", "GSM Signal Strength: ${cellSignalStrengthGsm.dbm} for $providerKey")
                            signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthGsm.dbm)
                        }
                        is CellInfoCdma -> {
                            val cellSignalStrengthCdma = cellInfo.cellSignalStrength
                            val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: "Unknown"
                            val providerKey = getProviderKey(null, null, networkName)
                            Log.d("APNApp", "CDMA Signal Strength: ${cellSignalStrengthCdma.dbm} for $providerKey")
                            signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthCdma.dbm)
                        }
                        is CellInfoWcdma -> {
                            val cellSignalStrengthWcdma = cellInfo.cellSignalStrength
                            val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: "Unknown"
                            val mcc = cellInfo.cellIdentity.mccString?.toIntOrNull()
                            val mnc = cellInfo.cellIdentity.mncString?.toIntOrNull()
                            val providerKey = getProviderKey(mcc, mnc, networkName)
                            Log.d("APNApp", "WCDMA Signal Strength: ${cellSignalStrengthWcdma.dbm} for $providerKey")
                            signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthWcdma.dbm)
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
                adapter.notifyDataSetChanged()
            }

            override fun onComplete() {
                super.onComplete()
                Log.d("APNApp", "Network Scan Complete")
            }

            override fun onError(error: Int) {
                super.onError(error)
                Log.d("APNApp", "Network Scan Error: $error")
            }
        }

        val executor = Executors.newSingleThreadExecutor()
        telephonyManager.requestNetworkScan(networkScanRequest, executor, networkScanCallback)
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
