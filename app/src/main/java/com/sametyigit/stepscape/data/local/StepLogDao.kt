package com.sametyigit.stepscape.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface StepLogDao {

    @Query("SELECT * FROM step_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAllStepLogs(userId: String): Flow<List<StepLog>>

    @Query("SELECT * FROM step_logs WHERE userId = :userId AND date = :date ORDER BY timestamp ASC")
    suspend fun getStepLogsByDate(userId: String, date: Long): List<StepLog>

    /** Aggregate total steps for a given day (start-of-day millis). */
    @Query("SELECT COALESCE(SUM(steps), 0) FROM step_logs WHERE userId = :userId AND date = :date")
    suspend fun getTotalStepsForDate(userId: String, date: Long): Int

    @Query("SELECT * FROM step_logs WHERE userId = :userId AND syncedToFirebase = 0")
    suspend fun getUnsyncedLogs(userId: String): List<StepLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepLog(stepLog: StepLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStepLogs(stepLogs: List<StepLog>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStepLogsIfNew(stepLogs: List<StepLog>)

    @Update
    suspend fun updateStepLog(stepLog: StepLog)

    @Query("UPDATE step_logs SET syncedToFirebase = 1 WHERE timestamp = :timestamp")
    suspend fun markAsSynced(timestamp: Long)

    @Query("SELECT * FROM step_logs WHERE userId = :userId AND date >= :startDate AND date <= :endDate ORDER BY timestamp ASC")
    suspend fun getStepLogsBetweenDates(userId: String, startDate: Long, endDate: Long): List<StepLog>

    @Query("SELECT * FROM step_logs WHERE userId = :userId ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentStepLogs(userId: String, limit: Int): List<StepLog>
}