package com.mipo.mipsos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ApnAdapter(private val apnList: List<Apn>) : RecyclerView.Adapter<ApnAdapter.ApnViewHolder>() {

    // Filter out entries with signal strength -120
    private val filteredApnList = apnList.filter { it.signalStrength != -120 }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApnViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_apn, parent, false)
        return ApnViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApnViewHolder, position: Int) {
        val apn = filteredApnList[position] // Use the filtered list
        holder.nameTextView.text = apn.name
        holder.apnTextView.text = apn.apn
        holder.signalStrengthTextView.text = holder.itemView.context.getString(R.string.signal_strength, apn.signalStrength)
    }

    override fun getItemCount(): Int = filteredApnList.size // Use the filtered list size

    class ApnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val apnTextView: TextView = itemView.findViewById(R.id.apnTextView)
        val signalStrengthTextView: TextView = itemView.findViewById(R.id.signalStrengthTextView)
    }
}
