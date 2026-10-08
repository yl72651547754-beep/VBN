package com.example.model

/**
 * يمثل بيانات خادم VPN المستخرج من VPN Gate
 * Represents a VPN server entity with metrics and OpenVPN configuration.
 */
data class VpnServer(
    val hostName: String,
    val ip: String,
    val score: Long,
    val pingMs: Long,
    val speedBps: Long,
    val countryLong: String,
    val countryShort: String,
    val numVpnSessions: Int,
    val uptimeSeconds: Long,
    val totalUsers: Long,
    val operator: String,
    val message: String,
    val openVpnConfigBase64: String,
    val openVpnConfigText: String,
    val isFavorite: Boolean = false,
    val measuredPingMs: Long? = null,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    val speedMbps: Double
        get() = (speedBps.toDouble() / 1_000_000.0)

    val formattedSpeed: String
        get() = String.format("%.1f Mbps", speedMbps)

    val effectivePing: Long
        get() = measuredPingMs ?: pingMs

    val flagEmoji: String
        get() = getFlagEmoji(countryShort)

    companion object {
        fun getFlagEmoji(countryCode: String): String {
            if (countryCode.length != 2) return "🌐"
            return try {
                val upper = countryCode.uppercase()
                val firstChar = Character.codePointAt(upper, 0) - 0x41 + 0x1F1E6
                val secondChar = Character.codePointAt(upper, 1) - 0x41 + 0x1F1E6
                String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
            } catch (e: Exception) {
                "🌐"
            }
        }
    }
}
