package com.example.vpn

/**
 * محلل إعدادات وتكوينات OpenVPN (.ovpn)
 * Parses OpenVPN configuration directives into structured parameters.
 */
data class ParsedOpenVpnConfig(
    val remoteHost: String,
    val remotePort: Int,
    val protocol: String = "udp",
    val cipher: String? = null,
    val isTcp: Boolean = false,
    val mtu: Int = 1500
)

object OpenVpnConfigParser {

    fun parse(configText: String, defaultHost: String = ""): ParsedOpenVpnConfig {
        var host = defaultHost
        var port = 1194
        var protocol = "udp"
        var cipher: String? = null
        var mtu = 1500

        for (line in configText.lines()) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#") || trimmed.startsWith(";") || trimmed.isEmpty()) {
                continue
            }

            val tokens = trimmed.split("\\s+".toRegex())
            if (tokens.isEmpty()) continue

            when (tokens[0].lowercase()) {
                "remote" -> {
                    if (tokens.size >= 2) {
                        host = tokens[1]
                    }
                    if (tokens.size >= 3) {
                        port = tokens[2].toIntOrNull() ?: port
                    }
                    if (tokens.size >= 4) {
                        protocol = tokens[3].lowercase()
                    }
                }
                "proto" -> {
                    if (tokens.size >= 2) {
                        protocol = tokens[1].lowercase()
                    }
                }
                "port" -> {
                    if (tokens.size >= 2) {
                        port = tokens[1].toIntOrNull() ?: port
                    }
                }
                "cipher" -> {
                    if (tokens.size >= 2) {
                        cipher = tokens[1]
                    }
                }
                "tun-mtu" -> {
                    if (tokens.size >= 2) {
                        mtu = tokens[1].toIntOrNull() ?: mtu
                    }
                }
            }
        }

        val isTcp = protocol.contains("tcp")
        return ParsedOpenVpnConfig(
            remoteHost = host,
            remotePort = port,
            protocol = if (isTcp) "tcp" else "udp",
            cipher = cipher,
            isTcp = isTcp,
            mtu = mtu
        )
    }
}
