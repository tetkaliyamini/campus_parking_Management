package com.example.campusparking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campusparking.R
import com.example.campusparking.db.DatabaseHelper
import com.example.campusparking.model.UserBooking
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserBookingAdapter(
    private val bookings: List<UserBooking>,
    private val dbHelper: DatabaseHelper
) : RecyclerView.Adapter<UserBookingAdapter.BookingViewHolder>() {

    private var selectedPosition = -1

    inner class BookingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val radioButton: RadioButton = itemView.findViewById(R.id.rbSelect)
        val slotNumber: TextView = itemView.findViewById(R.id.tvSlotNumber)
        val bookingDate: TextView = itemView.findViewById(R.id.tvBookingDate)
        val expectedTimes: TextView = itemView.findViewById(R.id.tvExpectedTimes)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]
        val slot = dbHelper.getParkingSlotById(booking.slotId)

        holder.slotNumber.text = "Slot ${slot?.slotNumber ?: "Unknown"}"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val bookingDateStr = dateFormat.format(Date(booking.bookingDate))
        holder.bookingDate.text = "Booked on: $bookingDateStr"

        // Format and display expected entry/exit times
        if (booking.expectedEntryTime > 0 && booking.expectedExitTime > 0) {
            val entryDateStr = dateFormat.format(Date(booking.expectedEntryTime))
            val entryTimeStr = timeFormat.format(Date(booking.expectedEntryTime))
            val exitTimeStr = timeFormat.format(Date(booking.expectedExitTime))
            holder.expectedTimes.text = "Expected: $entryDateStr $entryTimeStr - $exitTimeStr"
            holder.expectedTimes.visibility = View.VISIBLE
        } else {
            holder.expectedTimes.visibility = View.GONE
        }

        when (booking.status) {
            "active" -> {
                holder.status.text = "Active"
                holder.status.setBackgroundResource(R.drawable.bg_status_active)
            }
            "completed" -> {
                holder.status.text = "Completed"
                holder.status.setBackgroundResource(R.drawable.bg_status_available)
            }
            "cancelled" -> {
                holder.status.text = "Cancelled"
                holder.status.setBackgroundResource(R.drawable.bg_status_unavailable)
            }
        }

        holder.radioButton.isChecked = position == selectedPosition

        holder.itemView.setOnClickListener {
            if (booking.status == "active") {
                val previousSelected = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
            }
        }

        holder.radioButton.setOnClickListener {
            if (booking.status == "active") {
                val previousSelected = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previousSelected)
                notifyItemChanged(selectedPosition)
            } else {
                holder.radioButton.isChecked = false
            }
        }
    }

    override fun getItemCount(): Int = bookings.size

    fun getSelectedBooking(): UserBooking? {
        return if (selectedPosition != -1) bookings[selectedPosition] else null
    }
}

