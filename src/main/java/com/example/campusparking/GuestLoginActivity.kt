package com.example.campusparking

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.campusparking.databinding.ActivityGuestLoginBinding
import com.example.campusparking.db.DatabaseHelper
import com.example.campusparking.model.GuestPass
import java.util.Random
import java.util.UUID

class GuestLoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGuestLoginBinding
    private lateinit var dbHelper: DatabaseHelper
    private var captchaText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGuestLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        generateCaptcha()

        binding.btnRefreshCaptcha.setOnClickListener {
            generateCaptcha()
        }

        binding.btnVerifyCaptcha.setOnClickListener {
            val enteredCaptcha = binding.etCaptcha.text.toString()
            if (enteredCaptcha.equals(captchaText, ignoreCase = true)) {
                // CAPTCHA verified
                binding.cvGuestInfo.visibility = View.VISIBLE
                Toast.makeText(this, "CAPTCHA verified successfully", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Incorrect CAPTCHA. Please try again.", Toast.LENGTH_SHORT).show()
                generateCaptcha()
            }
        }

        binding.btnSubmitGuestInfo.setOnClickListener {
            submitGuestInfo()
        }
    }

    private fun generateCaptcha() {
        // Generate random CAPTCHA text (6 characters)
        val allowedChars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"
        val random = Random()
        val sb = StringBuilder(6)
        for (i in 0 until 6) {
            sb.append(allowedChars[random.nextInt(allowedChars.length)])
        }
        captchaText = sb.toString()

        // Create CAPTCHA image
        val bitmap = Bitmap.createBitmap(binding.ivCaptcha.width, binding.ivCaptcha.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint()

        // Draw background
        canvas.drawColor(Color.rgb(240, 240, 240))

        // Draw text
        paint.color = Color.rgb(0, 0, 0)
        paint.textSize = 60f
        paint.isAntiAlias = true
        paint.textSkewX = -0.1f
        canvas.drawText(captchaText, 20f, 80f, paint)

        // Draw random lines
        for (i in 0 until 6) {
            paint.color = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256))
            canvas.drawLine(
                random.nextInt(bitmap.width).toFloat(),
                random.nextInt(bitmap.height).toFloat(),
                random.nextInt(bitmap.width).toFloat(),
                random.nextInt(bitmap.height).toFloat(),
                paint
            )
        }

        binding.ivCaptcha.setImageBitmap(bitmap)
        binding.etCaptcha.text?.clear()
    }

    private fun submitGuestInfo() {
        val vehicleNumber = binding.etVehicleNumber.text.toString()
        val contactPhone = binding.etContactPhone.text.toString()
        val contactEmail = binding.etContactEmail.text.toString()
        val vehicleType = if (binding.rb2Wheeler.isChecked) "2-wheeler" else "4-wheeler"

        if (vehicleNumber.isEmpty() || contactPhone.isEmpty()) {
            Toast.makeText(this, "Please fill all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if vehicle is already registered
        if (dbHelper.isVehicleRegistered(vehicleNumber)) {
            Toast.makeText(this, "This vehicle is already registered in the system", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if there's an active guest pass for this vehicle
        if (dbHelper.getActiveGuestPassByVehicle(vehicleNumber) != null) {
            Toast.makeText(this, "This vehicle already has an active guest pass", Toast.LENGTH_SHORT).show()
            return
        }

        // Auto-allocate a slot
        val slot = dbHelper.autoAllocateSlotForGuest(vehicleType)
        if (slot == null) {
            Toast.makeText(this, "No available slots for your vehicle type", Toast.LENGTH_SHORT).show()
            return
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
                
                A confirmation SMS/email will be sent to your contact details.
            """.trimIndent()

            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Guest Pass Created")
                .setMessage(message)
                .setPositiveButton("OK") { _, _ ->
                    // In a real app, this would send SMS/email
                    // For now, just finish the activity
                    finish()
                }
                .setCancelable(false)
                .show()
        } else {
            Toast.makeText(this, "Failed to create guest pass", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

