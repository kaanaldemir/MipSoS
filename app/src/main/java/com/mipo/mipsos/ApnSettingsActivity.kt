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
import android.os.Handler
import android.os.Looper
import java.util.concurrent.Executors

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
        val telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
        if (telephonyManager == null) {
            Log.e("APNApp", getString(R.string.telephony_manager_null))
            return
        }

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            Log.d("APNApp", getString(R.string.permission_not_granted))
            return
        }

        val radioAccessSpecifier = arrayOf(
            RadioAccessSpecifier(
                AccessNetworkConstants.AccessNetworkType.GERAN,
                intArrayOf(900, 1800), // Specify bands for GERAN (GSM)
                null // channels (null means scan all)
            ),
            RadioAccessSpecifier(
                AccessNetworkConstants.AccessNetworkType.UTRAN,
                intArrayOf(2100), // Specify bands for UTRAN (UMTS)
                null // channels
            ),
            RadioAccessSpecifier(
                AccessNetworkConstants.AccessNetworkType.EUTRAN,
                intArrayOf(1800, 2600), // Specify bands for EUTRAN (LTE)
                null // channels
            )
        )

        // Add PLMN IDs for Turkey
        val plmnIds = arrayListOf("28601", "28602", "28603") // Turkcell, Vodafone Türkiye, Türk Telekom

        val networkScanRequest = NetworkScanRequest(
            NetworkScanRequest.SCAN_TYPE_ONE_SHOT,
            radioAccessSpecifier,
            5, // periodicity - not used for one-shot scans
            60, // max search time in seconds
            true, // incremental results
            5, // incremental results periodicity in seconds
            plmnIds // PLMN ids to search for
        )

        val networkScanCallback = object : TelephonyScanManager.NetworkScanCallback() {
            override fun onResults(results: List<CellInfo>) {
                super.onResults(results)
                Log.d("APNApp", getString(R.string.network_scan_results, results.size))

                val signalStrengthMap = mutableMapOf<String, MutableList<Int>>()

                for (cellInfo in results) {
                    when (cellInfo) {
                        is CellInfoLte -> {
                            val cellSignalStrengthLte = cellInfo.cellSignalStrength
                            val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: getString(R.string.unknown)
                            val mcc = cellInfo.cellIdentity.mccString?.toIntOrNull()
                            val mnc = cellInfo.cellIdentity.mncString?.toIntOrNull()
                            val providerKey = getProviderKey(mcc, mnc, networkName)
                            Log.d("APNApp", getString(R.string.lte_signal_strength, cellSignalStrengthLte.dbm, providerKey))
                            signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthLte.dbm)
                        }
                        is CellInfoGsm -> {
                            val cellSignalStrengthGsm = cellInfo.cellSignalStrength
                            val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: getString(R.string.unknown)
                            val mcc = cellInfo.cellIdentity.mccString?.toIntOrNull()
                            val mnc = cellInfo.cellIdentity.mncString?.toIntOrNull()
                            val providerKey = getProviderKey(mcc, mnc, networkName)
                            Log.d("APNApp", getString(R.string.gsm_signal_strength, cellSignalStrengthGsm.dbm, providerKey))
                            signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthGsm.dbm)
                        }
                        is CellInfoCdma -> {
                            val cellSignalStrengthCdma = cellInfo.cellSignalStrength
                            val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: getString(R.string.unknown)
                            val providerKey = getProviderKey(null, null, networkName)
                            Log.d("APNApp", getString(R.string.cdma_signal_strength, cellSignalStrengthCdma.dbm, providerKey))
                            signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthCdma.dbm)
                        }
                        is CellInfoWcdma -> {
                            val cellSignalStrengthWcdma = cellInfo.cellSignalStrength
                            val networkName = cellInfo.cellIdentity.operatorAlphaLong?.toString() ?: getString(R.string.unknown)
                            val mcc = cellInfo.cellIdentity.mccString?.toIntOrNull()
                            val mnc = cellInfo.cellIdentity.mncString?.toIntOrNull()
                            val providerKey = getProviderKey(mcc, mnc, networkName)
                            Log.d("APNApp", getString(R.string.wcdma_signal_strength, cellSignalStrengthWcdma.dbm, providerKey))
                            signalStrengthMap.getOrPut(providerKey) { mutableListOf() }.add(cellSignalStrengthWcdma.dbm)
                        }
                        else -> {
                            Log.d("APNApp", getString(R.string.unknown_cell_info, cellInfo.javaClass))
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
                        Log.d("APNApp", getString(R.string.apn_signal_strength, apn.name, apn.signalStrength))
                    }
                }

                apnList.sortByDescending { it.signalStrength }
                adapter.notifyDataSetChanged()
            }

            override fun onComplete() {
                super.onComplete()
                Log.d("APNApp", getString(R.string.network_scan_complete))
            }

            override fun onError(error: Int) {
                super.onError(error)
                Log.d("APNApp", getString(R.string.network_scan_error, error))
                if (error == 3) { // ERROR_MODEM_UNAVAILABLE
                    retryNetworkScan()
                }
            }
        }

        val executor = Executors.newSingleThreadExecutor()
        try {
            telephonyManager.requestNetworkScan(networkScanRequest, executor, networkScanCallback)
        } catch (e: Exception) {
            Log.e("APNApp", getString(R.string.failed_start_network_scan), e)
        }
    }

    private var retryCount = 0
    private val maxRetries = 3

    private fun retryNetworkScan() {
        if (retryCount < maxRetries) {
            retryCount++
            Handler(Looper.getMainLooper()).postDelayed({
                startNetworkScan()
            }, 5000) // Retry after 5 seconds
        } else {
            Log.e("APNApp", getString(R.string.max_retry_attempts_reached))
        }
    }
    private fun getProviderKey(mcc: Int?, mnc: Int?, networkName: String): String {
        return when {
            mcc == 286 && mnc == 1 -> getString(R.string.turkcell)
            mcc == 286 && mnc == 2 -> getString(R.string.vodafone_turkey)
            mcc == 286 && mnc == 3 -> getString(R.string.turk_telekom)
            else -> networkName
        }
    }

    private fun getProviderKeyForApn(apnName: String): String {
        return when {
            apnName.contains("Turkcell", ignoreCase = true) -> getString(R.string.turkcell)
            apnName.contains("Vodafone", ignoreCase = true) -> getString(R.string.vodafone_turkey)
            apnName.contains("Türk Telekom", ignoreCase = true) -> getString(R.string.turk_telekom)
            else -> getString(R.string.unknown)
        }
    }

}
