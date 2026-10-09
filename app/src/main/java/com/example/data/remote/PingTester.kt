package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * يقوم باختبار سرعة الاستجابة الفعلية للخوادم (Ping / Latency)
 * Real socket latency tester trying OpenVPN and SoftEther management ports (443, 80, 992).
 */
object PingTester {

    suspend fun measureLatency(ip: String, port: Int = 443, timeoutMs: Int = 1800): Long =
        withContext(Dispatchers.IO) {
            val candidatePorts = mutableListOf<Int>()
            if (port in 1..65535) candidatePorts.add(port)
            candidatePorts.add(443) // Standard SSL / SoftEther OpenVPN port
            candidatePorts.add(80)  // Standard HTTP port
            candidatePorts.add(992) // SoftEther alternate SSL port

            for (p in candidatePorts.distinct()) {
                val startTime = System.currentTimeMillis()
                try {
                    Socket().use { socket ->
                        socket.tcpNoDelay = true
                        socket.connect(InetSocketAddress(ip, p), timeoutMs)
                        val latency = System.currentTimeMillis() - startTime
                        if (latency in 1..5000) {
                            return@withContext latency
                        }
                    }
                } catch (ignored: Exception) {
                    // تجربة المنفذ البديل
                }
            }
            -1L // لا يمكن الوصول أو الخادم غير متصل
        }
}
