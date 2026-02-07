package com.sametyigit.stepscape

import android.app.Application
import com.sametyigit.stepscape.data.health.HealthConnectManager
import com.sametyigit.stepscape.data.local.StepScapeDatabase
import com.sametyigit.stepscape.data.remote.FirebaseService
import com.sametyigit.stepscape.data.repository.StepRepository

class StepScapeApplication : Application() {

    val database by lazy { StepScapeDatabase.getDatabase(this) }
    val repository by lazy {
        StepRepository(
            database.stepLogDao(),
            FirebaseService()
        )
    }
    val healthConnectManager by lazy { HealthConnectManager(this) }
}