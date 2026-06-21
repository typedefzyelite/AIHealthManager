package com.example.aihealthmanager_2

data class MedicineItem(
    val name: String,
    var expiryDate: String = "",
    var quantity: Int = 1,
    val addedDate: String = "",
    var frequency: Int = 1
)
