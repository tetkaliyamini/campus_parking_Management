package com.example.campusparking

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.campusparking.db.DatabaseHelper
import com.example.campusparking.model.User
import com.example.campusparking.utils.DatabaseInspectorActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var dbHelper: DatabaseHelper

    // Add a method to handle database reset if needed
    private fun resetDatabaseIfNeeded() {
        try {
            // Check if we need to reset the database due to schema issues
            val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
            val lastDbVersion = prefs.getInt("db_version", 0)

            if (lastDbVersion < DatabaseHelper.DATABASE_VERSION) {
                // Delete the database file to force recreation
                val dbFile = getDatabasePath("campus_parking.db")
                if (dbFile.exists()) {
                    dbFile.delete()

                    // Recreate the database helper to initialize the new database
                    dbHelper = DatabaseHelper(this)

                    // Save the new version
                    prefs.edit().putInt("db_version", DatabaseHelper.DATABASE_VERSION).apply()

                    Toast.makeText(this, "Database updated to new version", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize database helper
        dbHelper = DatabaseHelper(this)

        // Add this line to reset the database if needed
        resetDatabaseIfNeeded()

        // Create default admin user if not exists
        if (!dbHelper.checkUserExists("admin")) {
            val adminUser = User(
                username = "admin",
                password = "admin123",
                role = "admin",
                name = "Administrator",
                email = "admin@campus.edu"
            )
            dbHelper.addUser(adminUser)
        }

        // Set click listeners
        findViewById<android.widget.Button>(R.id.loginButton).setOnClickListener {
            val username = findViewById<android.widget.EditText>(R.id.usernameEditText).text.toString().trim()
            val password = findViewById<android.widget.EditText>(R.id.passwordEditText).text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                val user = dbHelper.getUser(username, password)
                if (user != null) {
                    // Login successful
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()

                    // Navigate to appropriate dashboard based on user role
                    when (user.role) {
                        "admin" -> {
                            val intent = Intent(this, AdminDashboardActivity::class.java)
                            intent.putExtra("USER_ID", user.id)
                            startActivity(intent)
                        }
                        "security" -> {
                            val intent = Intent(this, SecurityDashboardActivity::class.java)
                            intent.putExtra("USER_ID", user.id)
                            startActivity(intent)
                        }
                        else -> {
                            val intent = Intent(this, UserDashboardActivity::class.java)
                            intent.putExtra("USER_ID", user.id)
                            startActivity(intent)
                        }
                    }
                } else {
                    Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("LoginActivity", "Error during login: ${e.message}")
                Toast.makeText(this, "Error during login: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<android.widget.TextView>(R.id.registerTextView).setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }

        findViewById<android.widget.TextView>(R.id.guestLoginTextView).setOnClickListener {
            val intent = Intent(this, GuestLoginActivity::class.java)
            startActivity(intent)
        }

        // Add debug database button click listener
        findViewById<android.widget.TextView>(R.id.debugDbTextView).setOnClickListener {
            val intent = Intent(this, DatabaseInspectorActivity::class.java)
            startActivity(intent)
        }

        // Add a long press listener to the login button for database debugging
        findViewById<android.widget.Button>(R.id.loginButton).setOnLongClickListener {
            exportDatabaseToDownloads()
            true
        }
    }

    // Method to export the database to the Downloads folder
    private fun exportDatabaseToDownloads() {
        try {
            val dbFile = getDatabasePath("campus_parking.db")
            if (dbFile.exists()) {
                val downloadsDir = getExternalFilesDir(null)
                val exportDir = java.io.File(downloadsDir, "CampusParking")
                if (!exportDir.exists()) {
                    exportDir.mkdirs()
                }

                val exportFile = java.io.File(exportDir, "campus_parking_export.db")
                dbFile.copyTo(exportFile, overwrite = true)

                Toast.makeText(
                    this,
                    "Database exported to: ${exportFile.absolutePath}",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(this, "Database file not found", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}