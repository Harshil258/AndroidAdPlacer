package com.harshil258.adplacer.interfaces

import com.harshil258.adplacer.models.ApiResponse
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiInterface {

    @Headers("Content-Type: application/json")
    @POST("api.php")
    fun getAll(@Body body: RequestBody): Call<ApiResponse?>?
}
