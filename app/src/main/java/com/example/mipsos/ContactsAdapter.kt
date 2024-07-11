package com.example.mipsos

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactsAdapter(private var contacts: MutableList<String>, private val context: Context) :
    RecyclerView.Adapter<ContactsAdapter.ContactViewHolder>() {

    inner class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val contactTextView: TextView = itemView.findViewById(R.id.contactTextView)
        val deleteIcon: ImageView = itemView.findViewById(R.id.deleteIcon)

        init {
            deleteIcon.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    removeItem(position)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.contact_item, parent, false)
        return ContactViewHolder(itemView)
    }

    @SuppressLint("Range")
    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val phoneNumber = contacts[position]

        // Query for contact name based on phone number
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        val nameCursor =
            context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
        nameCursor?.use {
            if (it.moveToFirst()) {
                val name = it.getString(it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME))
                holder.contactTextView.text = "$name ($phoneNumber)" // Display name and number
            } else {
                holder.contactTextView.text = phoneNumber // Display only number if name not found
            }
        }
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    private fun removeItem(position: Int) {
        val sharedPref = context.getSharedPreferences("emergency_contacts", Context.MODE_PRIVATE)
        contacts.removeAt(position)
        sharedPref.edit().putStringSet("contacts", contacts.toSet()).apply()
        notifyItemRemoved(position)
    }
}
