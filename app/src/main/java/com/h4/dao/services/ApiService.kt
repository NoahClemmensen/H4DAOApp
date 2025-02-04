package com.h4.dao.services

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


class ApiService(private val baseUrl: String) {
    private val webhookService: IWebhookService by lazy {
        Retrofit.Builder()
            .baseUrl("https://webhook.site/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IWebhookService::class.java)
    }

    private val apiService: IApiService by lazy {
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IApiService::class.java)
    }

    private fun <T> enqueueCall(
        call: Call<T>,
        continuation: CancellableContinuation<T?>
    ) {
        call.enqueue(object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) {
                    continuation.resume(response.body())
                } else {
                    return continuation.resumeWithException(Exception("Error: ${response.errorBody()?.string()}"))
                }
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                continuation.resumeWithException(t)
            }
        })
    }

    suspend fun makeWebhookGetCall() = suspendCancellableCoroutine { continuation ->
        val call: Call<ResponseBody> = webhookService.get()

        continuation.invokeOnCancellation {
            call.cancel()
        }

        enqueueCall(call, continuation)
    }

    suspend fun makeWebhookPostCall(body: MyDataClass) = suspendCancellableCoroutine { continuation ->
        val call: Call<ResponseBody> = webhookService.post(body)

        continuation.invokeOnCancellation {
            call.cancel()
        }

        enqueueCall(call, continuation)
    }

    suspend fun makeApiCall() = suspendCancellableCoroutine { continuation ->
        val call: Call<ResponseBody> = apiService.hello()

        continuation.invokeOnCancellation {
            call.cancel()
        }

        enqueueCall(call, continuation)
    }
}

data class MyDataClass(
    val key: String
)

interface IWebhookService {
    @GET("ed6c80fd-283b-49d9-9e60-b9fe0d661ac4")
    fun get(): Call<ResponseBody>

    @POST("ed6c80fd-283b-49d9-9e60-b9fe0d661ac4")
    fun post(@Body body: MyDataClass): Call<ResponseBody>
}

interface IApiService {
    @GET("/hello")
    fun hello(): Call<ResponseBody>
}