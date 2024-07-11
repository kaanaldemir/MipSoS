package com.example.mipsos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ApnAdapter(private var apnList: MutableList<String>, private val onApnClickListener: (String) -> Unit) :
    RecyclerView.Adapter<ApnAdapter.ApnViewHolder>() {

    class ApnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val apnTextView: TextView = itemView.findViewById(R.id.apnNameTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApnViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.apn_item, parent, false)
        return ApnViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ApnViewHolder, position: Int) {
        val apnName = apnList[position]
        holder.apnTextView.text = apnName

        // Add click listener to apn item in recyclerview
        holder.itemView.setOnClickListener { onApnClickListener(apnName) }
    }

    override fun getItemCount(): Int {
        return apnList.size
    }

    fun updateApnList(newApnList: List<String>) {
        apnList.clear()
        apnList.addAll(newApnList)
        notifyDataSetChanged()
    }
}
