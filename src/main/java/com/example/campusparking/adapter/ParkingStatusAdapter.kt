package com.example.campusparking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campusparking.R
import com.example.campusparking.model.ParkingSlot

class ParkingStatusAdapter(private val slots: List<ParkingSlot>) :
    RecyclerView.Adapter<ParkingStatusAdapter.StatusViewHolder>() {

    inner class StatusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val slotNumber: TextView = itemView.findViewById(R.id.tvSlotNumber)
        val location: TextView = itemView.findViewById(R.id.tvLocation)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
        val vehicleInfoLayout: View = itemView.findViewById(R.id.layoutVehicleInfo)
        val vehicleNumber: TextView = itemView.findViewById(R.id.tvVehicleNumber)
        val entryTime: TextView = itemView.findViewById(R.id.tvEntryTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatusViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking_status, parent, false)
        return StatusViewHolder(view)
    }

    override fun onBindViewHolder(holder: StatusViewHolder, position: Int) {
        val slot = slots[position]

        holder.slotNumber.text = "Slot ${slot.slotNumber}"
        holder.location.text = slot.location

        if (slot.isAvailable) {
            holder.status.text = "Available"
            holder.status.setBackgroundResource(R.drawable.bg_status_available)
            holder.vehicleInfoLayout.visibility = View.GONE
        } else {
            holder.status.text = "Occupied"
            holder.status.setBackgroundResource(R.drawable.bg_status_unavailable)

            // In a real app, you would fetch the vehicle information from the database
            // For this example, we'll just show placeholder data
            holder.vehicleInfoLayout.visibility = View.VISIBLE
            holder.vehicleNumber.text = "Vehicle Number: KA-XX-XX-XXXX"
            holder.entryTime.text = "Entry Time: Not recorded"
        }
    }

    override fun getItemCount(): Int = slots.size
}

