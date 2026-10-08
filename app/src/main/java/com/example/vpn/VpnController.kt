package com.example.vpn

import android.content.Context
import android.content.Intent
import com.example.model.VpnServer
import com.example.model.VpnStatistics
import com.example.model.VpnStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * مدير حالة ونشاط الـ VPN في التطبيق
 * Singleton controller managing state and actions between the UI and FreeVpnService.
 */
object VpnController {

    private val _vpnStatus = MutableStateFlow<VpnStatus>(VpnStatus.Disconnected)
    val vpnStatus: StateFlow<VpnStatus> = _vpnStatus.asStateFlow()

    private val _statistics = MutableStateFlow(VpnStatistics())
    val statistics: StateFlow<VpnStatistics> = _statistics.asStateFlow()

    fun updateStatus(status: VpnStatus) {
        _vpnStatus.value = status
        if (status is VpnStatus.Disconnected) {
            _statistics.value = VpnStatistics()
        }
    }

    fun updateStatistics(stats: VpnStatistics) {
        _statistics.value = stats
    }

    fun connect(context: Context, server: VpnServer) {
        _vpnStatus.value = VpnStatus.Connecting(server, "Initializing secure tunnel…")
        val intent = Intent(context, FreeVpnService::class.java).apply {
            action = FreeVpnService.ACTION_CONNECT
            putExtra(FreeVpnService.EXTRA_SERVER_IP, server.ip)
            putExtra(FreeVpnService.EXTRA_SERVER_HOST, server.hostName)
            putExtra(FreeVpnService.EXTRA_SERVER_COUNTRY, server.countryLong)
            putExtra(FreeVpnService.EXTRA_SERVER_CONFIG, server.openVpnConfigText)
            putExtra(FreeVpnService.EXTRA_SERVER_PING, server.effectivePing)
            putExtra(FreeVpnService.EXTRA_SERVER_SPEED, server.speedBps)
        }
        context.startService(intent)
    }

    fun disconnect(context: Context) {
        _vpnStatus.value = VpnStatus.Disconnecting
        val intent = Intent(context, FreeVpnService::class.java).apply {
            action = FreeVpnService.ACTION_DISCONNECT
        }
        context.startService(intent)
    }
}
