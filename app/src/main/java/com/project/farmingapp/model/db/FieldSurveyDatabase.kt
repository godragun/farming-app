package com.project.farmingapp.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.project.farmingapp.model.data.FieldSurvey

@Database(entities = [FieldSurvey::class], version = 3, exportSchema = false)
abstract class FieldSurveyDatabase : RoomDatabase() {

    abstract fun fieldSurveyDao(): FieldSurveyDao

    companion object {
        @Volatile
        private var INSTANCE: FieldSurveyDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE field_surveys ADD COLUMN imageUrl TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "ALTER TABLE field_surveys ADD COLUMN audioUrl TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE field_surveys ADD COLUMN remoteId TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        fun getInstance(context: Context): FieldSurveyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FieldSurveyDatabase::class.java,
                    "field_survey_db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
