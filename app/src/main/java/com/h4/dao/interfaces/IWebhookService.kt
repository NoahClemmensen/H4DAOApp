package com.h4.dao.interfaces

import com.h4.dao.Package
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface IWebhookService {
    @GET("ed6c80fd-283b-49d9-9e60-b9fe0d661ac4")
    fun get(): Call<ResponseBody>

    @POST("ed6c80fd-283b-49d9-9e60-b9fe0d661ac4")
    fun post(@Body body: Package): Call<ResponseBody>
}