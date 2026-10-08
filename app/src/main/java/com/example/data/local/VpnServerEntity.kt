package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.model.VpnServer

/**
 * كيان قاعدة البيانات لتخزين خوادم الـ VPN محلياً
 * Room entity representing cached VPN server.
 */
@Entity(tableName = "vpn_servers")
data class VpnServerEntity(
    @PrimaryKey
    val ip: String,
    val hostName: String,
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
    fun toDomain(): VpnServer = VpnServer(
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
        openVpnConfigText = openVpnConfigText,
        isFavorite = isFavorite,
        measuredPingMs = measuredPingMs,
        lastUpdated = lastUpdated
    )

    companion object {
        fun fromDomain(server: VpnServer): VpnServerEntity = VpnServerEntity(
            ip = server.ip,
            hostName = server.hostName,
            score = server.score,
            pingMs = server.pingMs,
            speedBps = server.speedBps,
            countryLong = server.countryLong,
            countryShort = server.countryShort,
            numVpnSessions = server.numVpnSessions,
            uptimeSeconds = server.uptimeSeconds,
            totalUsers = server.totalUsers,
            operator = server.operator,
            message = server.message,
            openVpnConfigBase64 = server.openVpnConfigBase64,
            openVpnConfigText = server.openVpnConfigText,
            isFavorite = server.isFavorite,
            measuredPingMs = server.measuredPingMs,
            lastUpdated = server.lastUpdated
        )
    }
}
