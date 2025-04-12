package com.example.campusparking.model

data class ParkingSlot(
    val id: Int = 0,
    val slotNumber: String,
    val location: String,
    val isAvailable: Boolean,
    val vehicleType: String = "2-wheeler", // "2-wheeler" or "4-wheeler"
    val zone: String = "gate", // "canteen", "skill_hub", or "gate"
    val isReserved: Boolean = false,
    val reservedFor: String = "" // "VC", "ambulance", "registrar", or empty
)

