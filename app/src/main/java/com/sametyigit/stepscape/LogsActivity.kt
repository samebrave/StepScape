package com.sametyigit.stepscape

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.sametyigit.stepscape.databinding.ActivityLogsBinding
import com.sametyigit.stepscape.ui.logs.LogsAdapter
import com.sametyigit.stepscape.ui.logs.LogsViewModel

class LogsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLogsBinding
    private lateinit var viewModel: LogsViewModel
    private lateinit var adapter: LogsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLogsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupViewModel() {
        val app = application as StepScapeApplication
        viewModel = ViewModelProvider(
            this,
            LogsViewModel.Factory(app.repository)
        )[LogsViewModel::class.java]
    }

    private fun setupRecyclerView() {
        // Use actual Google account display name, fall back to "Aurora"
        val displayName = FirebaseAuth.getInstance().currentUser?.displayName
        val firstName = displayName?.split(" ")?.firstOrNull() ?: "Aurora"
        adapter = LogsAdapter(firstName)
        binding.rvLogs.apply {
            layoutManager = LinearLayoutManager(this@LogsActivity)
            adapter = this@LogsActivity.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
    }

    private fun observeViewModel() {
        viewModel.allLogs.observe(this) { logs ->
            adapter.submitList(logs)
            binding.tvEmptyState.visibility = if (logs.isEmpty()) View.VISIBLE else View.GONE
            binding.rvLogs.visibility = if (logs.isEmpty()) View.GONE else View.VISIBLE
        }
    }
}