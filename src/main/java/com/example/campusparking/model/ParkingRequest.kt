package com.example.campusparking.model

data class ParkingRequest(
    val id: Int = 0,
    val userId: Int,
    val vehicleNumber: String,
    val vehicleType: String = "2-wheeler", // "2-wheeler" or "4-wheeler"
    val cvBookPath: String,
    val rcBookPath: String,
    val driverLicensePath: String = "", // Added driver's license
    val hasHelmet: Boolean,
    val hasSeatBelt: Boolean,
    val status: String,
    val requestDate: Long
)

