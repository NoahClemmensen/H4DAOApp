package com.h4.dao.interfaces

import com.h4.dao.PendingPackage
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface IApiService {
    @GET("/hello")
    fun hello(): Call<ResponseBody>

    @GET("/getPendingPackages")
    fun getPendingPackages(): Call<List<PendingPackage>>

    @POST("/registerPackages")
    fun registerPackages(
        @Body body: List<Int>
    ): Call<Boolean>
}