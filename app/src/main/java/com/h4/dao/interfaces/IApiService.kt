package com.h4.dao.interfaces

import com.h4.dao.Delivery
import com.h4.dao.PendingPackage
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface IApiService {
    @GET("/getDeliveries")
    fun getDeliveries(): Call<List<Delivery>>

    @GET("/getPendingPackages/{shopName}")
    fun getPendingPackages(@Path("shopName") shopName: String): Call<List<PendingPackage>>

    @POST("/registerPackages")
    fun registerPackages(
        @Body body: List<Int>
    ): Call<Boolean>
}