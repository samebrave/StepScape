package com.sametyigit.stepscape.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sametyigit.stepscape.data.health.HealthConnectManager
import com.sametyigit.stepscape.data.local.StepLog
import com.sametyigit.stepscape.data.repository.StepRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

class MainViewModel(
    private val repository: StepRepository,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    companion object {
        const val DAILY_GOAL = 10000
    }

    /** Current Firebase UID — guaranteed non-null because LoginActivity gates access. */
    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    private val _todaySteps = MutableLiveData<Int>()
    val todaySteps: LiveData<Int> = _todaySteps

    private val _weeklySteps = MutableLiveData<List<Pair<String, Int>>>()
    val weeklySteps: LiveData<List<Pair<String, Int>>> = _weeklySteps

    private val _dailyChartData = MutableLiveData<List<Pair<Float, Int>>>()
    val dailyChartData: LiveData<List<Pair<Float, Int>>> = _dailyChartData

    private val _syncStatus = MutableLiveData<String>()
    val syncStatus: LiveData<String> = _syncStatus

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadTodaySteps() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val today = LocalDate.now()
                val dateMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

                // Fetch individual step intervals from Health Connect
                val intervals = healthConnectManager.getStepIntervalsForDate(today)

                // Convert each interval into a separate StepLog
                val stepLogs = intervals.map { interval ->
                    StepLog(
                        timestamp = interval.startTime.toEpochMilli(),
                        date = dateMillis,
                        steps = interval.steps,
                        userId = userId
                    )
                }

                if (stepLogs.isNotEmpty()) {
                    repository.saveStepLogsIfNew(stepLogs)
                }

                val totalSteps = intervals.sumOf { it.steps }
                _todaySteps.value = totalSteps

                syncUnsyncedData()
            } catch (e: Exception) {
                e.printStackTrace()
                loadFromLocalDatabase()
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadFromLocalDatabase() {
        val today = LocalDate.now()
        val dateMillis = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val totalSteps = repository.getTotalStepsForDate(userId, dateMillis)
        _todaySteps.value = totalSteps
    }

    private suspend fun syncRangeToRoom(startDate: LocalDate, endDate: LocalDate) {
        try {
            val intervals = healthConnectManager.getStepIntervalsForDateRange(startDate, endDate)
            val stepLogs = intervals.map { interval ->
                val dateMillis = interval.startTime.atZone(ZoneId.systemDefault())
                    .toLocalDate()
                    .atStartOfDay(ZoneId.systemDefault())
                    .toInstant().toEpochMilli()
                StepLog(
                    timestamp = interval.startTime.toEpochMilli(),
                    date = dateMillis,
                    steps = interval.steps,
                    userId = userId
                )
            }
            if (stepLogs.isNotEmpty()) {
                repository.saveStepLogsIfNew(stepLogs)
            }
        } catch (_: Exception) { }
    }

    fun loadWeeklyData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val weekStart = today.minusDays(6)

            // Single HC call for the whole range
            val hcData = healthConnectManager.getStepsForDateRange(weekStart, today)

            val weekData = mutableListOf<Pair<String, Int>>()
            for (i in 6 downTo 0) {
                val date = today.minusDays(i.toLong())
                val steps = hcData[date] ?: 0
                val dayLabel = date.dayOfWeek.name.take(3)
                weekData.add(dayLabel to steps)
            }
            _weeklySteps.value = weekData

            // Persist in background for Firebase sync
            syncRangeToRoom(weekStart, today)
        }
    }

    fun loadDailyData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val intervals = healthConnectManager.getStepIntervalsForDate(today)
            val sorted = intervals.sortedBy { it.startTime }

            val zone = ZoneId.systemDefault()
            val dataPoints = mutableListOf<Pair<Float, Int>>()

            // Start at midnight with 0 steps
            dataPoints.add(0f to 0)

            var cumulative = 0
            for (interval in sorted) {
                val startZoned = interval.startTime.atZone(zone)
                val startHour = startZoned.hour + startZoned.minute / 60f

                // Hold the line flat at the previous cumulative value
                // until the interval actually starts
                if (dataPoints.last().first < startHour) {
                    dataPoints.add(startHour to cumulative)
                }

                val endZoned = interval.endTime.atZone(zone)
                val endHour = endZoned.hour + endZoned.minute / 60f

                cumulative += interval.steps
                dataPoints.add(endHour to cumulative)
            }

            // Extend to current time so the line reaches "now"
            val now = java.time.LocalTime.now()
            val currentHour = now.hour + now.minute / 60f
            if (dataPoints.last().first < currentHour) {
                dataPoints.add(currentHour to cumulative)
            }

            _dailyChartData.value = dataPoints
        }
    }

    fun loadMonthlyData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val monthStart = today.minusDays(29)

            val hcData = healthConnectManager.getStepsForDateRange(monthStart, today)

            val monthData = mutableListOf<Pair<String, Int>>()
            for (i in 29 downTo 0) {
                val date = today.minusDays(i.toLong())
                val steps = hcData[date] ?: 0
                val dayLabel = date.dayOfMonth.toString()
                monthData.add(dayLabel to steps)
            }
            _weeklySteps.value = monthData

            syncRangeToRoom(monthStart, today)
        }
    }

    fun loadSixMonthData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val rangeStart = today.minusMonths(5).withDayOfMonth(1)

            val hcData = healthConnectManager.getStepsForDateRange(rangeStart, today)

            val sixMonthData = mutableListOf<Pair<String, Int>>()
            for (i in 5 downTo 0) {
                val monthStart = today.minusMonths(i.toLong()).withDayOfMonth(1)
                val monthEnd = if (i == 0) today else monthStart.plusMonths(1).minusDays(1)

                var monthTotal = 0
                var currentDate = monthStart
                while (!currentDate.isAfter(monthEnd)) {
                    monthTotal += hcData[currentDate] ?: 0
                    currentDate = currentDate.plusDays(1)
                }

                val monthLabel = monthStart.month.name.take(3)
                sixMonthData.add(monthLabel to monthTotal)
            }
            _weeklySteps.value = sixMonthData

            syncRangeToRoom(rangeStart, today)
        }
    }

    fun loadYearData() {
        viewModelScope.launch {
            val today = LocalDate.now()
            val rangeStart = today.minusMonths(11).withDayOfMonth(1)

            val hcData = healthConnectManager.getStepsForDateRange(rangeStart, today)

            val yearData = mutableListOf<Pair<String, Int>>()
            for (i in 11 downTo 0) {
                val monthStart = today.minusMonths(i.toLong()).withDayOfMonth(1)
                val monthEnd = if (i == 0) today else monthStart.plusMonths(1).minusDays(1)

                var monthTotal = 0
                var currentDate = monthStart
                while (!currentDate.isAfter(monthEnd)) {
                    monthTotal += hcData[currentDate] ?: 0
                    currentDate = currentDate.plusDays(1)
                }

                val monthLabel = monthStart.month.name.take(3)
                yearData.add(monthLabel to monthTotal)
            }
            _weeklySteps.value = yearData

            syncRangeToRoom(rangeStart, today)
        }
    }

    private suspend fun syncUnsyncedData() {
        try {
            val syncedCount = repository.syncUnsyncedLogsToFirebase(userId)
            if (syncedCount > 0) {
                _syncStatus.value = "Synced $syncedCount records"
            }
        } catch (e: Exception) {
            _syncStatus.value = "Sync failed"
        }
    }

    class Factory(
        private val repository: StepRepository,
        private val healthConnectManager: HealthConnectManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                return MainViewModel(repository, healthConnectManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}