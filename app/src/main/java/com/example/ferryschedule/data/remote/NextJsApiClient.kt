package com.example.ferryschedule.data.remote

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface NextJsFerryApi {
    @GET("api/ferry/departures")
    suspend fun getDepartures(
        @Query("direction") direction: String,
        @Query("limit") limit: Int = 3
    ): NextJsFerryResponse
}

object NextJsApiClient {
    /**
     * Set this URL to your deployed Next.js web app (e.g. "https://my-ferry-app.vercel.app/")
     * or for local testing on emulator: "http://10.0.2.2:3000/"
     */
    var BASE_URL: String = "http://10.0.2.2:3000/"

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    val api: NextJsFerryApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NextJsFerryApi::class.java)
    }
}
