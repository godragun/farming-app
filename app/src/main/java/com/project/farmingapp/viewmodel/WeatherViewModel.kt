package com.project.farmingapp.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.project.farmingapp.adapter.WeatherAdapter
import com.project.farmingapp.model.WeatherApi
import com.project.farmingapp.model.WeatherRepository
import com.project.farmingapp.model.data.Weather
import com.project.farmingapp.model.data.WeatherRootList
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class WeatherViewModel : ViewModel() {
    lateinit var Adapter: WeatherAdapter
    lateinit var rootData: MutableLiveData<WeatherRootList>
    var weatherListener: WeatherListener? = null

    private var message1 = MutableLiveData<WeatherRootList>()
    private var message2 = MutableLiveData<WeatherRootList>()

    private var coordinates = MutableLiveData<List<String>>(listOf("6.9271", "79.8612", "Colombo"))

    val _rootData1 = MutableLiveData<WeatherRootList>()
    val rootData2: LiveData<WeatherRootList> = _rootData1


    var newDataTrial = MutableLiveData<WeatherRootList>(fallbackWeather())
    private var isRequestInFlight = false
    private var lastRequestAtMs: Long = 0L
    private var inFlightCall: Call<WeatherRootList>? = null

    // Prevent request storms when multiple observers trigger updates back-to-back.
    private val minRefreshIntervalMs = 8_000L


    fun messageToB(msg: WeatherRootList){
        message2.value = msg

    }

    fun messageToA(msg: WeatherRootList){
        message1.value = msg
    }

    fun getMessageA(): MutableLiveData<WeatherRootList> {
        return message1
    }

    fun getMessageB(): MutableLiveData<WeatherRootList>{
        return message2
    }

    fun callWeatherRepository() {
        val response = WeatherRepository().getWeather()
//        val ss = response.value!!.list[0].dt_txt
//        Log.d("ViewModel", ss)
        weatherListener?.onSuccess(response)
    }

    fun updateCoordinates(coords: List<String>){
        coordinates.value = coords
        Log.d("WeatherView", coordinates.value.toString())
    }

    fun getCoordinates(): MutableLiveData<List<String>> {
        return coordinates
    }

    fun updateNewData(){
        val now = System.currentTimeMillis()
        if (now - lastRequestAtMs < minRefreshIntervalMs) return
        if (isRequestInFlight) {
            // If the request is already running but UI has never received any data, emit fallback
            if (newDataTrial.value == null || newDataTrial.value?.list.isNullOrEmpty()) {
                newDataTrial.postValue(fallbackWeather())
            }
            return
        }
        isRequestInFlight = true
        lastRequestAtMs = now

        inFlightCall?.cancel()
        val response: Call<WeatherRootList> =
            WeatherApi.weatherInstances.getWeather("${coordinates.value?.get(0)}", "${coordinates.value?.get(1)}")
        inFlightCall = response

        Log.d("New Weather Updated", "Yes")
        Log.d("Weather Lat", "${coordinates.value?.get(0)}")
        Log.d("Weather Long", "${coordinates.value?.get(1)}")
        Log.d("Weather City", "${coordinates.value?.get(2)}")

        response.enqueue(object : Callback<WeatherRootList> {
            override fun onFailure(call: Call<WeatherRootList>, t: Throwable) {
                Log.e("WeatherRepository", "Weather request failed", t)
                isRequestInFlight = false
                if (inFlightCall === call) inFlightCall = null
                if (newDataTrial.value == null || newDataTrial.value?.list.isNullOrEmpty()) {
                    newDataTrial.postValue(fallbackWeather())
                }
            }

            override fun onResponse(
                call: Call<WeatherRootList>,
                response: Response<WeatherRootList>
            ) {
                isRequestInFlight = false
                if (inFlightCall === call) inFlightCall = null
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body != null && body.list.isNotEmpty()) {
                        newDataTrial.postValue(body)
                    } else {
                        newDataTrial.postValue(fallbackWeather())
                    }
                } else {
                    Log.w(
                        "WeatherRepository",
                        "Weather HTTP ${response.code()} ${response.message()}"
                    )
                    if (newDataTrial.value == null || newDataTrial.value?.list.isNullOrEmpty()) {
                        newDataTrial.postValue(fallbackWeather())
                    }
                }
            }
        })
    }

    private fun fallbackWeather(): WeatherRootList {
        val fallbackEntries = mutableListOf<com.project.farmingapp.model.data.WeatherList>()
        repeat(40) { index ->
            val day = (index / 8) + 1
            val hour = (index % 8) * 3
            val dayPart = day.toString().padStart(2, '0')
            val hourPart = hour.toString().padStart(2, '0')
            fallbackEntries.add(
                com.project.farmingapp.model.data.WeatherList(
                    main = com.project.farmingapp.model.data.WeatherMain(
                        temp = 300f,
                        humidity = 70,
                        temp_min = 298f,
                        temp_max = 302f
                    ),
                    weather = listOf(
                        com.project.farmingapp.model.data.Weather(
                            main = "Cloudy",
                            description = "Demo weather",
                            icon = "03d"
                        )
                    ),
                    dt_txt = "2026-04-${dayPart} ${hourPart}:00:00",
                    wind = com.project.farmingapp.model.data.WeatherWind(speed = 12f)
                )
            )
        }
        return WeatherRootList(fallbackEntries)
    }
}