package com.sametyigit.stepscape.data.health

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectManager(private val context: Context) {

    private var healthConnectClient: HealthConnectClient? = null

    init {
        if (isAvailable(context)) {
            healthConnectClient = HealthConnectClient.getOrCreate(context)
        }
    }

    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class)
    )

    suspend fun hasAllPermissions(): Boolean {
        return try {
            healthConnectClient?.permissionController?.getGrantedPermissions()
                ?.containsAll(permissions) ?: false
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getTodaySteps(): Int {
        val today = LocalDate.now()
        val startOfDay = today.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return getStepsBetween(startOfDay, endOfDay)
    }

    suspend fun getStepsForDate(date: LocalDate): Int {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return getStepsBetween(startOfDay, endOfDay)
    }

    /**
     * Data class representing a single Health Connect step interval.
     */
    data class StepInterval(
        val startTime: Instant,
        val endTime: Instant,
        val steps: Int
    )

    suspend fun getStepIntervalsForDate(date: LocalDate): List<StepInterval> {
        val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return try {
            val client = healthConnectClient ?: return emptyList()
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
            val response = client.readRecords(request)
            response.records.map { record ->
                StepInterval(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    steps = record.count.toInt()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun getStepsBetween(start: Instant, end: Instant): Int {
        return try {
            val client = healthConnectClient ?: return 0
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = client.readRecords(request)
            response.records.sumOf { it.count.toInt() }
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }

    suspend fun getStepsForDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): Map<LocalDate, Int> {
        val start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return try {
            val client = healthConnectClient ?: return emptyMap()
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = client.readRecords(request)
            response.records
                .groupBy { record ->
                    record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
                }
                .mapValues { (_, records) -> records.sumOf { it.count.toInt() } }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyMap()
        }
    }

    suspend fun getStepIntervalsForDateRange(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<StepInterval> {
        val start = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val end = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

        return try {
            val client = healthConnectClient ?: return emptyList()
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(start, end)
            )
            val response = client.readRecords(request)
            response.records.map { record ->
                StepInterval(
                    startTime = record.startTime,
                    endTime = record.endTime,
                    steps = record.count.toInt()
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun writeSteps(steps: Int, date: LocalDate = LocalDate.now()) {
        try {
            val client = healthConnectClient ?: return
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = startOfDay.plusSeconds(1)

            val stepsRecord = StepsRecord(
                count = steps.toLong(),
                startTime = startOfDay,
                endTime = endOfDay,
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(startOfDay),
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(endOfDay)
            )
            client.insertRecords(listOf(stepsRecord))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"

        fun isAvailable(context: Context): Boolean {
            val status = HealthConnectClient.getSdkStatus(context)
            return status == HealthConnectClient.SDK_AVAILABLE
        }

        fun getSdkStatus(context: Context): Int {
            return HealthConnectClient.getSdkStatus(context)
        }

        fun createInstallIntent(): Intent {
            return Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("market://details?id=$HEALTH_CONNECT_PACKAGE")
                setPackage("com.android.vending")
            }
        }
    }
}