package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * عميل الشبكة للتواصل مع خوادم VPN Gate
 * Network client for fetching the official VPN Gate public CSV server list.
 */
class VpnGateApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {

    private val primaryUrl = "https://www.vpngate.net/api/iphone/"
    private val backupUrl = "http://www.vpngate.net/api/iphone/"

    suspend fun fetchServersCsv(): Result<String> = withContext(Dispatchers.IO) {
        // محاولة الاتصال بالرابط الأساسي
        val primaryResult = fetchUrl(primaryUrl)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }

        // محاولة الاتصال بالرابط الاحتياطي
        val backupResult = fetchUrl(backupUrl)
        if (backupResult.isSuccess) {
            return@withContext backupResult
        }

        Result.failure(primaryResult.exceptionOrNull() ?: Exception("Failed to fetch VPN Gate servers"))
    }

    private fun fetchUrl(url: String): Result<String> {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "FreeVpnAndroid/1.0 (Linux; Android)")
                .header("Accept", "text/plain, text/csv")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response body from VPN Gate"))
                }
            } else {
                Result.failure(Exception("HTTP error ${response.code}: ${response.message}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
