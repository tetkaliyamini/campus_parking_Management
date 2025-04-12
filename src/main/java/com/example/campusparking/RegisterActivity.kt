package com.example.campusparking

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.campusparking.databinding.ActivityRegisterBinding
import com.example.campusparking.db.DatabaseHelper
import com.example.campusparking.model.User
// Add imports for CAPTCHA
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.View
import android.widget.ImageView
import android.widget.Button
import android.widget.EditText
import java.util.Random

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var dbHelper: DatabaseHelper

    // Add these properties to the class
    private lateinit var captchaImage: ImageView
    private var captchaText = ""

    // Add this method to generate CAPTCHA
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
        val bitmap = Bitmap.createBitmap(300, 100, Bitmap.Config.ARGB_8888)
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

        captchaImage.setImageBitmap(bitmap)
    }

    // Modify the onCreate method to add CAPTCHA verification
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dbHelper = DatabaseHelper(this)

        // Add CAPTCHA image view and refresh button
        val captchaLayout = layoutInflater.inflate(R.layout.captcha_layout, null)
        binding.captchaContainer.removeAllViews() // Clear any existing views first
        binding.captchaContainer.addView(captchaLayout)

        captchaImage = captchaLayout.findViewById(R.id.ivCaptcha)
        val refreshButton = captchaLayout.findViewById<Button>(R.id.btnRefreshCaptcha)
        val captchaInput = captchaLayout.findViewById<EditText>(R.id.etCaptcha)

        generateCaptcha()

        refreshButton.setOnClickListener {
            generateCaptcha()
        }

        binding.btnRegister.setOnClickListener {
            val username = binding.etUsername.text.toString()
            val password = binding.etPassword.text.toString()
            val confirmPassword = binding.etConfirmPassword.text.toString()
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val enteredCaptcha = captchaInput.text.toString()

            if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() || name.isEmpty() || email.isEmpty() || enteredCaptcha.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!enteredCaptcha.equals(captchaText, ignoreCase = true)) {
                Toast.makeText(this, "Invalid CAPTCHA. Please try again.", Toast.LENGTH_SHORT).show()
                generateCaptcha()
                captchaInput.text.clear()
                return@setOnClickListener
            }

            if (dbHelper.checkUserExists(username)) {
                Toast.makeText(this, "Username already exists", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val user = User(
                username = username,
                password = password,
                role = "user", // Default role is user
                name = name,
                email = email
            )

            val result = dbHelper.addUser(user)
            if (result > 0) {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            } else {
                Toast.makeText(this, "Registration failed", Toast.LENGTH_SHORT).show()
            }
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }
}

