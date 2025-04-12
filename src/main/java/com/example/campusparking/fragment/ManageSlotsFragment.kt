package com.example.campusparking.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusparking.adapter.ManageSlotsAdapter
import com.example.campusparking.databinding.FragmentManageSlotsBinding
import com.example.campusparking.db.DatabaseHelper
import com.example.campusparking.model.ParkingSlot

class ManageSlotsFragment : Fragment() {

    private var _binding: FragmentManageSlotsBinding? = null
    private val binding get() = _binding!!
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var adapter: ManageSlotsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageSlotsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dbHelper = DatabaseHelper(requireContext())

        setupRecyclerView()
        setupAddSlotButton()
    }

    private fun setupRecyclerView() {
        val allSlots = dbHelper.getAllParkingSlots()
        adapter = ManageSlotsAdapter(allSlots, dbHelper) {
            // Refresh the list after a slot is updated
            adapter.updateSlots(dbHelper.getAllParkingSlots())
        }
        binding.rvAllSlots.layoutManager = LinearLayoutManager(requireContext())
        binding.rvAllSlots.adapter = adapter
    }

    private fun setupAddSlotButton() {
        binding.btnAddSlot.setOnClickListener {
            val slotNumber = binding.etSlotNumber.text.toString()
            val location = binding.etLocation.text.toString()

            if (slotNumber.isEmpty() || location.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newSlot = ParkingSlot(
                slotNumber = slotNumber,
                location = location,
                isAvailable = true
            )

            val result = dbHelper.addParkingSlot(newSlot)
            if (result > 0) {
                Toast.makeText(requireContext(), "Slot added successfully", Toast.LENGTH_SHORT).show()
                binding.etSlotNumber.text?.clear()
                binding.etLocation.text?.clear()

                // Refresh the list
                adapter.updateSlots(dbHelper.getAllParkingSlots())
            } else {
                Toast.makeText(requireContext(), "Failed to add slot", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

