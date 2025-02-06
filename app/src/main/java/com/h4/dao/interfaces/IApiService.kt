package com.h4.dao.interfaces

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.GET

interface IApiService {
    @GET("/hello")
    fun hello(): Call<ResponseBody>
}