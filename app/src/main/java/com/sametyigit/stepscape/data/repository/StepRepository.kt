package com.sametyigit.stepscape.data.repository

import com.sametyigit.stepscape.data.local.StepLog
import com.sametyigit.stepscape.data.local.StepLogDao
import com.sametyigit.stepscape.data.remote.FirebaseService
import kotlinx.coroutines.flow.Flow

class StepRepository(
    private val stepLogDao: StepLogDao,
    private val firebaseService: FirebaseService
) {

    fun getAllStepLogs(userId: String): Flow<List<StepLog>> = stepLogDao.getAllStepLogs(userId)

    suspend fun getStepLogsByDate(userId: String, date: Long): List<StepLog> =
        stepLogDao.getStepLogsByDate(userId, date)

    suspend fun getTotalStepsForDate(userId: String, date: Long): Int =
        stepLogDao.getTotalStepsForDate(userId, date)

    suspend fun saveStepLog(stepLog: StepLog) {
        stepLogDao.insertStepLog(stepLog)
    }

    suspend fun saveStepLogs(stepLogs: List<StepLog>) {
        stepLogDao.insertStepLogs(stepLogs)
    }

    suspend fun saveStepLogsIfNew(stepLogs: List<StepLog>) {
        stepLogDao.insertStepLogsIfNew(stepLogs)
    }

    suspend fun getStepLogsBetweenDates(userId: String, startDate: Long, endDate: Long): List<StepLog> {
        return stepLogDao.getStepLogsBetweenDates(userId, startDate, endDate)
    }

    suspend fun getRecentStepLogs(userId: String, limit: Int): List<StepLog> {
        return stepLogDao.getRecentStepLogs(userId, limit)
    }

    suspend fun syncUnsyncedLogsToFirebase(userId: String): Int {
        val unsyncedLogs = stepLogDao.getUnsyncedLogs(userId)
        if (unsyncedLogs.isEmpty()) return 0

        val syncedTimestamps = firebaseService.syncMultipleStepLogs(unsyncedLogs)
        syncedTimestamps.forEach { ts ->
            stepLogDao.markAsSynced(ts)
        }
        return syncedTimestamps.size
    }
}