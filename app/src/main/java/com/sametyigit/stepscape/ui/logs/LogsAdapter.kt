package com.sametyigit.stepscape.ui.logs

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.sametyigit.stepscape.data.local.StepLog
import com.sametyigit.stepscape.databinding.ItemLogBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogsAdapter(
    private val userName: String = "Aurora"
) : ListAdapter<StepLog, LogsAdapter.LogViewHolder>(StepLogDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogViewHolder {
        val binding = ItemLogBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LogViewHolder, position: Int) {
        holder.bind(getItem(position), userName)
    }

    class LogViewHolder(
        private val binding: ItemLogBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault())

        fun bind(stepLog: StepLog, userName: String) {
            // Format: StepLog: 2024-01-26T10:00:00Z - User 'Aurora' took 50 steps. Data synced to Firebase.
            val timestamp = dateFormat.format(Date(stepLog.timestamp))
            val syncStatus = if (stepLog.syncedToFirebase) "Data synced to Firebase." else "Data not synced."
            binding.tvLogEntry.text = "StepLog: $timestamp - User '$userName' took ${stepLog.steps} steps. $syncStatus"
        }
    }

    class StepLogDiffCallback : DiffUtil.ItemCallback<StepLog>() {
        override fun areItemsTheSame(oldItem: StepLog, newItem: StepLog): Boolean {
            return oldItem.timestamp == newItem.timestamp
        }

        override fun areContentsTheSame(oldItem: StepLog, newItem: StepLog): Boolean {
            return oldItem == newItem
        }
    }
}