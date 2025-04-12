package com.example.campusparking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.campusparking.R
import com.example.campusparking.model.ParkingSlot
import com.example.campusparking.db.DatabaseHelper

class ManageSlotsAdapter(
    private var slots: List<ParkingSlot>,
    private val dbHelper: DatabaseHelper,
    private val onSlotUpdated: () -> Unit
) : RecyclerView.Adapter<ManageSlotsAdapter.SlotViewHolder>() {

    inner class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val slotNumber: TextView = itemView.findViewById(R.id.tvSlotNumber)
        val location: TextView = itemView.findViewById(R.id.tvLocation)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
        val btnToggleStatus: Button = itemView.findViewById(R.id.btnToggleStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]

        holder.slotNumber.text = "Slot ${slot.slotNumber}"
        holder.location.text = slot.location

        if (slot.isAvailable) {
            holder.status.text = "Available"
            holder.status.setBackgroundResource(R.drawable.bg_status_available)
            holder.btnToggleStatus.text = "Disable"
        } else {
            holder.status.text = "Disabled"
            holder.status.setBackgroundResource(R.drawable.bg_status_unavailable)
            holder.btnToggleStatus.text = "Enable"
        }

        holder.btnToggleStatus.setOnClickListener {
            val newStatus = !slot.isAvailable
            val result = dbHelper.updateParkingSlotStatus(slot.id, newStatus)

            if (result > 0) {
                Toast.makeText(
                    holder.itemView.context,
                    "Slot ${if (newStatus) "enabled" else "disabled"}",
                    Toast.LENGTH_SHORT
                ).show()
                onSlotUpdated()
            } else {
                Toast.makeText(
                    holder.itemView.context,
                    "Failed to update slot status",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    override fun getItemCount(): Int = slots.size

    fun updateSlots(newSlots: List<ParkingSlot>) {
        slots = newSlots
        notifyDataSetChanged()
    }
}

