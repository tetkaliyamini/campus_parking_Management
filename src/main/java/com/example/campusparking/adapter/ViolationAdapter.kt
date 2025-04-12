package com.example.campusparking.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.campusparking.R
import com.example.campusparking.model.Violation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ViolationAdapter(private val violations: List<Violation>) :
    RecyclerView.Adapter<ViolationAdapter.ViolationViewHolder>() {

    inner class ViolationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val violationType: TextView = itemView.findViewById(R.id.tvViolationType)
        val penaltyAmount: TextView = itemView.findViewById(R.id.tvPenaltyAmount)
        val vehicleNumber: TextView = itemView.findViewById(R.id.tvVehicleNumber)
        val violationDate: TextView = itemView.findViewById(R.id.tvViolationDate)
        val description: TextView = itemView.findViewById(R.id.tvDescription)
        val status: TextView = itemView.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViolationViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_violation, parent, false)
        return ViolationViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViolationViewHolder, position: Int) {
        val violation = violations[position]

        holder.violationType.text = violation.violationType
        holder.penaltyAmount.text = "₹${violation.penaltyAmount}"
        holder.vehicleNumber.text = "Vehicle: ${violation.vehicleNumber}"

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val violationDateStr = dateFormat.format(Date(violation.violationDate))
        holder.violationDate.text = "Date: $violationDateStr"

        holder.description.text = violation.description

        holder.status.text = violation.status.capitalize(Locale.getDefault())

        // Set status background based on status
        when (violation.status) {
            "pending" -> holder.status.setBackgroundResource(R.drawable.bg_status_unavailable)
            "paid" -> holder.status.setBackgroundResource(R.drawable.bg_status_available)
            "disputed" -> holder.status.setBackgroundResource(R.drawable.bg_status_active)
        }
    }

    override fun getItemCount(): Int = violations.size
}

