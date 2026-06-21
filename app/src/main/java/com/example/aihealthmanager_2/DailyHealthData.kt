package com.example.aihealthmanager_2

data class DailyHealthData(
    val date: String,
    var heartRate: Int,
    var steps: Int,
    var bloodOxygen: Double,
    var sleepDuration: Double
)