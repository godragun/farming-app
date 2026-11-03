package com.project.farmingapp.fieldguard.ui

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.project.farmingapp.fieldguard.agent.AgentRunResult
import com.project.farmingapp.fieldguard.agent.AgentTraceStep
import com.project.farmingapp.fieldguard.agent.FieldGuardAgent
import com.project.farmingapp.fieldguard.agent.NodeIndicatorState
import com.project.farmingapp.fieldguard.data.FieldGuardIncident
import com.project.farmingapp.fieldguard.data.FieldGuardRepository
import com.project.farmingapp.fieldguard.llm.ConnectionTestResult
import com.project.farmingapp.fieldguard.llm.LlmClient
import com.project.farmingapp.fieldguard.scenario.DemoScenario
import com.project.farmingapp.fieldguard.scenario.DemoScenarios
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FieldGuardViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FieldGuardRepository(application)
    private val llm = LlmClient()
    private val agent = FieldGuardAgent(application, llm, repository)

    val incidents: LiveData<List<FieldGuardIncident>> = repository.observeIncidents()

    private val _traces = MutableLiveData<List<AgentTraceStep>>(emptyList())
    val traces: LiveData<List<AgentTraceStep>> = _traces

    private val _indicator = MutableLiveData(NodeIndicatorState())
    val indicator: LiveData<NodeIndicatorState> = _indicator

    private val _frameDataUrl = MutableLiveData<String?>()
    val frameDataUrl: LiveData<String?> = _frameDataUrl

    private val _running = MutableLiveData(false)
    val running: LiveData<Boolean> = _running

    private val _lastResult = MutableLiveData<AgentRunResult?>()
    val lastResult: LiveData<AgentRunResult?> = _lastResult

    private val _connectionTest = MutableLiveData<ConnectionTestResult?>()
    val connectionTest: LiveData<ConnectionTestResult?> = _connectionTest

    private val _statusMessage = MutableLiveData<String>("")
    val statusMessage: LiveData<String> = _statusMessage

    fun modelName(): String = llm.configuredModel()
    fun baseUrl(): String = llm.configuredBaseUrl()

    fun scenarios(): List<DemoScenario> = DemoScenarios.ALL

    fun testConnection() {
        viewModelScope.launch {
            _statusMessage.value = "Testing connection…"
            val result = withContext(Dispatchers.IO) { llm.testConnection() }
            _connectionTest.value = result
            _statusMessage.value = if (result.ok) {
                "Connected (${result.latencyMs} ms)"
            } else {
                "Connection failed: ${result.error}"
            }
        }
    }

    fun runScenario(scenarioId: String) {
        val scenario = DemoScenarios.byId(scenarioId) ?: return
        runAgent(scenario, cameraDataUrl = null)
    }

    fun runCamera(dataUrl: String) {
        runAgent(scenario = null, cameraDataUrl = dataUrl)
    }

    private fun runAgent(scenario: DemoScenario?, cameraDataUrl: String?) {
        if (_running.value == true) return
        viewModelScope.launch {
            _running.value = true
            _traces.value = emptyList()
            _lastResult.value = null
            _indicator.value = NodeIndicatorState()
            _statusMessage.value = "Agent running…"
            val lang = currentLanguageTag()
            val result = agent.run(
                scenario = scenario,
                languageTag = lang,
                cameraDataUrl = cameraDataUrl,
                onTrace = { step ->
                    val next = (_traces.value.orEmpty() + step)
                    _traces.postValue(next)
                },
                onIndicator = { _indicator.postValue(it) },
                onFrame = { _frameDataUrl.postValue(it) }
            )
            _lastResult.value = result
            _running.value = false
            _statusMessage.value = if (result.success) {
                "Done — ${result.status}"
            } else {
                "Failed: ${result.error}"
            }
        }
    }

    private fun currentLanguageTag(): String {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (!locales.isEmpty) {
            return locales.get(0)?.language ?: "en"
        }
        return LocaleListCompat.getAdjustedDefault().get(0)?.language ?: "en"
    }
}
