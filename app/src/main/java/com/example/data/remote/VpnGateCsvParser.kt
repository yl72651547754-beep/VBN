package com.example.data.remote

import android.util.Base64
import com.example.model.VpnServer
import java.nio.charset.StandardCharsets

/**
 * محلل بيانات CSV الواردة من واجهة برمجة تطبيقات VPN Gate
 * Robust parser for VPN Gate CSV endpoint (https://www.vpngate.net/api/iphone/).
 */
object VpnGateCsvParser {

    /**
     * Parsing columns:
     * 0: HostName
     * 1: IP
     * 2: Score
     * 3: Ping
     * 4: Speed (bps)
     * 5: CountryLong
     * 6: CountryShort
     * 7: NumVpnSessions
     * 8: Uptime
     * 9: TotalUsers
     * 10: TotalTraffic
     * 11: LogType
     * 12: Operator
     * 13: Message
     * 14: OpenVPN_ConfigData_Base64
     */
    fun parseCsv(csvContent: String): List<VpnServer> {
        val servers = mutableListOf<VpnServer>()
        val lines = csvContent.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("*") || trimmed.startsWith("#")) {
                continue
            }

            val parts = trimmed.split(",")
            if (parts.size >= 15) {
                try {
                    val hostName = parts[0].trim()
                    val ip = parts[1].trim()
                    val score = parts[2].trim().toLongOrNull() ?: 0L
                    val pingMs = parts[3].trim().toLongOrNull() ?: 999L
                    val speedBps = parts[4].trim().toLongOrNull() ?: 0L
                    val countryLong = parts[5].trim()
                    val countryShort = parts[6].trim()
                    val numVpnSessions = parts[7].trim().toIntOrNull() ?: 0
                    val uptimeSeconds = parts[8].trim().toLongOrNull() ?: 0L
                    val totalUsers = parts[9].trim().toLongOrNull() ?: 0L
                    val operator = parts[12].trim()
                    val message = parts[13].trim()
                    val openVpnConfigBase64 = parts[14].trim()

                    // فك تشفير تكوين OpenVPN من Base64
                    val decodedConfigText = decodeBase64(openVpnConfigBase64)

                    // التحقق من صحة عنوان الـ IP والبيانات
                    if (ip.isNotBlank() && decodedConfigText.isNotBlank()) {
                        servers.add(
                            VpnServer(
                                hostName = hostName,
                                ip = ip,
                                score = score,
                                pingMs = pingMs,
                                speedBps = speedBps,
                                countryLong = countryLong,
                                countryShort = countryShort,
                                numVpnSessions = numVpnSessions,
                                uptimeSeconds = uptimeSeconds,
                                totalUsers = totalUsers,
                                operator = operator,
                                message = message,
                                openVpnConfigBase64 = openVpnConfigBase64,
                                openVpnConfigText = decodedConfigText
                            )
                        )
                    }
                } catch (e: Exception) {
                    // تجاهل السطور المعطوبة واستمرار التحليل
                }
            }
        }
        return servers
    }

    private fun decodeBase64(base64Str: String): String {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            String(decodedBytes, StandardCharsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }
}
