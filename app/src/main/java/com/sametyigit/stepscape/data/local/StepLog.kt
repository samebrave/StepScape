package com.sametyigit.stepscape.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step_logs")
data class StepLog(
    @PrimaryKey
    val timestamp: Long,       // exact start-time of each Health Connect interval
    val date: Long,            // start-of-day millis, kept for daily queries
    val steps: Int,
    val userId: String,        // Firebase UID — isolates data per user
    val syncedToFirebase: Boolean = false
)