package com.example.campusparking.db

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.campusparking.model.ParkingRequest
import com.example.campusparking.model.ParkingSlot
import com.example.campusparking.model.User
import com.example.campusparking.model.UserBooking
import com.example.campusparking.model.Violation
import java.util.concurrent.TimeUnit
import com.example.campusparking.model.GuestPass

// Add imports for OCR and auto-release functionality
import android.os.Handler
import android.os.Looper
import java.util.Timer
import java.util.TimerTask

class DatabaseHelper(val context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        // Make DATABASE_VERSION accessible outside the class
        const val DATABASE_VERSION = 5 // Increased from 4 to 5
        private const val DATABASE_NAME = "campus_parking.db"

        // Table names
        private const val TABLE_USERS = "users"
        private const val TABLE_PARKING_SLOTS = "parking_slots"
        private const val TABLE_USER_BOOKINGS = "user_bookings"
        private const val TABLE_PARKING_REQUESTS = "parking_requests"
        private const val TABLE_VIOLATIONS = "violations" // New table for violations
        private const val TABLE_GUEST_PASSES = "guest_passes"

        // Common column names
        private const val KEY_ID = "id"

        // Users Table Columns
        private const val KEY_USERNAME = "username"
        private const val KEY_PASSWORD = "password"
        private const val KEY_ROLE = "role"
        private const val KEY_NAME = "name"
        private const val KEY_EMAIL = "email"
        private const val KEY_SALARY = "salary" // Added for salary deductions

        // Parking Slots Table Columns
        private const val KEY_SLOT_NUMBER = "slot_number"
        private const val KEY_SLOT_LOCATION = "location"
        private const val KEY_IS_AVAILABLE = "is_available"
        private const val KEY_VEHICLE_TYPE = "vehicle_type" // Added for vehicle type

        // User Bookings Table Columns
        private const val KEY_USER_ID = "user_id"
        private const val KEY_SLOT_ID = "slot_id"
        private const val KEY_BOOKING_DATE = "booking_date"
        private const val KEY_ENTRY_TIME = "entry_time"
        private const val KEY_EXIT_TIME = "exit_time"
        private const val KEY_STATUS = "status"
        private const val KEY_DURATION = "duration" // Added for duration tracking
        // Add these new constants for expected entry/exit times
        private const val KEY_EXPECTED_ENTRY_TIME = "expected_entry_time"
        private const val KEY_EXPECTED_EXIT_TIME = "expected_exit_time"

        // Parking Requests Table Columns
        private const val KEY_VEHICLE_NUMBER = "vehicle_number"
        private const val KEY_CV_BOOK_PATH = "cv_book_path"
        private const val KEY_RC_BOOK_PATH = "rc_book_path"
        private const val KEY_DRIVER_LICENSE_PATH = "driver_license_path" // Added for driver's license
        private const val KEY_HAS_HELMET = "has_helmet"
        private const val KEY_HAS_SEAT_BELT = "has_seat_belt"
        private const val KEY_REQUEST_DATE = "request_date"
        private const val KEY_LICENSE_VERIFIED = "license_verified"
        private const val KEY_RC_VERIFIED = "rc_verified"
        private const val KEY_CV_VERIFIED = "cv_verified"

        // Violations Table Columns
        private const val KEY_VIOLATION_TYPE = "violation_type"
        private const val KEY_PENALTY_AMOUNT = "penalty_amount"
        private const val KEY_VIOLATION_DATE = "violation_date"
        private const val KEY_DESCRIPTION = "description"

        // Guest Passes Table
        private const val KEY_ISSUE_TIME = "issue_time"
        private const val KEY_EXPIRY_TIME = "expiry_time"
        private const val KEY_CONTACT_PHONE = "contact_phone"
        private const val KEY_CONTACT_EMAIL = "contact_email"
        private const val KEY_PASS_CODE = "pass_code"
        private const val KEY_IS_ACTIVE = "is_active"

        // Add these constants to the companion object
        private const val KEY_ZONE = "zone"
        private const val KEY_IS_RESERVED = "is_reserved"
        private const val KEY_RESERVED_FOR = "reserved_for"
        private const val MAX_PARKING_DURATION = 12 * 60 // 12 hours in minutes
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create Users table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USERNAME TEXT UNIQUE,
                $KEY_PASSWORD TEXT,
                $KEY_ROLE TEXT,
                $KEY_NAME TEXT,
                $KEY_EMAIL TEXT,
                $KEY_SALARY REAL DEFAULT 0.0
            )
        """.trimIndent()

        // Create Parking Slots table
        val createParkingSlotsTable = """
            CREATE TABLE $TABLE_PARKING_SLOTS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_SLOT_NUMBER TEXT UNIQUE,
                $KEY_SLOT_LOCATION TEXT,
                $KEY_IS_AVAILABLE INTEGER DEFAULT 1,
                $KEY_VEHICLE_TYPE TEXT DEFAULT '2-wheeler',
                $KEY_ZONE TEXT DEFAULT 'gate',
                $KEY_IS_RESERVED INTEGER DEFAULT 0,
                $KEY_RESERVED_FOR TEXT DEFAULT ''
            )
        """.trimIndent()

        // Create User Bookings table
        val createUserBookingsTable = """
            CREATE TABLE $TABLE_USER_BOOKINGS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_ID INTEGER,
                $KEY_SLOT_ID INTEGER,
                $KEY_BOOKING_DATE INTEGER,
                $KEY_ENTRY_TIME INTEGER DEFAULT 0,
                $KEY_EXIT_TIME INTEGER DEFAULT 0,
                $KEY_EXPECTED_ENTRY_TIME INTEGER DEFAULT 0,
                $KEY_EXPECTED_EXIT_TIME INTEGER DEFAULT 0,
                $KEY_STATUS TEXT,
                $KEY_VEHICLE_TYPE TEXT DEFAULT '2-wheeler',
                $KEY_DURATION INTEGER DEFAULT 0,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID),
                FOREIGN KEY($KEY_SLOT_ID) REFERENCES $TABLE_PARKING_SLOTS($KEY_ID)
            )
        """.trimIndent()

        // Create Parking Requests table
        val createParkingRequestsTable = """
            CREATE TABLE $TABLE_PARKING_REQUESTS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_ID INTEGER,
                $KEY_VEHICLE_NUMBER TEXT,
                $KEY_VEHICLE_TYPE TEXT DEFAULT '2-wheeler',
                $KEY_CV_BOOK_PATH TEXT,
                $KEY_RC_BOOK_PATH TEXT,
                $KEY_DRIVER_LICENSE_PATH TEXT,
                $KEY_HAS_HELMET INTEGER DEFAULT 0,
                $KEY_HAS_SEAT_BELT INTEGER DEFAULT 0,
                $KEY_STATUS TEXT,
                $KEY_REQUEST_DATE INTEGER,
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID)
            )
        """.trimIndent()

        // Create Violations table
        val createViolationsTable = """
            CREATE TABLE $TABLE_VIOLATIONS (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_USER_ID INTEGER,
                $KEY_VEHICLE_NUMBER TEXT,
                $KEY_VIOLATION_TYPE TEXT,
                $KEY_PENALTY_AMOUNT REAL DEFAULT 500.0,
                $KEY_VIOLATION_DATE INTEGER,
                $KEY_DESCRIPTION TEXT,
                $KEY_STATUS TEXT DEFAULT 'pending',
                FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID)
            )
        """.trimIndent()

        // Create Guest Passes table
        val createGuestPassesTable = """
            CREATE TABLE $TABLE_GUEST_PASSES (
                $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $KEY_VEHICLE_NUMBER TEXT,
                $KEY_VEHICLE_TYPE TEXT,
                $KEY_SLOT_ID INTEGER,
                $KEY_ISSUE_TIME INTEGER,
                $KEY_EXPIRY_TIME INTEGER,
                $KEY_CONTACT_PHONE TEXT,
                $KEY_CONTACT_EMAIL TEXT,
                $KEY_PASS_CODE TEXT UNIQUE,
                $KEY_IS_ACTIVE INTEGER DEFAULT 1,
                FOREIGN KEY($KEY_SLOT_ID) REFERENCES $TABLE_PARKING_SLOTS($KEY_ID)
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createParkingSlotsTable)
        db.execSQL(createUserBookingsTable)
        db.execSQL(createParkingRequestsTable)
        db.execSQL(createViolationsTable)
        db.execSQL(createGuestPassesTable)

        // Add document verification columns to parking requests table
        db.execSQL("ALTER TABLE $TABLE_PARKING_REQUESTS ADD COLUMN $KEY_LICENSE_VERIFIED INTEGER DEFAULT 0")
        db.execSQL("ALTER TABLE $TABLE_PARKING_REQUESTS ADD COLUMN $KEY_RC_VERIFIED INTEGER DEFAULT 0")
        db.execSQL("ALTER TABLE $TABLE_PARKING_REQUESTS ADD COLUMN $KEY_CV_VERIFIED INTEGER DEFAULT 0")

        // Initialize with some parking slots
        initializeParkingSlots(db)
    }

    // Modify the onUpgrade method to add the missing 'zone' column
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            // Add new columns to existing tables
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $KEY_SALARY REAL DEFAULT 0.0")
            db.execSQL("ALTER TABLE $TABLE_PARKING_SLOTS ADD COLUMN $KEY_VEHICLE_TYPE TEXT DEFAULT '2-wheeler'")
            db.execSQL("ALTER TABLE $TABLE_USER_BOOKINGS ADD COLUMN $KEY_VEHICLE_TYPE TEXT DEFAULT '2-wheeler'")
            db.execSQL("ALTER TABLE $TABLE_USER_BOOKINGS ADD COLUMN $KEY_DURATION INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_PARKING_REQUESTS ADD COLUMN $KEY_VEHICLE_TYPE TEXT DEFAULT '2-wheeler'")
            db.execSQL("ALTER TABLE $TABLE_PARKING_REQUESTS ADD COLUMN $KEY_DRIVER_LICENSE_PATH TEXT DEFAULT ''")

            // Create new violations table
            val createViolationsTable = """
                CREATE TABLE $TABLE_VIOLATIONS (
                    $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $KEY_USER_ID INTEGER,
                    $KEY_VEHICLE_NUMBER TEXT,
                    $KEY_VIOLATION_TYPE TEXT,
                    $KEY_PENALTY_AMOUNT REAL DEFAULT 500.0,
                    $KEY_VIOLATION_DATE INTEGER,
                    $KEY_DESCRIPTION TEXT,
                    $KEY_STATUS TEXT DEFAULT 'pending',
                    FOREIGN KEY($KEY_USER_ID) REFERENCES $TABLE_USERS($KEY_ID)
                )
            """.trimIndent()
            db.execSQL(createViolationsTable)
        }

        if (oldVersion < 3) {
            // Create Guest Passes table
            val createGuestPassesTable = """
                CREATE TABLE $TABLE_GUEST_PASSES (
                    $KEY_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                    $KEY_VEHICLE_NUMBER TEXT,
                    $KEY_VEHICLE_TYPE TEXT,
                    $KEY_SLOT_ID INTEGER,
                    $KEY_ISSUE_TIME INTEGER,
                    $KEY_EXPIRY_TIME INTEGER,
                    $KEY_CONTACT_PHONE TEXT,
                    $KEY_CONTACT_EMAIL TEXT,
                    $KEY_PASS_CODE TEXT UNIQUE,
                    $KEY_IS_ACTIVE INTEGER DEFAULT 1,
                    FOREIGN KEY($KEY_SLOT_ID) REFERENCES $TABLE_PARKING_SLOTS($KEY_ID)
                )
            """.trimIndent()

            db.execSQL(createGuestPassesTable)

            // Add document verification columns to parking requests table
            db.execSQL("ALTER TABLE $TABLE_PARKING_REQUESTS ADD COLUMN $KEY_LICENSE_VERIFIED INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_PARKING_REQUESTS ADD COLUMN $KEY_RC_VERIFIED INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_PARKING_REQUESTS ADD COLUMN $KEY_CV_VERIFIED INTEGER DEFAULT 0")
        }

        if (oldVersion < 4) {
            // Add expected entry/exit time columns to user_bookings table
            db.execSQL("ALTER TABLE $TABLE_USER_BOOKINGS ADD COLUMN $KEY_EXPECTED_ENTRY_TIME INTEGER DEFAULT 0")
            db.execSQL("ALTER TABLE $TABLE_USER_BOOKINGS ADD COLUMN $KEY_EXPECTED_EXIT_TIME INTEGER DEFAULT 0")
        }

        if (oldVersion < 5) {
            // Add missing zone, is_reserved, and reserved_for columns to parking_slots table
            try {
                db.execSQL("ALTER TABLE $TABLE_PARKING_SLOTS ADD COLUMN $KEY_ZONE TEXT DEFAULT 'gate'")
            } catch (e: Exception) {
                // Column might already exist, ignore
            }

            try {
                db.execSQL("ALTER TABLE $TABLE_PARKING_SLOTS ADD COLUMN $KEY_IS_RESERVED INTEGER DEFAULT 0")
            } catch (e: Exception) {
                // Column might already exist, ignore
            }

            try {
                db.execSQL("ALTER TABLE $TABLE_PARKING_SLOTS ADD COLUMN $KEY_RESERVED_FOR TEXT DEFAULT ''")
            } catch (e: Exception) {
                // Column might already exist, ignore
            }
        }
    }

    // Update the initializeParkingSlots method to include zones and reserved slots
    private fun initializeParkingSlots(db: SQLiteDatabase) {
        val zones = arrayOf("canteen", "skill_hub", "gate")

        // Create 2-wheeler slots
        for (i in 1..10) {
            val zone = zones[(i-1) % zones.size]
            val values = ContentValues().apply {
                put(KEY_SLOT_NUMBER, "P$i")
                put(KEY_SLOT_LOCATION, "Block ${('A' + (i-1)/4)}")
                put(KEY_IS_AVAILABLE, 1)
                put(KEY_VEHICLE_TYPE, "2-wheeler")
                put(KEY_ZONE, zone)
                put(KEY_IS_RESERVED, 0)
                put(KEY_RESERVED_FOR, "")
            }
            db.insert(TABLE_PARKING_SLOTS, null, values)
        }

        // Create 4-wheeler slots
        for (i in 11..15) {
            val zone = zones[(i-11) % zones.size]
            val values = ContentValues().apply {
                put(KEY_SLOT_NUMBER, "P$i")
                put(KEY_SLOT_LOCATION, "Block ${('A' + (i-11)/2)}")
                put(KEY_IS_AVAILABLE, 1)
                put(KEY_VEHICLE_TYPE, "4-wheeler")
                put(KEY_ZONE, zone)
                put(KEY_IS_RESERVED, 0)
                put(KEY_RESERVED_FOR, "")
            }
            db.insert(TABLE_PARKING_SLOTS, null, values)
        }

        // Create reserved slots
        val reservedSlots = arrayOf(
            Triple("P16", "gate", "VC"),
            Triple("P17", "gate", "ambulance"),
            Triple("P18", "gate", "registrar")
        )

        for ((index, slotInfo) in reservedSlots.withIndex()) {
            val (slotNumber, zone, reservedFor) = slotInfo
            val values = ContentValues().apply {
                put(KEY_SLOT_NUMBER, slotNumber)
                put(KEY_SLOT_LOCATION, "Reserved Area")
                put(KEY_IS_AVAILABLE, 0)
                put(KEY_VEHICLE_TYPE, "4-wheeler")
                put(KEY_ZONE, zone)
                put(KEY_IS_RESERVED, 1)
                put(KEY_RESERVED_FOR, reservedFor)
            }
            db.insert(TABLE_PARKING_SLOTS, null, values)
        }
    }

    // User related methods
    fun addUser(user: User): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USERNAME, user.username)
            put(KEY_PASSWORD, user.password)
            put(KEY_ROLE, user.role)
            put(KEY_NAME, user.name)
            put(KEY_EMAIL, user.email)
            put(KEY_SALARY, user.salary)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    fun getUser(username: String, password: String): User? {
        val db = this.readableDatabase
        var user: User? = null

        try {
            val cursor = db.query(
                TABLE_USERS,
                arrayOf(KEY_ID, KEY_USERNAME, KEY_PASSWORD, KEY_ROLE, KEY_NAME, KEY_EMAIL, KEY_SALARY),
                "$KEY_USERNAME = ? AND $KEY_PASSWORD = ?",
                arrayOf(username, password),
                null, null, null
            )

            if (cursor.moveToFirst()) {
                user = User(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                    password = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)),
                    role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE)),
                    name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
                    email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
                    salary = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_SALARY))
                )
            }
            cursor.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return user
    }

    fun checkUserExists(username: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_ID),
            "$KEY_USERNAME = ?",
            arrayOf(username),
            null, null, null
        )

        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // Deduct salary for violations
    fun deductSalary(userId: Int, amount: Double): Boolean {
        val db = this.writableDatabase
        val user = getUserById(userId)

        if (user != null) {
            val newSalary = user.salary - amount
            val values = ContentValues().apply {
                put(KEY_SALARY, newSalary)
            }

            val result = db.update(
                TABLE_USERS,
                values,
                "$KEY_ID = ?",
                arrayOf(userId.toString())
            )

            return result > 0
        }

        return false
    }

    fun getUserById(userId: Int): User? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USERS,
            arrayOf(KEY_ID, KEY_USERNAME, KEY_PASSWORD, KEY_ROLE, KEY_NAME, KEY_EMAIL, KEY_SALARY),
            "$KEY_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val user = User(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                username = cursor.getString(cursor.getColumnIndexOrThrow(KEY_USERNAME)),
                password = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASSWORD)),
                role = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ROLE)),
                name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_NAME)),
                email = cursor.getString(cursor.getColumnIndexOrThrow(KEY_EMAIL)),
                salary = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_SALARY))
            )
            cursor.close()
            user
        } else {
            cursor.close()
            null
        }
    }

    // Parking Slot related methods
    // Update the getAvailableParkingSlots method to include zone filtering
    fun getAvailableParkingSlots(vehicleType: String = "2-wheeler", zone: String? = null): List<ParkingSlot> {
        val slotsList = mutableListOf<ParkingSlot>()
        val db = this.readableDatabase

        try {
            val selection = if (zone != null) {
                "$KEY_IS_AVAILABLE = ? AND $KEY_VEHICLE_TYPE = ? AND $KEY_ZONE = ? AND $KEY_IS_RESERVED = 0"
            } else {
                "$KEY_IS_AVAILABLE = ? AND $KEY_VEHICLE_TYPE = ? AND $KEY_IS_RESERVED = 0"
            }

            val selectionArgs = if (zone != null) {
                arrayOf("1", vehicleType, zone)
            } else {
                arrayOf("1", vehicleType)
            }

            val cursor = db.query(
                TABLE_PARKING_SLOTS,
                null,
                selection,
                selectionArgs,
                null, null, null
            )

            if (cursor.moveToFirst()) {
                do {
                    val slot = ParkingSlot(
                        id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                        slotNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_NUMBER)),
                        location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_LOCATION)),
                        isAvailable = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_AVAILABLE)) == 1,
                        vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                        zone = getStringOrDefault(cursor, KEY_ZONE, "gate"),
                        isReserved = getIntOrDefault(cursor, KEY_IS_RESERVED, 0) == 1,
                        reservedFor = getStringOrDefault(cursor, KEY_RESERVED_FOR, "")
                    )
                    slotsList.add(slot)
                } while (cursor.moveToNext())
            }
            cursor.close()
        } catch (e: Exception) {
            // Fallback to a simpler query if the columns don't exist
            try {
                val cursor = db.query(
                    TABLE_PARKING_SLOTS,
                    null,
                    "$KEY_IS_AVAILABLE = ? AND $KEY_VEHICLE_TYPE = ?",
                    arrayOf("1", vehicleType),
                    null, null, null
                )

                if (cursor.moveToFirst()) {
                    do {
                        val slot = ParkingSlot(
                            id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                            slotNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_NUMBER)),
                            location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_LOCATION)),
                            isAvailable = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_AVAILABLE)) == 1,
                            vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                            zone = "gate", // Default value
                            isReserved = false, // Default value
                            reservedFor = "" // Default value
                        )
                        slotsList.add(slot)
                    } while (cursor.moveToNext())
                }
                cursor.close()
            } catch (e2: Exception) {
                e2.printStackTrace()
            }
        }
        return slotsList
    }

    // Helper methods to safely get column values
    private fun getStringOrDefault(cursor: android.database.Cursor, columnName: String, defaultValue: String): String {
        return try {
            val columnIndex = cursor.getColumnIndexOrThrow(columnName)
            cursor.getString(columnIndex)
        } catch (e: Exception) {
            defaultValue
        }
    }

    private fun getIntOrDefault(cursor: android.database.Cursor, columnName: String, defaultValue: Int): Int {
        return try {
            val columnIndex = cursor.getColumnIndexOrThrow(columnName)
            cursor.getInt(columnIndex)
        } catch (e: Exception) {
            defaultValue
        }
    }

    // Update the getAllParkingSlots method to include new fields
    fun getAllParkingSlots(): List<ParkingSlot> {
        val slotsList = mutableListOf<ParkingSlot>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_PARKING_SLOTS,
            null,
            null,
            null,
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val slot = ParkingSlot(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    slotNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_NUMBER)),
                    location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_LOCATION)),
                    isAvailable = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_AVAILABLE)) == 1,
                    vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                    zone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ZONE)),
                    isReserved = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_RESERVED)) == 1,
                    reservedFor = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RESERVED_FOR))
                )
                slotsList.add(slot)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return slotsList
    }

    // Update the getParkingSlotById method to include new fields
    fun getParkingSlotById(slotId: Int): ParkingSlot? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_PARKING_SLOTS,
            null,
            "$KEY_ID = ?",
            arrayOf(slotId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val slot = ParkingSlot(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                slotNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_NUMBER)),
                location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_LOCATION)),
                isAvailable = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_AVAILABLE)) == 1,
                vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                zone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ZONE)),
                isReserved = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_RESERVED)) == 1,
                reservedFor = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RESERVED_FOR))
            )
            cursor.close()
            slot
        } else {
            cursor.close()
            null
        }
    }

    fun updateParkingSlotStatus(slotId: Int, isAvailable: Boolean): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_IS_AVAILABLE, if (isAvailable) 1 else 0)
        }
        return db.update(
            TABLE_PARKING_SLOTS,
            values,
            "$KEY_ID = ?",
            arrayOf(slotId.toString())
        )
    }

    fun addParkingSlot(slot: ParkingSlot): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_SLOT_NUMBER, slot.slotNumber)
            put(KEY_SLOT_LOCATION, slot.location)
            put(KEY_IS_AVAILABLE, if (slot.isAvailable) 1 else 0)
            put(KEY_VEHICLE_TYPE, slot.vehicleType)
        }
        return db.insert(TABLE_PARKING_SLOTS, null, values)
    }

    fun getSlotCountByType(vehicleType: String): Pair<Int, Int> {
        val db = this.readableDatabase

        // Get total slots of this type
        val totalCursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_PARKING_SLOTS WHERE $KEY_VEHICLE_TYPE = ?",
            arrayOf(vehicleType)
        )

        // Get available slots of this type
        val availableCursor = db.rawQuery(
            "SELECT COUNT(*) FROM $TABLE_PARKING_SLOTS WHERE $KEY_VEHICLE_TYPE = ? AND $KEY_IS_AVAILABLE = 1",
            arrayOf(vehicleType)
        )

        var total = 0
        var available = 0

        if (totalCursor.moveToFirst()) {
            total = totalCursor.getInt(0)
        }

        if (availableCursor.moveToFirst()) {
            available = availableCursor.getInt(0)
        }

        totalCursor.close()
        availableCursor.close()

        return Pair(available, total)
    }

    // Add a method to get slots by zone
    fun getSlotsByZone(zone: String): List<ParkingSlot> {
        val slotsList = mutableListOf<ParkingSlot>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_PARKING_SLOTS,
            null,
            "$KEY_ZONE = ?",
            arrayOf(zone),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val slot = ParkingSlot(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    slotNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_NUMBER)),
                    location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_LOCATION)),
                    isAvailable = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_AVAILABLE)) == 1,
                    vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                    zone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ZONE)),
                    isReserved = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_RESERVED)) == 1,
                    reservedFor = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RESERVED_FOR))
                )
                slotsList.add(slot)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return slotsList
    }

    // Add a method to get reserved slots
    fun getReservedSlots(): List<ParkingSlot> {
        val slotsList = mutableListOf<ParkingSlot>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_PARKING_SLOTS,
            null,
            "$KEY_IS_RESERVED = 1",
            null,
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val slot = ParkingSlot(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    slotNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_NUMBER)),
                    location = cursor.getString(cursor.getColumnIndexOrThrow(KEY_SLOT_LOCATION)),
                    isAvailable = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_AVAILABLE)) == 1,
                    vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                    zone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_ZONE)),
                    isReserved = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_RESERVED)) == 1,
                    reservedFor = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RESERVED_FOR))
                )
                slotsList.add(slot)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return slotsList
    }

    // Booking related methods
    fun addBooking(booking: UserBooking): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_ID, booking.userId)
            put(KEY_SLOT_ID, booking.slotId)
            put(KEY_BOOKING_DATE, booking.bookingDate)
            put(KEY_STATUS, booking.status)
            put(KEY_VEHICLE_TYPE, booking.vehicleType)
            put(KEY_EXPECTED_ENTRY_TIME, booking.expectedEntryTime)
            put(KEY_EXPECTED_EXIT_TIME, booking.expectedExitTime)
        }
        return db.insert(TABLE_USER_BOOKINGS, null, values)
    }

    fun getUserBookings(userId: Int): List<UserBooking> {
        val bookingsList = mutableListOf<UserBooking>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USER_BOOKINGS,
            null,
            "$KEY_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val booking = UserBooking(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                    slotId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SLOT_ID)),
                    bookingDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_BOOKING_DATE)),
                    entryTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ENTRY_TIME)),
                    exitTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXIT_TIME)),
                    expectedEntryTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXPECTED_ENTRY_TIME)),
                    expectedExitTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXPECTED_EXIT_TIME)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
                    vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                    duration = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DURATION))
                )
                bookingsList.add(booking)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return bookingsList
    }

    fun cancelBooking(bookingId: Int): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_STATUS, "cancelled")
        }
        return db.update(
            TABLE_USER_BOOKINGS,
            values,
            "$KEY_ID = ?",
            arrayOf(bookingId.toString())
        )
    }

    fun getBookingByVehicleNumber(vehicleNumber: String): UserBooking? {
        val db = this.readableDatabase
        val query = """
            SELECT b.* FROM $TABLE_USER_BOOKINGS b
            JOIN $TABLE_PARKING_REQUESTS r ON b.$KEY_USER_ID = r.$KEY_USER_ID
            WHERE r.$KEY_VEHICLE_NUMBER = ? AND b.$KEY_STATUS = 'active'
        """.trimIndent()

        val cursor = db.rawQuery(query, arrayOf(vehicleNumber))

        return if (cursor.moveToFirst()) {
            val booking = UserBooking(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                slotId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SLOT_ID)),
                bookingDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_BOOKING_DATE)),
                entryTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ENTRY_TIME)),
                exitTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXIT_TIME)),
                expectedEntryTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXPECTED_ENTRY_TIME)),
                expectedExitTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXPECTED_EXIT_TIME)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
                vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                duration = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DURATION))
            )
            cursor.close()
            booking
        } else {
            cursor.close()
            null
        }
    }

    fun updateBookingEntryTime(bookingId: Int, entryTime: Long): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_ENTRY_TIME, entryTime)
        }
        return db.update(
            TABLE_USER_BOOKINGS,
            values,
            "$KEY_ID = ?",
            arrayOf(bookingId.toString())
        )
    }

    fun updateBookingExitTime(bookingId: Int, exitTime: Long): Int {
        val db = this.writableDatabase
        val booking = getBookingById(bookingId)

        if (booking != null && booking.entryTime > 0) {
            val duration = calculateDuration(booking.entryTime, exitTime)

            val values = ContentValues().apply {
                put(KEY_EXIT_TIME, exitTime)
                put(KEY_DURATION, duration)
                put(KEY_STATUS, "completed")
            }

            return db.update(
                TABLE_USER_BOOKINGS,
                values,
                "$KEY_ID = ?",
                arrayOf(bookingId.toString())
            )
        }

        return 0
    }

    private fun calculateDuration(entryTime: Long, exitTime: Long): Long {
        return TimeUnit.MILLISECONDS.toMinutes(exitTime - entryTime)
    }

    fun getBookingById(bookingId: Int): UserBooking? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_USER_BOOKINGS,
            null,
            "$KEY_ID = ?",
            arrayOf(bookingId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val booking = UserBooking(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                slotId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SLOT_ID)),
                bookingDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_BOOKING_DATE)),
                entryTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ENTRY_TIME)),
                exitTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXIT_TIME)),
                expectedEntryTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXPECTED_ENTRY_TIME)),
                expectedExitTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXPECTED_EXIT_TIME)),
                status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
                vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                duration = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_DURATION))
            )
            cursor.close()
            booking
        } else {
            cursor.close()
            null
        }
    }

    // Parking Request related methods
    fun addParkingRequest(request: ParkingRequest): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_ID, request.userId)
            put(KEY_VEHICLE_NUMBER, request.vehicleNumber)
            put(KEY_VEHICLE_TYPE, request.vehicleType)
            put(KEY_CV_BOOK_PATH, request.cvBookPath)
            put(KEY_RC_BOOK_PATH, request.rcBookPath)
            put(KEY_DRIVER_LICENSE_PATH, request.driverLicensePath)
            put(KEY_HAS_HELMET, if (request.hasHelmet) 1 else 0)
            put(KEY_HAS_SEAT_BELT, if (request.hasSeatBelt) 1 else 0)
            put(KEY_STATUS, request.status)
            put(KEY_REQUEST_DATE, request.requestDate)
        }
        return db.insert(TABLE_PARKING_REQUESTS, null, values)
    }

    fun getPendingRequests(): List<ParkingRequest> {
        return getRequestsByStatus("pending")
    }

    fun getApprovedRequests(): List<ParkingRequest> {
        return getRequestsByStatus("approved")
    }

    // Add this method to fix the unresolved reference error
    fun getPendingRequestsByUserId(userId: Int): List<ParkingRequest> {
        val requestsList = mutableListOf<ParkingRequest>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_PARKING_REQUESTS,
            null,
            "$KEY_USER_ID = ? AND $KEY_STATUS = ?",
            arrayOf(userId.toString(), "pending"),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val request = ParkingRequest(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                    vehicleNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_NUMBER)),
                    vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                    cvBookPath = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CV_BOOK_PATH)),
                    rcBookPath = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RC_BOOK_PATH)),
                    driverLicensePath = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DRIVER_LICENSE_PATH)),
                    hasHelmet = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HAS_HELMET)) == 1,
                    hasSeatBelt = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HAS_SEAT_BELT)) == 1,
                    status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
                    requestDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_REQUEST_DATE))
                )
                requestsList.add(request)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return requestsList
    }

    private fun getRequestsByStatus(status: String): List<ParkingRequest> {
        val requestsList = mutableListOf<ParkingRequest>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_PARKING_REQUESTS,
            null,
            "$KEY_STATUS = ?",
            arrayOf(status),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val request = ParkingRequest(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                    vehicleNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_NUMBER)),
                    vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                    cvBookPath = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CV_BOOK_PATH)),
                    rcBookPath = cursor.getString(cursor.getColumnIndexOrThrow(KEY_RC_BOOK_PATH)),
                    driverLicensePath = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DRIVER_LICENSE_PATH)),
                    hasHelmet = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HAS_HELMET)) == 1,
                    hasSeatBelt = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HAS_SEAT_BELT)) == 1,
                    status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS)),
                    requestDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_REQUEST_DATE))
                )
                requestsList.add(request)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return requestsList
    }

    fun approveRequest(requestId: Int): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_STATUS, "approved")
        }
        return db.update(
            TABLE_PARKING_REQUESTS,
            values,
            "$KEY_ID = ?",
            arrayOf(requestId.toString())
        )
    }

    fun rejectRequest(requestId: Int): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_STATUS, "rejected")
        }
        return db.update(
            TABLE_PARKING_REQUESTS,
            values,
            "$KEY_ID = ?",
            arrayOf(requestId.toString())
        )
    }

    // Violation related methods
    fun addViolation(violation: Violation): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_USER_ID, violation.userId)
            put(KEY_VEHICLE_NUMBER, violation.vehicleNumber)
            put(KEY_VIOLATION_TYPE, violation.violationType)
            put(KEY_PENALTY_AMOUNT, violation.penaltyAmount)
            put(KEY_VIOLATION_DATE, violation.violationDate)
            put(KEY_DESCRIPTION, violation.description)
            put(KEY_STATUS, violation.status)
        }

        // Deduct salary for the violation
        deductSalary(violation.userId, violation.penaltyAmount)

        return db.insert(TABLE_VIOLATIONS, null, values)
    }

    fun getViolationsByUser(userId: Int): List<Violation> {
        val violationsList = mutableListOf<Violation>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_VIOLATIONS,
            null,
            "$KEY_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val violation = Violation(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                    vehicleNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_NUMBER)),
                    violationType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VIOLATION_TYPE)),
                    penaltyAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PENALTY_AMOUNT)),
                    violationDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_VIOLATION_DATE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS))
                )
                violationsList.add(violation)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return violationsList
    }

    fun getAllViolations(): List<Violation> {
        val violationsList = mutableListOf<Violation>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_VIOLATIONS,
            null,
            null,
            null,
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val violation = Violation(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID)),
                    vehicleNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_NUMBER)),
                    violationType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VIOLATION_TYPE)),
                    penaltyAmount = cursor.getDouble(cursor.getColumnIndexOrThrow(KEY_PENALTY_AMOUNT)),
                    violationDate = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_VIOLATION_DATE)),
                    description = cursor.getString(cursor.getColumnIndexOrThrow(KEY_DESCRIPTION)),
                    status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_STATUS))
                )
                violationsList.add(violation)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return violationsList
    }

    // Guest Pass related methods
    fun addGuestPass(guestPass: GuestPass): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_VEHICLE_NUMBER, guestPass.vehicleNumber)
            put(KEY_VEHICLE_TYPE, guestPass.vehicleType)
            put(KEY_SLOT_ID, guestPass.slotId)
            put(KEY_ISSUE_TIME, guestPass.issueTime)
            put(KEY_EXPIRY_TIME, guestPass.expiryTime)
            put(KEY_CONTACT_PHONE, guestPass.contactPhone)
            put(KEY_CONTACT_EMAIL, guestPass.contactEmail)
            put(KEY_PASS_CODE, guestPass.passCode)
            put(KEY_IS_ACTIVE, if (guestPass.isActive) 1 else 0)
        }

        // Update the slot status to unavailable
        updateParkingSlotStatus(guestPass.slotId, false)

        return db.insert(TABLE_GUEST_PASSES, null, values)
    }

    fun getActiveGuestPassByVehicle(vehicleNumber: String): GuestPass? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_GUEST_PASSES,
            null,
            "$KEY_VEHICLE_NUMBER = ? AND $KEY_IS_ACTIVE = 1 AND $KEY_EXPIRY_TIME > ?",
            arrayOf(vehicleNumber, System.currentTimeMillis().toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val guestPass = GuestPass(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                vehicleNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_NUMBER)),
                vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                slotId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SLOT_ID)),
                issueTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ISSUE_TIME)),
                expiryTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXPIRY_TIME)),
                contactPhone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_PHONE)),
                contactEmail = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_EMAIL)),
                passCode = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASS_CODE)),
                isActive = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_ACTIVE)) == 1
            )
            cursor.close()
            guestPass
        } else {
            cursor.close()
            null
        }
    }

    fun deactivateGuestPass(passId: Int): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(KEY_IS_ACTIVE, 0)
        }

        // Get the slot ID to free it up
        val guestPass = getGuestPassById(passId)
        if (guestPass != null) {
            updateParkingSlotStatus(guestPass.slotId, true)
        }

        return db.update(
            TABLE_GUEST_PASSES,
            values,
            "$KEY_ID = ?",
            arrayOf(passId.toString())
        )
    }

    fun getGuestPassById(passId: Int): GuestPass? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_GUEST_PASSES,
            null,
            "$KEY_ID = ?",
            arrayOf(passId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val guestPass = GuestPass(
                id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                vehicleNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_NUMBER)),
                vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                slotId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SLOT_ID)),
                issueTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ISSUE_TIME)),
                expiryTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXPIRY_TIME)),
                contactPhone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_PHONE)),
                contactEmail = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_EMAIL)),
                passCode = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASS_CODE)),
                isActive = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_ACTIVE)) == 1
            )
            cursor.close()
            guestPass
        } else {
            cursor.close()
            null
        }
    }

    fun getAllActiveGuestPasses(): List<GuestPass> {
        val passList = mutableListOf<GuestPass>()
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_GUEST_PASSES,
            null,
            "$KEY_IS_ACTIVE = 1 AND $KEY_EXPIRY_TIME > ?",
            arrayOf(System.currentTimeMillis().toString()),
            null, null, null
        )

        if (cursor.moveToFirst()) {
            do {
                val guestPass = GuestPass(
                    id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    vehicleNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_NUMBER)),
                    vehicleType = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE)),
                    slotId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SLOT_ID)),
                    issueTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ISSUE_TIME)),
                    expiryTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_EXPIRY_TIME)),
                    contactPhone = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_PHONE)),
                    contactEmail = cursor.getString(cursor.getColumnIndexOrThrow(KEY_CONTACT_EMAIL)),
                    passCode = cursor.getString(cursor.getColumnIndexOrThrow(KEY_PASS_CODE)),
                    isActive = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_ACTIVE)) == 1
                )
                passList.add(guestPass)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return passList
    }

    // Document verification methods
    fun markDocumentVerified(requestId: Int, documentType: String, isVerified: Boolean): Int {
        val db = this.writableDatabase
        val values = ContentValues()

        when (documentType) {
            "license" -> values.put(KEY_LICENSE_VERIFIED, if (isVerified) 1 else 0)
            "rc" -> values.put(KEY_RC_VERIFIED, if (isVerified) 1 else 0)
            "cv" -> values.put(KEY_CV_VERIFIED, if (isVerified) 1 else 0)
        }

        return db.update(
            TABLE_PARKING_REQUESTS,
            values,
            "$KEY_ID = ?",
            arrayOf(requestId.toString())
        )
    }

    // Auto-allocate a slot for a guest vehicle
    fun autoAllocateSlotForGuest(vehicleType: String): ParkingSlot? {
        val availableSlots = getAvailableParkingSlots(vehicleType)
        return availableSlots.firstOrNull()
    }

    // Check if a vehicle is registered (has an approved request)
    fun isVehicleRegistered(vehicleNumber: String): Boolean {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_PARKING_REQUESTS,
            arrayOf(KEY_ID),
            "$KEY_VEHICLE_NUMBER = ? AND $KEY_STATUS = 'approved'",
            arrayOf(vehicleNumber),
            null, null, null
        )

        val isRegistered = cursor.count > 0
        cursor.close()
        return isRegistered
    }

    // Get vehicle details by number plate
    fun getVehicleDetailsByNumber(vehicleNumber: String): Map<String, Any?> {
        val details = mutableMapOf<String, Any?>()
        val db = this.readableDatabase

        // Check if it's a registered vehicle
        val isRegistered = isVehicleRegistered(vehicleNumber)
        details["isRegistered"] = isRegistered

        if (isRegistered) {
            // Get request details
            val requestCursor = db.query(
                TABLE_PARKING_REQUESTS,
                null,
                "$KEY_VEHICLE_NUMBER = ? AND $KEY_STATUS = 'approved'",
                arrayOf(vehicleNumber),
                null, null, null
            )

            if (requestCursor.moveToFirst()) {
                details["vehicleType"] = requestCursor.getString(requestCursor.getColumnIndexOrThrow(KEY_VEHICLE_TYPE))
                details["userId"] = requestCursor.getInt(requestCursor.getColumnIndexOrThrow(KEY_USER_ID))
            }
            requestCursor.close()

            // Get violation history
            val violationCursor = db.query(
                TABLE_VIOLATIONS,
                null,
                "$KEY_VEHICLE_NUMBER = ?",
                arrayOf(vehicleNumber),
                null, null, null
            )

            details["violationCount"] = violationCursor.count
            violationCursor.close()

            // Get current booking if any
            val booking = getBookingByVehicleNumber(vehicleNumber)
            details["currentBooking"] = booking
        } else {
            // Check if it's a guest with active pass
            val guestPass = getActiveGuestPassByVehicle(vehicleNumber)
            details["guestPass"] = guestPass
        }

        return details
    }

    // Generate a unique pass code
    fun generatePassCode(): String {
        val allowedChars = ('A'..'Z') + ('0'..'9')
        return (1..6).map { allowedChars.random() }.joinToString("")
    }

    // Add a method to check for overstays and auto-release slots
    fun checkAndReleaseOverstayedSlots() {
        val db = this.writableDatabase
        val currentTime = System.currentTimeMillis()

        // Get all active bookings
        val cursor = db.query(
            TABLE_USER_BOOKINGS,
            null,
            "$KEY_STATUS = 'active' AND $KEY_ENTRY_TIME > 0",
            null,
            null, null, null
        )

        val overstayedBookings = mutableListOf<UserBooking>()

        if (cursor.moveToFirst()) {
            do {
                val bookingId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID))
                val entryTime = cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ENTRY_TIME))
                val slotId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_SLOT_ID))
                val userId = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_USER_ID))

                // Calculate duration in minutes
                val durationMinutes = TimeUnit.MILLISECONDS.toMinutes(currentTime - entryTime)

                // Check if duration exceeds maximum allowed (12 hours)
                if (durationMinutes > MAX_PARKING_DURATION) {
                    // Auto-release the slot
                    updateParkingSlotStatus(slotId, true)

                    // Update booking status to "completed"
                    val values = ContentValues().apply {
                        put(KEY_STATUS, "completed")
                        put(KEY_EXIT_TIME, currentTime)
                        put(KEY_DURATION, durationMinutes)
                    }

                    db.update(
                        TABLE_USER_BOOKINGS,
                        values,
                        "$KEY_ID = ?",
                        arrayOf(bookingId.toString())
                    )

                    // Create a violation for overstay
                    val booking = getBookingById(bookingId)
                    if (booking != null) {
                        val vehicleNumber = getVehicleNumberByUserId(userId)
                        if (vehicleNumber != null) {
                            val violation = Violation(
                                userId = userId,
                                vehicleNumber = vehicleNumber,
                                violationType = "overstay",
                                penaltyAmount = 500.0,
                                violationDate = currentTime,
                                description = "Exceeded maximum parking duration of 12 hours (actual: ${durationMinutes / 60} hours ${durationMinutes % 60} minutes)",
                                status = "pending"
                            )
                            addViolation(violation)
                        }
                    }
                }
            } while (cursor.moveToNext())
        }
        cursor.close()
    }

    // Add a method to get vehicle number by user ID
    fun getVehicleNumberByUserId(userId: Int): String? {
        val db = this.readableDatabase
        val cursor = db.query(
            TABLE_PARKING_REQUESTS,
            arrayOf(KEY_VEHICLE_NUMBER),
            "$KEY_USER_ID = ? AND $KEY_STATUS = 'approved'",
            arrayOf(userId.toString()),
            null, null, null
        )

        return if (cursor.moveToFirst()) {
            val vehicleNumber = cursor.getString(cursor.getColumnIndexOrThrow(KEY_VEHICLE_NUMBER))
            cursor.close()
            vehicleNumber
        } else {
            cursor.close()
            null
        }
    }

    // Add a method to recognize vehicle number using OCR (simulated)
    fun recognizeVehicleNumber(imagePath: String): String? {
        // In a real app, this would use an OCR library to extract text from the image
        // For this simulation, we'll return a random vehicle number
        val letters = ('A'..'Z').toList()
        val numbers = ('0'..'9').toList()

        val stateCode = "KA"
        val districtCode = (1..99).random().toString().padStart(2, '0')
        val series = letters.shuffled().take(2).joinToString("")
        val number = (1000..9999).random().toString()

        return "$stateCode-$districtCode-$series-$number"
    }

    // Add a method to schedule auto-release checks
    private var autoReleaseTimer: Timer? = null

    fun startAutoReleaseScheduler() {
        if (autoReleaseTimer == null) {
            autoReleaseTimer = Timer()
            autoReleaseTimer?.scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    checkAndReleaseOverstayedSlots()
                }
            }, 0, 15 * 60 * 1000) // Check every 15 minutes
        }
    }

    fun stopAutoReleaseScheduler() {
        autoReleaseTimer?.cancel()
        autoReleaseTimer = null
    }
}

