package com.project.farmingapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.project.farmingapp.model.FieldSurveyRepository
import com.project.farmingapp.model.data.FieldSurvey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

class FieldSurveyViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FieldSurveyRepository(application)

    val allSurveys: LiveData<List<FieldSurvey>> = repo.allSurveys
    val unsyncedCount: LiveData<Int> = repo.unsyncedCount

    private val _saveResult = MutableLiveData<SaveEvent>()
    val saveResult: LiveData<SaveEvent> = _saveResult

    private val _syncResult = MutableLiveData<SyncEvent>()
    val syncResult: LiveData<SyncEvent> = _syncResult

    fun saveSurvey(survey: FieldSurvey) {
        val withRemoteId =
            if (survey.remoteId.isBlank()) survey.copy(remoteId = UUID.randomUUID().toString()) else survey
        viewModelScope.launch(Dispatchers.IO) {
            val id = repo.saveSurvey(withRemoteId)
            _saveResult.postValue(SaveEvent.Saved(id.toInt()))
        }
    }

    fun updateSurvey(survey: FieldSurvey) {
        viewModelScope.launch(Dispatchers.IO) { repo.updateSurvey(survey) }
    }

    fun deleteSurvey(survey: FieldSurvey) {
        viewModelScope.launch(Dispatchers.IO) { repo.deleteSurvey(survey) }
    }

    fun syncAll() {
        _syncResult.value = SyncEvent.Syncing
        viewModelScope.launch(Dispatchers.IO) {
            val result = repo.syncAll(getApplication())
            _syncResult.postValue(
                when (result) {
                    is FieldSurveyRepository.SyncResult.Success -> SyncEvent.Success(result.count)
                    FieldSurveyRepository.SyncResult.NoInternet -> SyncEvent.NoInternet
                    FieldSurveyRepository.SyncResult.NothingToSync -> SyncEvent.NothingToSync
                    is FieldSurveyRepository.SyncResult.Error -> SyncEvent.Error(result.message)
                }
            )
        }
    }

    sealed class SaveEvent {
        data class Saved(val id: Int) : SaveEvent()
    }

    sealed class SyncEvent {
        object Syncing : SyncEvent()
        data class Success(val count: Int) : SyncEvent()
        object NoInternet : SyncEvent()
        object NothingToSync : SyncEvent()
        data class Error(val message: String) : SyncEvent()
    }
}
