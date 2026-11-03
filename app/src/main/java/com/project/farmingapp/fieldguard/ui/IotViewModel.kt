package com.project.farmingapp.fieldguard.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.project.farmingapp.fieldguard.data.FieldGuardRepository
import com.project.farmingapp.fieldguard.data.IotReading
import com.project.farmingapp.fieldguard.iot.DemoNode
import com.project.farmingapp.fieldguard.iot.DemoNodes
import com.project.farmingapp.fieldguard.iot.IotDemoPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class IotViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FieldGuardRepository(application)

    val latestReadings: LiveData<List<IotReading>> = repository.observeLatestIot()

    private val _busy = MutableLiveData(false)
    val busy: LiveData<Boolean> = _busy

    private val _status = MutableLiveData("")
    val status: LiveData<String> = _status

    private val _pattern = MutableLiveData(IotDemoPattern.NORMAL)
    val pattern: LiveData<IotDemoPattern> = _pattern

    fun nodes(): List<DemoNode> = DemoNodes.ALL

    fun ensureSeeded() {
        viewModelScope.launch {
            _busy.value = true
            _status.value = "Seeding simulated IoT history…"
            withContext(Dispatchers.IO) {
                repository.ensureIotHistory(forceRegenerate = false)
            }
            _pattern.value = repository.currentPattern()
            _busy.value = false
            _status.value = "Simulated nodes ready (7-day history)"
        }
    }

    fun applyPattern(pattern: IotDemoPattern) {
        viewModelScope.launch {
            _busy.value = true
            _status.value = "Applying demo pattern…"
            withContext(Dispatchers.IO) {
                repository.applyDemoPattern(pattern)
            }
            _pattern.value = pattern
            _busy.value = false
            _status.value = "Pattern applied: ${pattern.name}"
        }
    }

    fun latestFor(nodeId: String, readings: List<IotReading>?): IotReading? =
        readings?.find { it.nodeId == nodeId }
}
