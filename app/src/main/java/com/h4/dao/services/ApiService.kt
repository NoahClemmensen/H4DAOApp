package com.h4.dao.services

import com.h4.dao.Package
import com.h4.dao.interfaces.IApiService
import com.h4.dao.interfaces.IWebhookService
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
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

    suspend fun makeWebhookPostCall(body: Package) = suspendCancellableCoroutine { continuation ->
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