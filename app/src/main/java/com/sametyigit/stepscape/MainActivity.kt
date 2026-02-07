package com.sametyigit.stepscape

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.sametyigit.stepscape.data.health.HealthConnectManager
import com.sametyigit.stepscape.databinding.ActivityMainBinding
import com.sametyigit.stepscape.ui.main.MainViewModel
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    private enum class ChartPeriod { DAY, WEEK, MONTH, SIX_MONTH, YEAR }
    private var currentPeriod = ChartPeriod.DAY

    private val healthConnectPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        val app = application as StepScapeApplication
        if (grantedPermissions.containsAll(app.healthConnectManager.permissions)) {
            loadHealthConnectData()
        } else {
            showPermissionDeniedState()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupChart()
        setupClickListeners()
        setupChipListeners()
        observeViewModel()

        initializeHealthConnect()
    }

    private fun setupViewModel() {
        val app = application as StepScapeApplication
        viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(app.repository, app.healthConnectManager)
        )[MainViewModel::class.java]
    }

    private fun setupChart() {
        binding.lineChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setTouchEnabled(true)
            setDrawGridBackground(false)
            setBackgroundColor(Color.TRANSPARENT)

            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(true)
                setDrawAxisLine(true)   // bottom border line
                gridColor = ContextCompat.getColor(context, R.color.neutral_n400)
                gridLineWidth = 0.5f
                axisLineColor = ContextCompat.getColor(context, R.color.neutral_n400)
                axisLineWidth = 0.5f
                granularity = 1f
                textColor = ContextCompat.getColor(context, R.color.neutral_n400)
                textSize = 11f
            }

            // Left axis: border line only, no labels (Figma puts values on right)
            axisLeft.apply {
                setDrawGridLines(true)
                setDrawLabels(false)
                setDrawAxisLine(true)   // left border line
                gridColor = ContextCompat.getColor(context, R.color.neutral_n400)
                gridLineWidth = 0.5f
                axisLineColor = ContextCompat.getColor(context, R.color.neutral_n400)
                axisLineWidth = 0.5f
                axisMinimum = 0f
            }

            // Right axis: labels visible (matches Figma Y-axis on right)
            axisRight.apply {
                isEnabled = true
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = ContextCompat.getColor(context, R.color.neutral_n400)
                textSize = 11f
                axisMinimum = 0f
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnLogs.setOnClickListener {
            startActivity(Intent(this, LogsActivity::class.java))
        }
    }

    private fun setupChipListeners() {
        binding.chipDay.setOnClickListener { selectChip(ChartPeriod.DAY) }
        binding.chipWeek.setOnClickListener { selectChip(ChartPeriod.WEEK) }
        binding.chipMonth.setOnClickListener { selectChip(ChartPeriod.MONTH) }
        binding.chip6Month.setOnClickListener { selectChip(ChartPeriod.SIX_MONTH) }
        binding.chipYear.setOnClickListener { selectChip(ChartPeriod.YEAR) }
    }

    private fun selectChip(period: ChartPeriod) {
        currentPeriod = period

        val chips = listOf(binding.chipDay, binding.chipWeek, binding.chipMonth, binding.chip6Month, binding.chipYear)
        chips.forEach { chip ->
            chip.background = null
            chip.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        }

        val selectedChip = when (period) {
            ChartPeriod.DAY -> binding.chipDay
            ChartPeriod.WEEK -> binding.chipWeek
            ChartPeriod.MONTH -> binding.chipMonth
            ChartPeriod.SIX_MONTH -> binding.chip6Month
            ChartPeriod.YEAR -> binding.chipYear
        }
        selectedChip.setBackgroundResource(R.drawable.bg_chip_selected_segment)
        selectedChip.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

        when (period) {
            ChartPeriod.DAY -> viewModel.loadDailyData()
            ChartPeriod.WEEK -> viewModel.loadWeeklyData()
            ChartPeriod.MONTH -> viewModel.loadMonthlyData()
            ChartPeriod.SIX_MONTH -> viewModel.loadSixMonthData()
            ChartPeriod.YEAR -> viewModel.loadYearData()
        }
    }

    private fun observeViewModel() {
        viewModel.todaySteps.observe(this) { steps ->
            updateStepsUI(steps)
            // When today's steps arrive, refresh the daily chart if Day is selected
            if (currentPeriod == ChartPeriod.DAY) {
                viewModel.loadDailyData()
            }
        }

        viewModel.weeklySteps.observe(this) { data ->
            updateChart(data)
        }

        viewModel.dailyChartData.observe(this) { data ->
            updateDailyChart(data)
        }

        viewModel.syncStatus.observe(this) { status ->
            Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStepsUI(steps: Int) {
        // Update step count display (no comma formatting per Figma)
        binding.tvStepCount.text = steps.toString()

        val progress = ((steps.toFloat() / MainViewModel.DAILY_GOAL) * 100).toInt().coerceAtMost(100)
        binding.circularProgress.setProgress(progress)

        // Update goal status text based on current step count
        binding.tvGoalStatus.text = getEncouragementText(steps)

        // Update steps label with goal
        binding.tvStepsLabel.text = "/${MainViewModel.DAILY_GOAL}"
    }

    private fun getEncouragementText(steps: Int): String {
        return when {
            steps >= MainViewModel.DAILY_GOAL -> getString(R.string.goal_encouragement_done)
            steps >= 8000 -> getString(R.string.goal_encouragement_8000)
            steps >= 6000 -> getString(R.string.goal_encouragement_6000)
            steps >= 4000 -> getString(R.string.goal_encouragement_4000)
            steps >= 2000 -> getString(R.string.goal_encouragement_2000)
            else -> getString(R.string.goal_encouragement_0)
        }
    }

    private fun updateChart(data: List<Pair<String, Int>>) {
        val entries = data.mapIndexed { index, pair ->
            Entry(index.toFloat(), pair.second.toFloat())
        }

        // Total steps for the range (non-daily views only)
        val totalSteps = data.sumOf { it.second }
        binding.tvChartSteps.text = totalSteps.toString()

        // Update date range text based on current period
        updateDateRangeText()

        // Use data labels for X-axis (they come from ViewModel)
        val xLabels = data.map { it.first }

        val dataSet = LineDataSet(entries, "Steps").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.blue_graph)
            lineWidth = 8.8f  // 60% more bold than 5.5f to match Figma
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER  // Snake-like smooth curve
            cubicIntensity = 0.15f  // Gentle smoothing matching Figma
            setDrawFilled(false)
        }

        binding.lineChart.apply {
            xAxis.valueFormatter = IndexAxisValueFormatter(xLabels)
            xAxis.resetAxisMinimum()
            xAxis.resetAxisMaximum()
            xAxis.labelCount = when (currentPeriod) {
                ChartPeriod.DAY -> 5
                ChartPeriod.WEEK -> 7
                ChartPeriod.MONTH -> 5  // Show fewer labels for readability
                ChartPeriod.SIX_MONTH -> 6
                ChartPeriod.YEAR -> 6  // Show fewer labels for readability
            }
            xAxis.setAvoidFirstLastClipping(true)
            this.data = LineData(dataSet)
            animateX(500)
            invalidate()
        }
    }

    private fun updateDateRangeText() {
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        val yearFormat = SimpleDateFormat("yyyy", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val rangeText = when (currentPeriod) {
            ChartPeriod.DAY -> {
                "Today, ${dateFormat.format(calendar.time)}"
            }
            ChartPeriod.WEEK -> {
                val end = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, -6)
                val start = dateFormat.format(calendar.time)
                "$start-$end, ${yearFormat.format(Calendar.getInstance().time)}"
            }
            ChartPeriod.MONTH -> {
                val end = dateFormat.format(calendar.time)
                calendar.add(Calendar.DAY_OF_YEAR, -29)
                val start = dateFormat.format(calendar.time)
                "$start-$end, ${yearFormat.format(Calendar.getInstance().time)}"
            }
            ChartPeriod.SIX_MONTH -> {
                val end = dateFormat.format(calendar.time)
                calendar.add(Calendar.MONTH, -5)
                val start = dateFormat.format(calendar.time)
                "$start-$end"
            }
            ChartPeriod.YEAR -> {
                val monthYearFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                val end = monthYearFormat.format(calendar.time)
                calendar.add(Calendar.MONTH, -11)
                val start = monthYearFormat.format(calendar.time)
                "$start - $end"
            }
        }
        binding.tvDateRange.text = rangeText
    }

    private fun initializeHealthConnect() {
        val sdkStatus = HealthConnectManager.getSdkStatus(this)

        when (sdkStatus) {
            HealthConnectClient.SDK_UNAVAILABLE -> {
                showHealthConnectUnavailableState()
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                showHealthConnectInstallDialog()
            }
            HealthConnectClient.SDK_AVAILABLE -> {
                requestHealthConnectPermissions()
            }
            else -> {
                showHealthConnectUnavailableState()
            }
        }
    }

    private fun showHealthConnectUnavailableState() {
        updateStepsUI(0)
        updateChartEmpty()
        Toast.makeText(this, R.string.health_connect_unavailable, Toast.LENGTH_LONG).show()
    }

    private fun showPermissionDeniedState() {
        updateStepsUI(0)
        updateChartEmpty()
        Toast.makeText(this, R.string.health_connect_permission, Toast.LENGTH_LONG).show()
    }

    private fun updateDailyChart(data: List<Pair<Float, Int>>) {
        val entries = data.map { (hour, steps) ->
            Entry(hour, steps.toFloat())
        }

        if (entries.isEmpty()) {
            updateChartEmpty()
            return
        }

        val totalSteps = viewModel.todaySteps.value ?: 0
        binding.tvChartSteps.text = totalSteps.toString()
        updateDateRangeText()

        val dataSet = LineDataSet(entries, "Steps").apply {
            color = ContextCompat.getColor(this@MainActivity, R.color.blue_graph)
            lineWidth = 8.8f
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER  // Smooth snake-like curve (Figma)
            cubicIntensity = 0.15f  // Gentle smoothing without overshooting
            setDrawFilled(false)
        }

        binding.lineChart.apply {
            // Hour-only labels matching Figma: 00, 3, 10, 17, 24
            xAxis.valueFormatter = object : ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    val h = value.toInt()
                    return if (h == 0) "00" else h.toString()
                }
            }
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 24f
            xAxis.labelCount = 5
            xAxis.setAvoidFirstLastClipping(true)
            this.data = LineData(dataSet)
            animateX(500)
            invalidate()
        }
    }

    private fun updateChartEmpty() {
        binding.lineChart.clear()
        binding.lineChart.setNoDataText(getString(R.string.no_step_data))
        binding.lineChart.invalidate()
    }

    private fun showHealthConnectInstallDialog() {
        AlertDialog.Builder(this)
            .setTitle(R.string.health_connect_required)
            .setMessage(R.string.health_connect_install_message)
            .setPositiveButton(R.string.install) { _, _ ->
                try {
                    startActivity(HealthConnectManager.createInstallIntent())
                } catch (e: Exception) {
                    Toast.makeText(this, R.string.play_store_error, Toast.LENGTH_SHORT).show()
                    showHealthConnectUnavailableState()
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ ->
                showHealthConnectUnavailableState()
            }
            .setCancelable(false)
            .show()
    }

    private fun requestHealthConnectPermissions() {
        val app = application as StepScapeApplication
        lifecycleScope.launch {
            try {
                val hasPermissions = app.healthConnectManager.hasAllPermissions()
                if (hasPermissions) {
                    loadHealthConnectData()
                } else {
                    healthConnectPermissionLauncher.launch(app.healthConnectManager.permissions)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showHealthConnectUnavailableState()
            }
        }
    }

    private fun loadHealthConnectData() {
        viewModel.loadTodaySteps()
        // Daily chart data is loaded reactively via todaySteps observer
    }
}