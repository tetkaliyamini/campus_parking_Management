package com.example.campusparking.adapter

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.campusparking.R
import com.example.campusparking.model.ParkingRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.campusparking.db.DatabaseHelper

class ParkingRequestAdapter(
    private val requests: List<ParkingRequest>,
    private val dbHelper: DatabaseHelper,
    private val showActions: Boolean
) : RecyclerView.Adapter<ParkingRequestAdapter.RequestViewHolder>() {

    inner class RequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val vehicleNumber: TextView = itemView.findViewById(R.id.tvVehicleNumber)
        val requestDate: TextView = itemView.findViewById(R.id.tvRequestDate)
        val cvBook: TextView = itemView.findViewById(R.id.tvCVBook)
        val rcBook: TextView = itemView.findViewById(R.id.tvRCBook)
        val helmet: TextView = itemView.findViewById(R.id.tvHelmet)
        val seatBelt: TextView = itemView.findViewById(R.id.tvSeatBelt)
        val actionsLayout: View = itemView.findViewById(R.id.layoutActions)
        val btnApprove: Button = itemView.findViewById(R.id.btnApprove)
        val btnReject: Button = itemView.findViewById(R.id.btnReject)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_parking_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        val request = requests[position]

        holder.vehicleNumber.text = request.vehicleNumber

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val requestDateStr = dateFormat.format(Date(request.requestDate))
        holder.requestDate.text = requestDateStr

        holder.cvBook.text = "CV Book: Uploaded"
        holder.rcBook.text = "RC Book: Uploaded"

        holder.helmet.text = "Helmet: ${if (request.hasHelmet) "Yes" else "No"}"
        holder.helmet.setTextColor(
            holder.itemView.context.getColor(
                if (request.hasHelmet) R.color.design_default_color_secondary
                else R.color.design_default_color_error
            )
        )

        holder.seatBelt.text = "Seat Belt: ${if (request.hasSeatBelt) "Yes" else "No"}"
        holder.seatBelt.setTextColor(
            holder.itemView.context.getColor(
                if (request.hasSeatBelt) R.color.design_default_color_secondary
                else R.color.design_default_color_error
            )
        )

        if (showActions) {
            holder.actionsLayout.visibility = View.VISIBLE

            holder.btnApprove.setOnClickListener {
                showApproveDialog(request, position)
            }

            holder.btnReject.setOnClickListener {
                dbHelper.rejectRequest(request.id)
                Toast.makeText(holder.itemView.context, "Request rejected", Toast.LENGTH_SHORT).show()
                notifyItemChanged(position)
            }
        } else {
            holder.actionsLayout.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = requests.size

    private fun showApproveDialog(request: ParkingRequest, position: Int) {
        val context = dbHelper.context
        val availableSlots = dbHelper.getAvailableParkingSlots()

        if (availableSlots.isEmpty()) {
            Toast.makeText(context, "No available slots to assign", Toast.LENGTH_SHORT).show()
            return
        }

        val slotNumbers = availableSlots.map { it.slotNumber }.toTypedArray()
        var selectedSlotIndex = 0

        AlertDialog.Builder(context)
            .setTitle("Assign Parking Slot")
            .setSingleChoiceItems(slotNumbers, selectedSlotIndex) { _, which ->
                selectedSlotIndex = which
            }
            .setPositiveButton("Approve") { _, _ ->
                val selectedSlot = availableSlots[selectedSlotIndex]

                // Approve the request
                dbHelper.approveRequest(request.id)

                // Create a booking for the user
                val booking = com.example.campusparking.model.UserBooking(
                    userId = request.userId,
                    slotId = selectedSlot.id,
                    bookingDate = System.currentTimeMillis(),
                    status = "active"
                )
                dbHelper.addBooking(booking)

                // Update the slot status
                dbHelper.updateParkingSlotStatus(selectedSlot.id, false)

                Toast.makeText(context, "Request approved and slot assigned", Toast.LENGTH_SHORT).show()
                notifyItemChanged(position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

