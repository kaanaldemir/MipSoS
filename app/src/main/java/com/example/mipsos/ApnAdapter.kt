package com.example.mipsos

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ApnAdapter(private val apnList: List<Apn>) : RecyclerView.Adapter<ApnAdapter.ApnViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ApnViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_apn, parent, false)
        return ApnViewHolder(view)
    }

    override fun onBindViewHolder(holder: ApnViewHolder, position: Int) {
        val apn = apnList[position]
        holder.nameTextView.text = apn.name
        holder.apnTextView.text = apn.apn
        holder.signalStrengthTextView.text = "Signal Strength: ${apn.signalStrength}"
    }

    override fun getItemCount(): Int = apnList.size

    class ApnViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
        val apnTextView: TextView = itemView.findViewById(R.id.apnTextView)
        val signalStrengthTextView: TextView = itemView.findViewById(R.id.signalStrengthTextView)
    }
}
