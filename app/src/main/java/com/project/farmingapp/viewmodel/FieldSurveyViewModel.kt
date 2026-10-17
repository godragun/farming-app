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

    // One-shot events: the ViewModel is Activity-scoped, so a handled event must be cleared or the
    // next screen that observes it would receive it again (e.g. a new form instantly "saving").
    private val _saveResult = MutableLiveData<SaveEvent?>()
    val saveResult: LiveData<SaveEvent?> = _saveResult

    private val _syncResult = MutableLiveData<SyncEvent?>()
    val syncResult: LiveData<SyncEvent?> = _syncResult

    fun consumeSaveResult() { _saveResult.value = null }
    fun consumeSyncResult() { _syncResult.value = null }

    fun saveSurvey(survey: FieldSurvey) {
        val withRemoteId =
            if (survey.remoteId.isBlank()) survey.copy(remoteId = UUID.randomUUID().toString()) else survey
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val id = repo.saveSurvey(withRemoteId)
                _saveResult.postValue(SaveEvent.Saved(id.toInt()))
            } catch (e: Exception) {
                _saveResult.postValue(SaveEvent.Failed(e.message ?: "Could not save the survey"))
            }
        }
    }

    fun updateSurvey(survey: FieldSurvey) {
        viewModelScope.launch(Dispatchers.IO) { try { repo.updateSurvey(survey) } catch (_: Exception) { } }
    }

    fun deleteSurvey(survey: FieldSurvey) {
        viewModelScope.launch(Dispatchers.IO) { try { repo.deleteSurvey(survey) } catch (_: Exception) { } }
    }

    fun syncAll() {
        _syncResult.value = SyncEvent.Syncing
        viewModelScope.launch(Dispatchers.IO) {
            val event = try {
                when (val result = repo.syncAll(getApplication())) {
                    is FieldSurveyRepository.SyncResult.Success -> SyncEvent.Success(result.count)
                    FieldSurveyRepository.SyncResult.NoInternet -> SyncEvent.NoInternet
                    FieldSurveyRepository.SyncResult.NothingToSync -> SyncEvent.NothingToSync
                    is FieldSurveyRepository.SyncResult.Error -> SyncEvent.Error(result.message)
                }
            } catch (e: Exception) {
                SyncEvent.Error(e.message ?: "Sync failed")
            }
            _syncResult.postValue(event)
        }
    }

    sealed class SaveEvent {
        data class Saved(val id: Int) : SaveEvent()
        data class Failed(val message: String) : SaveEvent()
    }

    sealed class SyncEvent {
        object Syncing : SyncEvent()
        data class Success(val count: Int) : SyncEvent()
        object NoInternet : SyncEvent()
        object NothingToSync : SyncEvent()
        data class Error(val message: String) : SyncEvent()
    }
}
