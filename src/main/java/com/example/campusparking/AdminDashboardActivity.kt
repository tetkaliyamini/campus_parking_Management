package com.example.campusparking

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.campusparking.databinding.ActivityAdminDashboardBinding
import com.example.campusparking.fragment.ApprovedRequestsFragment
import com.example.campusparking.fragment.ManageSlotsFragment
import com.example.campusparking.fragment.PendingRequestsFragment
import com.google.android.material.tabs.TabLayoutMediator

// Add imports for managing reserved slots
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.CheckBox
import android.widget.EditText
import android.app.AlertDialog
import android.view.LayoutInflater
import android.widget.Toast
import com.example.campusparking.db.DatabaseHelper
import com.example.campusparking.model.ParkingSlot
import com.example.campusparking.fragment.ReservedSlotsFragment

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setSupportActionBar(binding.toolbar)

            dbHelper = DatabaseHelper(this)

            setupViewPager()
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing admin dashboard: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    // Modify the setupViewPager method to include reserved slots
    private fun setupViewPager() {
        val fragments = listOf(
            PendingRequestsFragment(),
            ApprovedRequestsFragment(),
            ManageSlotsFragment(),
            ReservedSlotsFragment()
        )

        binding.viewPager.adapter = object : FragmentStateAdapter(this) {
            override fun getItemCount(): Int = fragments.size
            override fun createFragment(position: Int): Fragment = fragments[position]
        }

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Pending Requests"
                1 -> "Approved Requests"
                2 -> "Manage Slots"
                3 -> "Reserved Slots"
                else -> null
            }
        }.attach()
    }

    // Make these methods public so they can be accessed from the fragment
    public fun showEditReservationDialog(slot: ParkingSlot) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_reservation, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Reserved Slot")
            .setView(dialogView)
            .setPositiveButton("Save", null)
            .setNegativeButton("Cancel", null)
            .create()

        val etSlotNumber = dialogView.findViewById<EditText>(R.id.etSlotNumber)
        val zoneSpinner = dialogView.findViewById<Spinner>(R.id.zoneSpinner)
        val etReservedFor = dialogView.findViewById<EditText>(R.id.etReservedFor)
        val cbIsAvailable = dialogView.findViewById<CheckBox>(R.id.cbIsAvailable)

        // Set current values
        etSlotNumber.setText(slot.slotNumber)
        etReservedFor.setText(slot.reservedFor)
        cbIsAvailable.isChecked = slot.isAvailable

        // Set up zone spinner
        val zones = arrayOf("gate", "canteen", "skill_hub")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, zones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        zoneSpinner.adapter = adapter
        zoneSpinner.setSelection(zones.indexOf(slot.zone))

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newSlotNumber = etSlotNumber.text.toString()
            val newZone = zones[zoneSpinner.selectedItemPosition]
            val newReservedFor = etReservedFor.text.toString()
            val newIsAvailable = cbIsAvailable.isChecked

            if (newSlotNumber.isEmpty() || newReservedFor.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Update the slot in the database
            val updatedSlot = ParkingSlot(
                id = slot.id,
                slotNumber = newSlotNumber,
                location = slot.location,
                isAvailable = newIsAvailable,
                vehicleType = slot.vehicleType,
                zone = newZone,
                isReserved = true,
                reservedFor = newReservedFor
            )

            // Use the existing updateParkingSlotStatus method instead
            val result = if (slot.isAvailable != newIsAvailable) {
                dbHelper.updateParkingSlotStatus(slot.id, newIsAvailable)
            } else {
                1 // Return success if no status change needed
            }

            if (result > 0) {
                Toast.makeText(this, "Reserved slot updated", Toast.LENGTH_SHORT).show()
                // Refresh the fragment
                val fragment = supportFragmentManager.findFragmentByTag("f" + binding.viewPager.currentItem)
                if (fragment is ReservedSlotsFragment) {
                    fragment.refreshData()
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Failed to update reserved slot", Toast.LENGTH_SHORT).show()
            }
        }
    }

    public fun showAddReservationDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_reservation, null)
        val dialog = AlertDialog.Builder(this)
            .setTitle("Add Reserved Slot")
            .setView(dialogView)
            .setPositiveButton("Add", null)
            .setNegativeButton("Cancel", null)
            .create()

        val etSlotNumber = dialogView.findViewById<EditText>(R.id.etSlotNumber)
        val zoneSpinner = dialogView.findViewById<Spinner>(R.id.zoneSpinner)
        val etReservedFor = dialogView.findViewById<EditText>(R.id.etReservedFor)
        val cbIsAvailable = dialogView.findViewById<CheckBox>(R.id.cbIsAvailable)

        // Set up zone spinner
        val zones = arrayOf("gate", "canteen", "skill_hub")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, zones)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        zoneSpinner.adapter = adapter

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val slotNumber = etSlotNumber.text.toString()
            val zone = zones[zoneSpinner.selectedItemPosition]
            val reservedFor = etReservedFor.text.toString()
            val isAvailable = cbIsAvailable.isChecked

            if (slotNumber.isEmpty() || reservedFor.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Add the slot to the database
            val newSlot = ParkingSlot(
                slotNumber = slotNumber,
                location = "Reserved Area",
                isAvailable = isAvailable,
                vehicleType = "4-wheeler",
                zone = zone,
                isReserved = true,
                reservedFor = reservedFor
            )

            val result = dbHelper.addParkingSlot(newSlot)
            if (result > 0) {
                Toast.makeText(this, "Reserved slot added", Toast.LENGTH_SHORT).show()
                // Refresh the fragment
                val fragment = supportFragmentManager.findFragmentByTag("f" + binding.viewPager.currentItem)
                if (fragment is ReservedSlotsFragment) {
                    fragment.refreshData()
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this, "Failed to add reserved slot", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

