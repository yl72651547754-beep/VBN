package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * عميل الشبكة لجلب خوادم VPN Gate مع دعم النطاقات والمرايا البديلة
 * Network client supporting multiple official mirrors and IP endpoints.
 */
class VpnGateApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(true)
        .build()
) {

    // قائمة الروابط والمرايا الرسمية لـ VPN Gate للتغلب على الحجب في مختلف الدول
    private val mirrorUrls = listOf(
        "https://www.vpngate.net/api/iphone/",
        "http://www.vpngate.net/api/iphone/",
        "http://130.158.6.80/api/iphone/",
        "http://130.158.6.81/api/iphone/",
        "https://vpngate.net/api/iphone/"
    )

    suspend fun fetchServersCsv(): Result<String> = withContext(Dispatchers.IO) {
        var lastException: Exception? = null

        for (url in mirrorUrls) {
            val result = fetchUrl(url)
            if (result.isSuccess) {
                return@withContext result
            } else {
                lastException = result.exceptionOrNull() as? Exception
            }
        }

        Result.failure(lastException ?: Exception("All VPN Gate endpoints unreachable"))
    }

    private fun fetchUrl(url: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; FreeVpn/1.0)")
                .header("Accept", "text/plain, text/csv")
                .header("Connection", "close")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank() && (body.contains("*vpn_servers") || body.contains("HostName"))) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Invalid or empty response from $url"))
                }
            } else {
                Result.failure(Exception("HTTP error ${response.code} from $url"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
