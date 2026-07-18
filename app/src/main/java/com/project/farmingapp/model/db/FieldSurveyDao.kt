package com.project.farmingapp.model.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.project.farmingapp.model.data.FieldSurvey

@Dao
interface FieldSurveyDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(survey: FieldSurvey): Long

    @Update
    suspend fun update(survey: FieldSurvey)

    @Delete
    suspend fun delete(survey: FieldSurvey)

    @Query("SELECT * FROM field_surveys ORDER BY savedAt DESC")
    fun getAllSurveys(): LiveData<List<FieldSurvey>>

    @Query("SELECT * FROM field_surveys WHERE id = :id")
    suspend fun getSurveyById(id: Int): FieldSurvey?

    @Query("SELECT * FROM field_surveys WHERE isSynced = 0")
    suspend fun getUnsyncedSurveys(): List<FieldSurvey>

    @Query("UPDATE field_surveys SET isSynced = 1 WHERE id = :id")
    suspend fun markAsSynced(id: Int)

    @Query("SELECT COUNT(*) FROM field_surveys WHERE isSynced = 0")
    fun getUnsyncedCount(): LiveData<Int>
}
