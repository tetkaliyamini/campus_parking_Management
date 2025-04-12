package com.example.campusparking

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusparking.adapter.ParkingSlotAdapter
import com.example.campusparking.adapter.UserBookingAdapter
import com.example.campusparking.adapter.ViolationAdapter
import com.example.campusparking.databinding.ActivityUserDashboardBinding
import com.example.campusparking.db.DatabaseHelper
import com.example.campusparking.model.ParkingRequest
import com.example.campusparking.model.ParkingSlot
import com.example.campusparking.model.UserBooking

// Add these imports at the top of the file
import android.app.TimePickerDialog
import android.app.DatePickerDialog
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
// Add imports for real-time updates
import android.os.Handler
import android.os.Looper
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.AdapterView
import java.util.concurrent.TimeUnit

class UserDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserDashboardBinding
    private lateinit var dbHelper: DatabaseHelper
    private var userId: Int = -1
    private var cvBookUri: Uri? = null
    private var rcBookUri: Uri? = null
    private var driverLicenseUri: Uri? = null
    private var selectedVehicleType: String = "2-wheeler"
    // Add these properties to the class
    private lateinit var zoneSpinner: Spinner
    private var selectedZone: String = "gate"
    private val handler = Handler(Looper.getMainLooper())
    private val refreshRunnable = object : Runnable {
        override fun run() {
            refreshData()
            handler.postDelayed(this, 30000) // Refresh every 30 seconds
        }
    }

    private val cvBookLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                cvBookUri = uri
                Toast.makeText(this, "CV Book uploaded successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val rcBookLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                rcBookUri = uri
                Toast.makeText(this, "RC Book uploaded successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val driverLicenseLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                driverLicenseUri = uri
                Toast.makeText(this, "Driver's License uploaded successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Modify the onCreate method to add zone selection and real-time updates
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivityUserDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            userId = intent.getIntExtra("USER_ID", -1)
            if (userId == -1) {
                Toast.makeText(this, "User not found", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            dbHelper = DatabaseHelper(this)
            setSupportActionBar(binding.toolbar)

            // Add zone spinner
            zoneSpinner = binding.zoneSpinner
            val zones = arrayOf("gate", "canteen", "skill_hub")
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, zones)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            zoneSpinner.adapter = adapter

            zoneSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                    selectedZone = zones[position]
                    setupAvailableSlots()
                }

                override fun onNothingSelected(parent: AdapterView<*>) {
                    // Do nothing
                }
            }

            setupSlotSummary()
            setupVehicleTypeSelection()
            setupAvailableSlots()
            setupMyBookings()
            setupRequestForm()
            setupViolations()

            binding.fabRefresh.setOnClickListener {
                refreshData()
            }

            // Start real-time updates
            handler.post(refreshRunnable)
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing dashboard: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    // Override onDestroy to stop real-time updates
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(refreshRunnable)
    }

    private fun setupSlotSummary() {
        val twoWheelerSlots = dbHelper.getSlotCountByType("2-wheeler")
        val fourWheelerSlots = dbHelper.getSlotCountByType("4-wheeler")

        binding.tv2WheelerSlots.text = "2-wheeler slots: ${twoWheelerSlots.first}/${twoWheelerSlots.second}"
        binding.tv4WheelerSlots.text = "4-wheeler slots: ${fourWheelerSlots.first}/${fourWheelerSlots.second}"
    }

    private fun setupVehicleTypeSelection() {
        binding.rgVehicleType.setOnCheckedChangeListener { _, checkedId ->
            selectedVehicleType = when (checkedId) {
                R.id.rb2Wheeler -> "2-wheeler"
                R.id.rb4Wheeler -> "4-wheeler"
                else -> "2-wheeler"
            }
            setupAvailableSlots()
        }
    }

    // Modify the setupAvailableSlots method to filter by zone
    private fun setupAvailableSlots() {
        try {
            val availableSlots = dbHelper.getAvailableParkingSlots(selectedVehicleType, selectedZone)
            val adapter = ParkingSlotAdapter(availableSlots)
            binding.rvAvailableSlots.layoutManager = LinearLayoutManager(this)
            binding.rvAvailableSlots.adapter = adapter

            // Set up time selection fields
            binding.etExpectedEntryTime.setOnClickListener {
                showDateTimePicker(true)
            }

            binding.etExpectedExitTime.setOnClickListener {
                showDateTimePicker(false)
            }

            // Modify the btnBookSlot click listener to include expected entry/exit times
            binding.btnBookSlot.setOnClickListener {
                val selectedSlot = adapter.getSelectedSlot()
                if (selectedSlot != null) {
                    val expectedEntryTimeStr = binding.etExpectedEntryTime.text.toString()
                    val expectedExitTimeStr = binding.etExpectedExitTime.text.toString()

                    if (expectedEntryTimeStr.isEmpty() || expectedExitTimeStr.isEmpty()) {
                        Toast.makeText(this, "Please select expected entry and exit times", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    val expectedEntryTime = dateFormat.parse(expectedEntryTimeStr)?.time ?: 0
                    val expectedExitTime = dateFormat.parse(expectedExitTimeStr)?.time ?: 0

                    if (expectedEntryTime >= expectedExitTime) {
                        Toast.makeText(this, "Exit time must be after entry time", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Check if duration exceeds 12 hours
                    val durationHours = TimeUnit.MILLISECONDS.toHours(expectedExitTime - expectedEntryTime)
                    if (durationHours > 12) {
                        Toast.makeText(this, "Maximum parking duration is 12 hours", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    val booking = UserBooking(
                        userId = userId,
                        slotId = selectedSlot.id,
                        bookingDate = System.currentTimeMillis(),
                        expectedEntryTime = expectedEntryTime,
                        expectedExitTime = expectedExitTime,
                        status = "active",
                        vehicleType = selectedVehicleType
                    )
                    val result = dbHelper.addBooking(booking)
                    if (result > 0) {
                        dbHelper.updateParkingSlotStatus(selectedSlot.id, false)
                        Toast.makeText(this, "Slot booked successfully", Toast.LENGTH_SHORT).show()
                        binding.etExpectedEntryTime.text?.clear()
                        binding.etExpectedExitTime.text?.clear()
                        refreshData()
                    } else {
                        Toast.makeText(this, "Failed to book slot", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Please select a slot", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            // Fallback to simple slot display without zone filtering
            Toast.makeText(this, "Using basic slot view due to database issue", Toast.LENGTH_SHORT).show()
            try {
                val availableSlots = dbHelper.getAllParkingSlots().filter { it.isAvailable && it.vehicleType == selectedVehicleType }
                val adapter = ParkingSlotAdapter(availableSlots)
                binding.rvAvailableSlots.layoutManager = LinearLayoutManager(this)
                binding.rvAvailableSlots.adapter = adapter
            } catch (e2: Exception) {
                Toast.makeText(this, "Error loading slots: ${e2.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupMyBookings() {
        val myBookings = dbHelper.getUserBookings(userId)
        val adapter = UserBookingAdapter(myBookings, dbHelper)
        binding.rvMyBookings.layoutManager = LinearLayoutManager(this)
        binding.rvMyBookings.adapter = adapter

        binding.btnCancelBooking.setOnClickListener {
            val selectedBooking = adapter.getSelectedBooking()
            if (selectedBooking != null) {
                dbHelper.updateParkingSlotStatus(selectedBooking.slotId, true)
                dbHelper.cancelBooking(selectedBooking.id)
                Toast.makeText(this, "Booking cancelled successfully", Toast.LENGTH_SHORT).show()
                refreshData()
            } else {
                Toast.makeText(this, "Please select a booking", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRequestForm() {
        binding.rgRequestVehicleType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbRequest2Wheeler -> {
                    binding.cbHelmet.isEnabled = true
                    binding.cbSeatBelt.isEnabled = false
                }
                R.id.rbRequest4Wheeler -> {
                    binding.cbHelmet.isEnabled = false
                    binding.cbSeatBelt.isEnabled = true
                }
            }
        }

        binding.btnUploadDriverLicense.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/pdf"
            }
            driverLicenseLauncher.launch(intent)
        }

        binding.btnUploadCV.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/pdf"
            }
            cvBookLauncher.launch(intent)
        }

        binding.btnUploadRC.setOnClickListener {
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                type = "application/pdf"
            }
            rcBookLauncher.launch(intent)
        }

        binding.btnSubmitRequest.setOnClickListener {
            val vehicleNumber = binding.etVehicleNumber.text.toString()
            val requestVehicleType = if (binding.rbRequest2Wheeler.isChecked) "2-wheeler" else "4-wheeler"

            if (vehicleNumber.isEmpty()) {
                Toast.makeText(this, "Please enter vehicle number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (driverLicenseUri == null) {
                Toast.makeText(this, "Please upload Driver's License", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (cvBookUri == null || rcBookUri == null) {
                Toast.makeText(this, "Please upload both CV Book and RC Book", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val hasHelmet = binding.cbHelmet.isChecked
            val hasSeatBelt = binding.cbSeatBelt.isChecked

            // Validate safety compliance based on vehicle type
            if (requestVehicleType == "2-wheeler" && !hasHelmet) {
                Toast.makeText(this, "Helmet is required for 2-wheelers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (requestVehicleType == "4-wheeler" && !hasSeatBelt) {
                Toast.makeText(this, "Seat belt is required for 4-wheelers", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val request = ParkingRequest(
                userId = userId,
                vehicleNumber = vehicleNumber,
                vehicleType = requestVehicleType,
                cvBookPath = cvBookUri.toString(),
                rcBookPath = rcBookUri.toString(),
                driverLicensePath = driverLicenseUri.toString(),
                hasHelmet = hasHelmet,
                hasSeatBelt = hasSeatBelt,
                status = "pending",
                requestDate = System.currentTimeMillis()
            )

            val result = dbHelper.addParkingRequest(request)
            if (result > 0) {
                Toast.makeText(this, "Request submitted successfully", Toast.LENGTH_SHORT).show()
                binding.etVehicleNumber.text?.clear()
                binding.cbHelmet.isChecked = false
                binding.cbSeatBelt.isChecked = false
                driverLicenseUri = null
                cvBookUri = null
                rcBookUri = null
            } else {
                Toast.makeText(this, "Failed to submit request", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupViolations() {
        val violations = dbHelper.getViolationsByUser(userId)

        if (violations.isEmpty()) {
            binding.tvNoViolations.visibility = android.view.View.VISIBLE
            binding.rvViolations.visibility = android.view.View.GONE
        } else {
            binding.tvNoViolations.visibility = android.view.View.GONE
            binding.rvViolations.visibility = android.view.View.VISIBLE

            val adapter = ViolationAdapter(violations)
            binding.rvViolations.layoutManager = LinearLayoutManager(this)
            binding.rvViolations.adapter = adapter
        }
    }

    private fun refreshData() {
        setupSlotSummary()
        setupAvailableSlots()
        setupMyBookings()
        setupViolations()
    }

    // Add this method to handle date and time selection
    private fun showDateTimePicker(isEntryTime: Boolean) {
        val calendar = Calendar.getInstance()

        // First, show date picker
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                // Then, show time picker
                val timePickerDialog = TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        calendar.set(Calendar.MINUTE, minute)

                        // Format the selected date and time
                        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                        val formattedDateTime = dateFormat.format(calendar.time)

                        // Update the appropriate EditText
                        if (isEntryTime) {
                            binding.etExpectedEntryTime.setText(formattedDateTime)
                        } else {
                            binding.etExpectedExitTime.setText(formattedDateTime)
                        }
                    },
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    true
                )
                timePickerDialog.show()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePickerDialog.datePicker.minDate = System.currentTimeMillis()

        datePickerDialog.show()
    }
}

