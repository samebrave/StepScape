package com.sametyigit.stepscape.data.remote

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.sametyigit.stepscape.data.local.StepLog
import kotlinx.coroutines.tasks.await

class FirebaseService {

    private val firestore = FirebaseFirestore.getInstance()

    private fun stepsCollection() =
        FirebaseAuth.getInstance().currentUser?.uid?.let { uid ->
            firestore.collection("users").document(uid).collection("step_logs")
        } ?: firestore.collection("step_logs")

    suspend fun syncStepLog(stepLog: StepLog): Boolean {
        return try {
            val user = FirebaseAuth.getInstance().currentUser
            val data = hashMapOf(
                "timestamp" to stepLog.timestamp,
                "date" to stepLog.date,
                "steps" to stepLog.steps,
                "userName" to (user?.displayName ?: "Unknown"),
                "syncedAt" to System.currentTimeMillis()
            )
            stepsCollection()
                .document(stepLog.timestamp.toString())
                .set(data)
                .await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun syncMultipleStepLogs(stepLogs: List<StepLog>): List<Long> {
        val syncedTimestamps = mutableListOf<Long>()
        stepLogs.forEach { stepLog ->
            if (syncStepLog(stepLog)) {
                syncedTimestamps.add(stepLog.timestamp)
            }
        }
        return syncedTimestamps
    }
}