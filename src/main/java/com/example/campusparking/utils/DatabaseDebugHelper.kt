package com.example.campusparking.utils

import android.content.Context
import android.os.Environment
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

/**
 * Helper class to export the database for debugging purposes
 */
class DatabaseDebugHelper(private val context: Context) {

    /**
     * Exports the database to the Downloads folder
     * @return true if successful, false otherwise
     */
    fun exportDatabase(): Boolean {
        try {
            // Get the database path
            val dbFile = context.getDatabasePath("campus_parking.db")

            if (!dbFile.exists()) {
                Toast.makeText(context, "Database does not exist", Toast.LENGTH_SHORT).show()
                return false
            }

            // Create the output directory if it doesn't exist
            val exportDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "CampusParking")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }

            // Create the output file
            val exportFile = File(exportDir, "campus_parking_export.db")

            // Copy the database file
            val input = FileInputStream(dbFile)
            val output = FileOutputStream(exportFile)

            val buffer = ByteArray(1024)
            var length: Int
            while (input.read(buffer).also { length = it } > 0) {
                output.write(buffer, 0, length)
            }

            // Close the streams
            output.flush()
            output.close()
            input.close()

            Toast.makeText(
                context,
                "Database exported to Downloads/CampusParking/campus_parking_export.db",
                Toast.LENGTH_LONG
            ).show()

            return true
        } catch (e: IOException) {
            Toast.makeText(
                context,
                "Error exporting database: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
            e.printStackTrace()
            return false
        }
    }
}

