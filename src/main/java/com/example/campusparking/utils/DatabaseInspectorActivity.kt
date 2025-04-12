package com.example.campusparking.utils

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.campusparking.R
import com.example.campusparking.db.DatabaseHelper

class DatabaseInspectorActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var tableSpinner: Spinner
    private lateinit var queryEditText: EditText
    private lateinit var executeButton: Button
    private lateinit var exportButton: Button
    private lateinit var resultsTable: TableLayout

    private val STORAGE_PERMISSION_CODE = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_inspector)

        dbHelper = DatabaseHelper(this)

        tableSpinner = findViewById(R.id.tableSpinner)
        queryEditText = findViewById(R.id.queryEditText)
        executeButton = findViewById(R.id.executeButton)
        exportButton = findViewById(R.id.exportButton)
        resultsTable = findViewById(R.id.resultsTable)

        // Load table names
        loadTableNames()

        // Set up spinner listener
        tableSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val tableName = tableSpinner.selectedItem.toString()
                queryEditText.setText("SELECT * FROM $tableName LIMIT 100")
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        // Set up execute button
        executeButton.setOnClickListener {
            executeQuery(queryEditText.text.toString())
        }

        // Set up export button
        exportButton.setOnClickListener {
            if (checkPermission()) {
                exportDatabase()
            } else {
                requestPermission()
            }
        }
    }

    private fun loadTableNames() {
        val db = dbHelper.readableDatabase
        val tableNames = ArrayList<String>()

        // Query for all table names
        val cursor = db.rawQuery(
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%'",
            null
        )

        if (cursor.moveToFirst()) {
            do {
                tableNames.add(cursor.getString(0))
            } while (cursor.moveToNext())
        }

        cursor.close()

        // Set up spinner adapter
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, tableNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        tableSpinner.adapter = adapter
    }

    private fun executeQuery(query: String) {
        resultsTable.removeAllViews()

        try {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(query, null)

            if (cursor.moveToFirst()) {
                // Add header row
                addHeaderRow(cursor)

                // Add data rows
                do {
                    addDataRow(cursor)
                } while (cursor.moveToNext())
            } else {
                Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show()
            }

            cursor.close()
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun addHeaderRow(cursor: Cursor) {
        val headerRow = TableRow(this)

        for (i in 0 until cursor.columnCount) {
            val textView = TextView(this)
            textView.text = cursor.getColumnName(i)
            textView.setPadding(10, 10, 10, 10)
            textView.setBackgroundResource(R.drawable.bg_status_active)
            textView.setTextColor(ContextCompat.getColor(this, android.R.color.white))
            headerRow.addView(textView)
        }

        resultsTable.addView(headerRow)
    }

    private fun addDataRow(cursor: Cursor) {
        val dataRow = TableRow(this)

        for (i in 0 until cursor.columnCount) {
            val textView = TextView(this)
            textView.text = cursor.getString(i) ?: "null"
            textView.setPadding(10, 10, 10, 10)
            dataRow.addView(textView)
        }

        resultsTable.addView(dataRow)
    }

    private fun exportDatabase() {
        val dbDebugHelper = DatabaseDebugHelper(this)
        dbDebugHelper.exportDatabase()
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
            STORAGE_PERMISSION_CODE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                exportDatabase()
            } else {
                Toast.makeText(
                    this,
                    "Storage permission is required to export the database",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}

