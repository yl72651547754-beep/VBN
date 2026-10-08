package com.example.model

/**
 * حالات اتصال الـ VPN
 * Connection state machine for the VPN tunnel.
 */
sealed class VpnStatus {
    object Disconnected : VpnStatus()
    data class Connecting(val server: VpnServer, val message: String = "") : VpnStatus()
    data class Connected(val server: VpnServer, val connectedAtMillis: Long = System.currentTimeMillis()) : VpnStatus()
    object Disconnecting : VpnStatus()
    data class Error(val message: String, val server: VpnServer? = null) : VpnStatus()

    val isConnected: Boolean
        get() = this is Connected

    val isConnecting: Boolean
        get() = this is Connecting

    val isBusy: Boolean
        get() = this is Connecting || this is Disconnecting
}

/**
 * إحصائيات الجلسة الحالية
 * Active session metrics (duration, traffic, ping).
 */
data class VpnStatistics(
    val durationSeconds: Long = 0,
    val bytesIn: Long = 0,
    val bytesOut: Long = 0,
    val currentPingMs: Long = 0
) {
    val formattedDuration: String
        get() {
            val hours = durationSeconds / 3600
            val minutes = (durationSeconds % 3600) / 60
            val seconds = durationSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedDownload: String
        get() = formatBytes(bytesIn)

    val formattedUpload: String
        get() = formatBytes(bytesOut)

    private fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1_000_000_000 -> String.format("%.2f GB", bytes / 1_000_000_000.0)
            bytes >= 1_000_000 -> String.format("%.2f MB", bytes / 1_000_000.0)
            bytes >= 1_000 -> String.format("%.1f KB", bytes / 1_000.0)
            else -> "$bytes B"
        }
    }
}
