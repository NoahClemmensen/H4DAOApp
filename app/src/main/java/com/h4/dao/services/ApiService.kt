package com.h4.dao.services

import android.util.Log
import com.h4.dao.Delivery
import com.h4.dao.PendingPackage
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

class ApiService() {
    private val baseUrl: String = "http://172.27.238.19:3000/"

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

    suspend fun getPendingPackages(shopName: String) = suspendCancellableCoroutine { continuation ->
        Log.d("LALA", "getPendingPackages")
        val call: Call<List<PendingPackage>> = apiService.getPendingPackages(shopName)

        continuation.invokeOnCancellation {
            call.cancel()
        }

        enqueueCall(call, continuation)
    }

    suspend fun registerPackages(packages: List<Int>) = suspendCancellableCoroutine { continuation ->
        val call: Call<Boolean> = apiService.registerPackages(packages)

        continuation.invokeOnCancellation {
            call.cancel()
        }

        enqueueCall(call, continuation)
    }

    suspend fun getDeliveries() = suspendCancellableCoroutine { continuation ->
        val call: Call<List<Delivery>> = apiService.getDeliveries()

        continuation.invokeOnCancellation {
            call.cancel()
        }

        enqueueCall(call, continuation)
    }
}