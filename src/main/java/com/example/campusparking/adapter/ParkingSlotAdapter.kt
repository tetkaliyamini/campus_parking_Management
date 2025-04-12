package com.example.campusparking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campusparking.R
import com.example.campusparking.model.ParkingSlot

class ParkingSlotAdapter(private val slots: List<ParkingSlot>) :
    RecyclerView.Adapter<ParkingSlotAdapter.SlotViewHolder>() {

    private var selectedPosition = -1

    inner class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radioButton: RadioButton = itemView.findViewById(R.id.rbSelect)
        val slotNumber: TextView = itemView.findViewById(R.id.tvSlotNumber)
        val location: TextView = itemView.findViewById(R.id.tvLocation)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking_slot, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val slot = slots[position]

        holder.slotNumber.text = "Slot ${slot.slotNumber}"
        holder.location.text = slot.location

        if (slot.isAvailable) {
            holder.status.text = "Available"
            holder.status.setBackgroundResource(R.drawable.bg_status_available)
        } else {
            holder.status.text = "Occupied"
            holder.status.setBackgroundResource(R.drawable.bg_status_unavailable)
        }

        holder.radioButton.isChecked = position == selectedPosition

        holder.itemView.setOnClickListener {
            if (slot.isAvailable) {
                val previousSelected = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
            }
        }

        holder.radioButton.setOnClickListener {
            if (slot.isAvailable) {
                val previousSelected = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
            } else {
                holder.radioButton.isChecked = false
            }
        }
    }

    override fun getItemCount(): Int = slots.size

    fun getSelectedSlot(): ParkingSlot? {
        return if (selectedPosition != -1) slots[selectedPosition] else null
    }
}

