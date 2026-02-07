package com.sametyigit.stepscape.ui.logs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.sametyigit.stepscape.data.local.StepLog
import com.sametyigit.stepscape.data.repository.StepRepository
import kotlinx.coroutines.launch

class LogsViewModel(
    private val repository: StepRepository
) : ViewModel() {

    private val userId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    val allLogs: LiveData<List<StepLog>> = repository.getAllStepLogs(userId).asLiveData()

    private val _syncStatus = MutableLiveData<String>()
    val syncStatus: LiveData<String> = _syncStatus

    fun syncAll() {
        viewModelScope.launch {
            try {
                val count = repository.syncUnsyncedLogsToFirebase(userId)
                _syncStatus.value = if (count > 0) "Synced $count records" else "All records synced"
            } catch (e: Exception) {
                _syncStatus.value = "Sync failed"
            }
        }
    }

    class Factory(
        private val repository: StepRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(LogsViewModel::class.java)) {
                return LogsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}