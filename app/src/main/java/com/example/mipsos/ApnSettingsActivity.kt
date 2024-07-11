package com.example.mipsos

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.Telephony
import androidx.appcompat.app.AppCompatActivity
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

            adapter.notifyDataSetChanged()
        }
    }
}
