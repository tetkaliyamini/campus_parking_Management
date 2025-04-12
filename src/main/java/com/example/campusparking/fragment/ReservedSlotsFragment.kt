package com.example.campusparking.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.campusparking.R
import com.example.campusparking.databinding.FragmentReservedSlotsBinding
import com.example.campusparking.db.DatabaseHelper
import com.example.campusparking.model.ParkingSlot
import com.example.campusparking.AdminDashboardActivity

class ReservedSlotsFragment : Fragment() {

    private var _binding: FragmentReservedSlotsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReservedSlotsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        setupReservedSlots()

        binding.btnAddReservedSlot.setOnClickListener {
            (activity as? AdminDashboardActivity)?.showAddReservationDialog()
        }
    }

    fun refreshData() {
        setupReservedSlots()
    }

    private fun setupReservedSlots() {
        val reservedSlots = dbHelper.getReservedSlots()

        if (reservedSlots.isEmpty()) {
            binding.tvNoReservedSlots.visibility = View.VISIBLE
            binding.rvReservedSlots.visibility = View.GONE
        } else {
            binding.tvNoReservedSlots.visibility = View.GONE
            binding.rvReservedSlots.visibility = View.VISIBLE

            val adapter = ReservedSlotsAdapter(reservedSlots) { slot ->
                (activity as? AdminDashboardActivity)?.showEditReservationDialog(slot)
            }
            binding.rvReservedSlots.layoutManager = LinearLayoutManager(requireContext())
            binding.rvReservedSlots.adapter = adapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private inner class ReservedSlotsAdapter(
        private val slots: List<ParkingSlot>,
        private val onEditClick: (ParkingSlot) -> Unit
    ) : RecyclerView.Adapter<ReservedSlotsAdapter.ViewHolder>() {

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val slotNumber: TextView = view.findViewById(R.id.tvSlotNumber)
            val location: TextView = view.findViewById(R.id.tvLocation)
            val reservedFor: TextView = view.findViewById(R.id.tvReservedFor)
            val status: TextView = view.findViewById(R.id.tvStatus)
            val btnEdit: Button = view.findViewById(R.id.btnEditReservation)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_reserved_slot, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val slot = slots[position]

            holder.slotNumber.text = "Slot ${slot.slotNumber}"
            holder.location.text = "Zone: ${slot.zone}"
            holder.reservedFor.text = "Reserved for: ${slot.reservedFor}"

            if (slot.isAvailable) {
                holder.status.text = "Available"
                holder.status.setBackgroundResource(R.drawable.bg_status_available)
            } else {
                holder.status.text = "Occupied"
                holder.status.setBackgroundResource(R.drawable.bg_status_unavailable)
            }

            holder.btnEdit.setOnClickListener {
                onEditClick(slot)
            }
        }

        override fun getItemCount(): Int = slots.size
    }
}

