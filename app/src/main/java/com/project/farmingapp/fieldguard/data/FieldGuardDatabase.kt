package com.project.farmingapp.fieldguard.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [FieldGuardIncident::class, FieldGuardQueuedAlert::class],
    version = 1,
    exportSchema = false
)
abstract class FieldGuardDatabase : RoomDatabase() {
    abstract fun dao(): FieldGuardDao

    companion object {
        @Volatile
        private var INSTANCE: FieldGuardDatabase? = null

        fun getInstance(context: Context): FieldGuardDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FieldGuardDatabase::class.java,
                    "fieldguard_db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
