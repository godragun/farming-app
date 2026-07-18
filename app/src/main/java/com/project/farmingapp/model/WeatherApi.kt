package com.project.farmingapp.model

import com.project.farmingapp.model.data.WeatherRootList
import okhttp3.OkHttpClient
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit


const val BASE_URL ="https://api.openweathermap.org/"
const val API_KEY ="e9256fd3636e53b70e7c4495df486bb2"
interface weatherInterface {
    @GET("data/2.5/forecast?appid=$API_KEY")
    fun getWeather(@Query("lat")lat:String, @Query("lon")lon:String): Call<WeatherRootList>
}

object WeatherApi {
    val weatherInstances:weatherInterface
    init {
        val http = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        weatherInstances =retrofit.create(weatherInterface::class.java)
    }
}