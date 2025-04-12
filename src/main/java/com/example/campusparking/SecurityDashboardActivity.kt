package com.example.campusparking

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.campusparking.adapter.ParkingStatusAdapter
import com.example.campusparking.adapter.ViolationAdapter
import com.example.campusparking.databinding.ActivitySecurityDashboardBinding
import com.example.campusparking.db.DatabaseHelper
import com.example.campusparking.model.GuestPass
import com.example.campusparking.model.UserBooking
import com.example.campusparking.model.Violation
import java.util.UUID

// Add imports for OCR functionality
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity

class SecurityDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecurityDashboardBinding
    private lateinit var dbHelper: DatabaseHelper

    // Add these properties to the class
    private lateinit var currentPhotoPath: String
    private var capturedVehicleNumber: String? = null

    // Add these launchers for camera and gallery
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Process the captured image with OCR
            val bitmap = BitmapFactory.decodeFile(currentPhotoPath)
            processOCRImage(bitmap)
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                processOCRImage(bitmap)
            }
        }
    }

    // Add these methods for OCR functionality
    private fun processOCRImage(bitmap: Bitmap) {
        // In a real app, this would use an OCR library to extract text from the image
        // For this simulation, we'll use the DatabaseHelper method
        capturedVehicleNumber = dbHelper.recognizeVehicleNumber(currentPhotoPath)

        // Update the UI with the recognized vehicle number
        binding.etScanVehicleNumber.setText(capturedVehicleNumber)

        // Automatically trigger the scan
        binding.btnScanVehicle.performClick()

        Toast.makeText(this, "Vehicle number recognized: $capturedVehicleNumber", Toast.LENGTH_LONG).show()
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Toast.makeText(this, "Error creating image file", Toast.LENGTH_SHORT).show()
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.campusparking.fileprovider",
                        it
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    takePictureLauncher.launch(takePictureIntent)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
        }
    }

    // Modify the onCreate method to add OCR functionality
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            binding = ActivitySecurityDashboardBinding.inflate(layoutInflater)
            setContentView(binding.root)

            dbHelper = DatabaseHelper(this)
            setSupportActionBar(binding.toolbar)

            // Start the auto-release scheduler
            dbHelper.startAutoReleaseScheduler()

            setupSlotSummary()
            setupParkingStatus()
            setupVerification()
            setupSpeedMonitoring()
            setupViolationReporting()
            setupGuestVehicleManagement()
            setupVehicleScan()

            binding.fabScan.setOnClickListener {
                // Show options for OCR scanning
                val options = arrayOf("Take Photo", "Choose from Gallery")
                AlertDialog.Builder(this)
                    .setTitle("Scan Vehicle Number")
                    .setItems(options) { _, which ->
                        when (which) {
                            0 -> dispatchTakePictureIntent()
                            1 -> {
                                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                                pickImageLauncher.launch(intent)
                            }
                        }
                    }
                    .show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error initializing security dashboard: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
            finish()
        }
    }

    private fun setupSlotSummary() {
        val twoWheelerSlots = dbHelper.getSlotCountByType("2-wheeler")
        val fourWheelerSlots = dbHelper.getSlotCountByType("4-wheeler")

        binding.tv2WheelerSlots.text = "2-wheeler slots: ${twoWheelerSlots.first}/${twoWheelerSlots.second}"
        binding.tv4WheelerSlots.text = "4-wheeler slots: ${fourWheelerSlots.first}/${fourWheelerSlots.second}"
    }

    private fun setupParkingStatus() {
        val allSlots = dbHelper.getAllParkingSlots()
        val adapter = ParkingStatusAdapter(allSlots)
        binding.rvParkingStatus.layoutManager = LinearLayoutManager(this)
        binding.rvParkingStatus.adapter = adapter
    }

    private fun setupVerification() {
        binding.btnVerifyEntry.setOnClickListener {
            val vehicleNumber = binding.etVehicleNumber.text.toString()
            if (vehicleNumber.isEmpty()) {
                Toast.makeText(this, "Please enter vehicle number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val booking = dbHelper.getBookingByVehicleNumber(vehicleNumber)
            if (booking != null) {
                dbHelper.updateBookingEntryTime(booking.id, System.currentTimeMillis())
                Toast.makeText(this, "Vehicle entry verified", Toast.LENGTH_SHORT).show()
                binding.etVehicleNumber.text?.clear()
            } else {
                Toast.makeText(this, "No booking found for this vehicle", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnVerifyExit.setOnClickListener {
            val vehicleNumber = binding.etVehicleNumber.text.toString()
            if (vehicleNumber.isEmpty()) {
                Toast.makeText(this, "Please enter vehicle number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val booking = dbHelper.getBookingByVehicleNumber(vehicleNumber)
            if (booking != null) {
                val result = dbHelper.updateBookingExitTime(booking.id, System.currentTimeMillis())
                if (result > 0) {
                    val updatedBooking = dbHelper.getBookingById(booking.id)
                    val hours = updatedBooking?.duration?.div(60) ?: 0
                    val minutes = updatedBooking?.duration?.rem(60) ?: 0

                    val durationText = if (hours > 0) {
                        "$hours hours $minutes minutes"
                    } else {
                        "$minutes minutes"
                    }

                    Toast.makeText(this, "Vehicle exit verified. Total time: $durationText", Toast.LENGTH_LONG).show()
                    binding.etVehicleNumber.text?.clear()
                } else {
                    Toast.makeText(this, "Failed to record exit time", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No booking found for this vehicle", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupSpeedMonitoring() {
        binding.btnCheckSpeed.setOnClickListener {
            val vehicleNumber = binding.etSpeedVehicleNumber.text.toString()
            val speedText = binding.etSpeed.text.toString()

            if (vehicleNumber.isEmpty() || speedText.isEmpty()) {
                Toast.makeText(this, "Please enter vehicle number and speed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val speed = speedText.toDoubleOrNull()
            if (speed == null) {
                Toast.makeText(this, "Please enter a valid speed", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (speed > 20) {
                binding.tvSpeedWarning.visibility = View.VISIBLE
                binding.btnReportViolation.visibility = View.VISIBLE
            } else {
                binding.tvSpeedWarning.visibility = View.GONE
                binding.btnReportViolation.visibility = View.GONE
                Toast.makeText(this, "Speed is within limit", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnReportViolation.setOnClickListener {
            val vehicleNumber = binding.etSpeedVehicleNumber.text.toString()
            val speedText = binding.etSpeed.text.toString()
            val speed = speedText.toDoubleOrNull() ?: 0.0

            // Find the user ID associated with this vehicle
            val booking = dbHelper.getBookingByVehicleNumber(vehicleNumber)
            if (booking != null) {
                val violation = Violation(
                    userId = booking.userId,
                    vehicleNumber = vehicleNumber,
                    violationType = "overspeeding",
                    penaltyAmount = 500.0,
                    violationDate = System.currentTimeMillis(),
                    description = "Speed limit exceeded ($speed km/h in 20 km/h zone)",
                    status = "pending"
                )

                val result = dbHelper.addViolation(violation)
                if (result > 0) {
                    Toast.makeText(this, "Violation reported. ₹500 penalty applied.", Toast.LENGTH_SHORT).show()
                    binding.etSpeedVehicleNumber.text?.clear()
                    binding.etSpeed.text?.clear()
                    binding.tvSpeedWarning.visibility = View.GONE
                    binding.btnReportViolation.visibility = View.GONE
                } else {
                    Toast.makeText(this, "Failed to report violation", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No booking found for this vehicle", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupViolationReporting() {
        binding.btnSubmitViolation.setOnClickListener {
            val vehicleNumber = binding.etViolationVehicleNumber.text.toString()
            val violationType = binding.etViolationType.text.toString()
            val description = binding.etViolationDescription.text.toString()

            if (vehicleNumber.isEmpty() || violationType.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Find the user ID associated with this vehicle
            val booking = dbHelper.getBookingByVehicleNumber(vehicleNumber)
            if (booking != null) {
                val violation = Violation(
                    userId = booking.userId,
                    vehicleNumber = vehicleNumber,
                    violationType = violationType.toLowerCase(),
                    penaltyAmount = 500.0,
                    violationDate = System.currentTimeMillis(),
                    description = description,
                    status = "pending"
                )

                val result = dbHelper.addViolation(violation)
                if (result > 0) {
                    Toast.makeText(this, "Violation reported. ₹500 penalty applied.", Toast.LENGTH_SHORT).show()
                    binding.etViolationVehicleNumber.text?.clear()
                    binding.etViolationType.text?.clear()
                    binding.etViolationDescription.text?.clear()
                } else {
                    Toast.makeText(this, "Failed to report violation", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "No booking found for this vehicle", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupGuestVehicleManagement() {
        binding.btnCreateGuestPass.setOnClickListener {
            val vehicleNumber = binding.etGuestVehicleNumber.text.toString()
            val contactPhone = binding.etGuestContactPhone.text.toString()
            val contactEmail = binding.etGuestContactEmail.text.toString()
            val vehicleType = if (binding.rbGuest2Wheeler.isChecked) "2-wheeler" else "4-wheeler"

            if (vehicleNumber.isEmpty() || contactPhone.isEmpty()) {
                Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if vehicle is already registered
            if (dbHelper.isVehicleRegistered(vehicleNumber)) {
                Toast.makeText(this, "This vehicle is already registered in the system", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Check if there's an active guest pass for this vehicle
            if (dbHelper.getActiveGuestPassByVehicle(vehicleNumber) != null) {
                Toast.makeText(this, "This vehicle already has an active guest pass", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Auto-allocate a slot
            val slot = dbHelper.autoAllocateSlotForGuest(vehicleType)
            if (slot == null) {
                Toast.makeText(this, "No available slots for this vehicle type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generate unique pass code
            val passCode = UUID.randomUUID().toString().substring(0, 8).uppercase()

            // Create guest pass
            val guestPass = GuestPass(
                vehicleNumber = vehicleNumber,
                vehicleType = vehicleType,
                slotId = slot.id,
                contactPhone = contactPhone,
                contactEmail = contactEmail,
                passCode = passCode
            )

            val result = dbHelper.addGuestPass(guestPass)
            if (result > 0) {
                // Show success message with pass details
                val message = """
                    Guest pass created successfully!
                    
                    Pass Code: $passCode
                    Slot Number: ${slot.slotNumber}
                    Location: ${slot.location}
                    Valid until: ${android.text.format.DateFormat.format("dd/MM/yyyy HH:mm", guestPass.expiryTime)}
                    
                    A confirmation SMS/email will be sent to the guest's contact details.
                """.trimIndent()

                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Guest Pass Created")
                    .setMessage(message)
                    .setPositiveButton("OK") { _, _ ->
                        // Clear the form
                        binding.etGuestVehicleNumber.text?.clear()
                        binding.etGuestContactPhone.text?.clear()
                        binding.etGuestContactEmail.text?.clear()
                        binding.rbGuest2Wheeler.isChecked = true

                        // In a real app, this would send SMS/email
                        Toast.makeText(this, "SMS/Email notification would be sent here", Toast.LENGTH_SHORT).show()
                    }
                    .show()
            } else {
                Toast.makeText(this, "Failed to create guest pass", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupVehicleScan() {
        binding.btnScanVehicle.setOnClickListener {
            val vehicleNumber = binding.etScanVehicleNumber.text.toString()

            if (vehicleNumber.isEmpty()) {
                Toast.makeText(this, "Please enter vehicle number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Get vehicle details
            val vehicleDetails = dbHelper.getVehicleDetailsByNumber(vehicleNumber)

            binding.cvVehicleDetails.visibility = View.VISIBLE

            if (vehicleDetails["isRegistered"] as Boolean) {
                // Registered vehicle
                binding.tvVehicleStatus.text = "Registered Vehicle"
                binding.tvVehicleType.text = "Type: ${vehicleDetails["vehicleType"]}"

                val booking = vehicleDetails["currentBooking"] as UserBooking?
                if (booking != null) {
                    val slot = dbHelper.getParkingSlotById(booking.slotId)
                    binding.tvSlotAllocated.text = "Slot: ${slot?.slotNumber} (${slot?.location})"
                } else {
                    binding.tvSlotAllocated.text = "No active booking"
                }

                val violationCount = vehicleDetails["violationCount"] as Int
                if (violationCount > 0) {
                    binding.tvViolationHistory.text = "Violations: $violationCount recorded"
                    binding.tvViolationHistory.setTextColor(Color.RED)
                } else {
                    binding.tvViolationHistory.text = "Violations: None"
                    binding.tvViolationHistory.setTextColor(Color.BLACK)
                }

                // Show document verification section
                binding.llDocumentVerification.visibility = View.VISIBLE
                binding.btnManualSlotOverride.visibility = View.GONE

                // Set up document verification update button
                binding.btnUpdateVerification.setOnClickListener {
                    val userId = vehicleDetails["userId"] as Int
                    val requests = dbHelper.getPendingRequestsByUserId(userId)

                    if (requests.isNotEmpty()) {
                        val request = requests[0]

                        dbHelper.markDocumentVerified(request.id, "license", binding.cbLicenseVerified.isChecked)
                        dbHelper.markDocumentVerified(request.id, "rc", binding.cbRCVerified.isChecked)
                        dbHelper.markDocumentVerified(request.id, "cv", binding.cbCVVerified.isChecked)

                        Toast.makeText(this, "Document verification updated", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "No pending requests found for this user", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // Guest vehicle or unregistered
                val guestPass = vehicleDetails["guestPass"] as GuestPass?

                if (guestPass != null) {
                    binding.tvVehicleStatus.text = "Guest Vehicle"
                    binding.tvVehicleType.text = "Type: ${guestPass.vehicleType}"

                    val slot = dbHelper.getParkingSlotById(guestPass.slotId)
                    binding.tvSlotAllocated.text = "Slot: ${slot?.slotNumber} (${slot?.location})"
                    binding.tvViolationHistory.text = "Guest Pass: ${guestPass.passCode}"
                    binding.tvViolationHistory.setTextColor(Color.BLACK)

                    // Hide document verification section
                    binding.llDocumentVerification.visibility = View.GONE
                    binding.btnManualSlotOverride.visibility = View.GONE
                } else {
                    binding.tvVehicleStatus.text = "Unregistered Vehicle"
                    binding.tvVehicleType.text = "Type: Unknown"
                    binding.tvSlotAllocated.text = "No slot allocated"
                    binding.tvViolationHistory.text = "Not in system"
                    binding.tvViolationHistory.setTextColor(Color.BLACK)

                    // Hide document verification section
                    binding.llDocumentVerification.visibility = View.GONE
                    binding.btnManualSlotOverride.visibility = View.VISIBLE

                    // Set up manual slot override button
                    binding.btnManualSlotOverride.setOnClickListener {
                        // Show dialog to create guest pass
                        showManualSlotOverrideDialog(vehicleNumber)
                    }
                }
            }
        }
    }

    private fun showManualSlotOverrideDialog(vehicleNumber: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_manual_slot_override, null)
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Manual Slot Override")
            .setView(dialogView)
            .setPositiveButton("Allocate", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()

        // Get references to dialog views
        val rgVehicleType = dialogView.findViewById<RadioGroup>(R.id.rgVehicleType)
        val etContactPhone = dialogView.findViewById<EditText>(R.id.etContactPhone)

        // Override positive button to prevent dialog from dismissing on validation failure
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val vehicleType = if (rgVehicleType.checkedRadioButtonId == R.id.rb2Wheeler) "2-wheeler" else "4-wheeler"
            val contactPhone = etContactPhone.text.toString()

            if (contactPhone.isEmpty()) {
                Toast.makeText(this, "Please enter contact phone", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Auto-allocate a slot
            val slot = dbHelper.autoAllocateSlotForGuest(vehicleType)
            if (slot == null) {
                Toast.makeText(this, "No available slots for this vehicle type", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Generate unique pass code
            val passCode = UUID.randomUUID().toString().substring(0, 8).uppercase()

            // Create guest pass
            val guestPass = GuestPass(
                vehicleNumber = vehicleNumber,
                vehicleType = vehicleType,
                slotId = slot.id,
                contactPhone = contactPhone,
                passCode = passCode
            )

            val result = dbHelper.addGuestPass(guestPass)
            if (result > 0) {
                Toast.makeText(this, "Slot allocated successfully", Toast.LENGTH_SHORT).show()
                dialog.dismiss()

                // Refresh vehicle scan
                binding.btnScanVehicle.performClick()
            } else {
                Toast.makeText(this, "Failed to allocate slot", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Override onDestroy to stop the auto-release scheduler
    override fun onDestroy() {
        super.onDestroy()
        dbHelper.stopAutoReleaseScheduler()
    }
}

