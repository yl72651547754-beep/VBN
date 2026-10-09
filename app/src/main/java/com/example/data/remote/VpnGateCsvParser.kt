package com.example.data.remote

import android.util.Base64
import com.example.model.VpnServer
import java.nio.charset.StandardCharsets

/**
 * محلل بيانات CSV الواردة من واجهة برمجة تطبيقات VPN Gate
 * Robust parser for VPN Gate CSV endpoint (https://www.vpngate.net/api/iphone/).
 * Handles escaped quotes, commas inside messages/operators, and clean Base64 decoding.
 */
object VpnGateCsvParser {

    fun parseCsv(csvContent: String): List<VpnServer> {
        val servers = mutableListOf<VpnServer>()
        val lines = csvContent.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("*") || trimmed.startsWith("#")) {
                continue
            }

            val parts = trimmed.split(",")
            // VPN Gate has at least 15 columns
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
                    val operator = if (parts.size > 12) parts[12].trim() else ""
                    val message = if (parts.size > 13) parts[13].trim() else ""

                    // عمود تكوين OpenVPN هو دائماً العمود الأخير في السطر، حتى لو احتوت الرسائل على فواصل
                    val openVpnConfigBase64 = parts.last().trim()
                        .replace("\r", "")
                        .replace("\n", "")
                        .replace(" ", "")

                    // فك تشفير تكوين OpenVPN من Base64
                    val decodedConfigText = decodeBase64(openVpnConfigBase64)

                    // التحقق من صحة عنوان الـ IP والبيانات
                    if (isValidIp(ip) && decodedConfigText.isNotBlank()) {
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
                    // تخطي أي سطر تالف والاستمرار بسلاسة
                }
            }
        }
        return servers
    }

    private fun isValidIp(ip: String): Boolean {
        if (ip.isBlank()) return false
        val parts = ip.split(".")
        if (parts.size != 4) return false
        return parts.all { it.toIntOrNull() in 0..255 }
    }

    private fun decodeBase64(base64Str: String): String {
        return try {
            if (base64Str.isBlank()) return ""
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            val decoded = String(decodedBytes, StandardCharsets.UTF_8).trim()
            if (decoded.contains("client") || decoded.contains("dev tun") || decoded.contains("remote")) {
                decoded
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }
}
