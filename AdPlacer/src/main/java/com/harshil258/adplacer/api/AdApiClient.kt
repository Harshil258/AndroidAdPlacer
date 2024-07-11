package com.harshil258.adplacer.api

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

//import io.michaelrocks.paranoid.Obfuscate;
//@Obfuscate
class AdApiClient {

    private var retrofit: Retrofit? = null

    private val gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    private val httpClient: OkHttpClient.Builder = OkHttpClient.Builder()
        .apply {
            val logging = HttpLoggingInterceptor()
            logging.setLevel(HttpLoggingInterceptor.Level.BODY)
            addInterceptor(logging)
        }

    val client: Retrofit
        get() {
            return retrofit ?: synchronized(this) {
                retrofit ?: buildRetrofit().also { retrofit = it }
            }
        }

    private fun buildRetrofit(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL_API)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(httpClient.build())
            .build()
    }

    companion object {
        var BASE_URL_API = ""
    }
}