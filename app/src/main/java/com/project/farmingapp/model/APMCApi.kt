package com.project.farmingapp.model

import com.project.farmingapp.BuildConfig
import com.project.farmingapp.model.data.APMCMain
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

const val BASE_URL2 = "https://api.data.gov.in/"

interface apmcInterface {
    @GET("resource/9ef84268-d588-465a-a308-a864a43d0070")
    fun getapmc(
        @Query("api-key") apiKey: String,
        @Query("format") format: String = "json",
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int
    ): Call<APMCMain>

    @GET("resource/9ef84268-d588-465a-a308-a864a43d0070")
    fun getSomeData(
        @Query("api-key") apiKey: String,
        @Query("format") format: String = "json",
        @Query("offset") offset: Int = 0,
        @Query("limit") limit: Int = 7000,
        @Query("filters[district]") filter: String
    ): Call<APMCMain>
}

object APMCApi {
    val apmcInstances: apmcInterface

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL2)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apmcInstances = retrofit.create(apmcInterface::class.java)
    }

    fun hasApiKey(): Boolean = BuildConfig.APMC_API_KEY.isNotBlank()

    fun getapmc(limit: Int): Call<APMCMain> {
        return apmcInstances.getapmc(apiKey = BuildConfig.APMC_API_KEY, limit = limit)
    }

    fun getSomeData(filter: String): Call<APMCMain> {
        return apmcInstances.getSomeData(apiKey = BuildConfig.APMC_API_KEY, filter = filter)
    }
}
