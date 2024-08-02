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
        startMediatekNetworkScan()
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

            adapter.notifyDataSetChanged()
        }
    }




    private fun startMediatekNetworkScan() {
        val telephonyManagerExClass = Class.forName("com.mediatek.telephony.TelephonyManagerEx")
        val getDefaultMethod = telephonyManagerExClass.getDeclaredMethod("getDefault")
        val telephonyManagerExInstance = getDefaultMethod.invoke(null)

        val scanType = 1 // NetworkScanRequest.SCAN_TYPE_ONE_SHOT
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
        val searchTime = 60
        val incrementalResults = true
        val incrementalResultsPeriodicity = 5
        val plmnIds = arrayListOf("28601", "28602", "28603") // Turkcell, Vodafone Türkiye, Türk Telekom

        val networkScanRequest = NetworkScanRequest(scanType, radioAccessSpecifier, 5, searchTime, incrementalResults, incrementalResultsPeriodicity, plmnIds)

        val requestNetworkScanMethod = telephonyManagerExClass.getDeclaredMethod("requestNetworkScan", NetworkScanRequest::class.java, TelephonyScanManager.NetworkScanCallback::class.java)
        requestNetworkScanMethod.isAccessible = true

        val networkScanCallback = object : TelephonyScanManager.NetworkScanCallback() {
            override fun onResults(results: List<CellInfo>) {
                super.onResults(results)
                Log.d("APNApp", "Network scan results: ${results.size}")
                results.forEach { cellInfo ->
                    Log.d("APNApp", cellInfo.toString())
                }
            }

            override fun onComplete() {
                super.onComplete()
                Log.d("APNApp", "Network scan complete")
            }

            override fun onError(error: Int) {
                super.onError(error)
                Log.e("APNApp", "Network scan error: $error")
            }
        }

        requestNetworkScanMethod.invoke(telephonyManagerExInstance, networkScanRequest, networkScanCallback)
    }
}
