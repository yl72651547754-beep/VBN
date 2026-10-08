package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * يقوم باختبار سرعة الاستجابة الفعلية للخوادم (Ping / Latency)
 * Real TCP handshake latency tester across network sockets.
 */
object PingTester {

    suspend fun measureLatency(ip: String, port: Int = 443, timeoutMs: Int = 2000): Long =
        withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                Socket().use { socket ->
                    val resolvedPort = if (port in 1..65535) port else 443
                    socket.connect(InetSocketAddress(ip, resolvedPort), timeoutMs)
                    System.currentTimeMillis() - startTime
                }
            } catch (e: Exception) {
                -1L // لا يمكن الوصول أو انتهت المهلة
            }
        }
}
