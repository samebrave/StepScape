package com.sametyigit.stepscape.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [StepLog::class], version = 3, exportSchema = false)
abstract class StepScapeDatabase : RoomDatabase() {

    abstract fun stepLogDao(): StepLogDao

    companion object {
        @Volatile
        private var INSTANCE: StepScapeDatabase? = null

        fun getDatabase(context: Context): StepScapeDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    StepScapeDatabase::class.java,
                    "stepscape_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}