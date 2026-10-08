package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.remote.VpnGateCsvParser
import com.example.model.VpnServer
import com.example.vpn.OpenVpnConfigParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Free VPN", appName)
    }

    @Test
    fun `test vpn gate csv parser with valid entry`() {
        val sampleCsv = """
            *vpn_servers
            #HostName,IP,Score,Ping,Speed,CountryLong,CountryShort,NumVpnSessions,Uptime,TotalUsers,TotalTraffic,LogType,Operator,Message,OpenVPN_ConfigData_Base64
            public-vpn-1.opengw.net,219.100.37.240,1500000,18,65000000,Japan,JP,45,86400,12000,9999999,2weeks,Volunteer,Welcome,Y2xpZW50CmRldiB0dW4KcHJvdG8gdWRwCnJlbW90ZSAyMTkuMTAwLjM3LjI0MCAxNDg3
            *
        """.trimIndent()

        val servers = VpnGateCsvParser.parseCsv(sampleCsv)
        assertEquals(1, servers.size)
        val s = servers.first()
        assertEquals("219.100.37.240", s.ip)
        assertEquals("Japan", s.countryLong)
        assertEquals("JP", s.countryShort)
        assertEquals("🇯🇵", s.flagEmoji)
        assertEquals(18L, s.pingMs)
        assertTrue(s.openVpnConfigText.contains("remote 219.100.37.240 1487"))
    }

    @Test
    fun `test openvpn config parser`() {
        val config = """
            client
            dev tun
            proto udp
            remote 219.100.37.240 1487
            cipher AES-128-CBC
        """.trimIndent()

        val parsed = OpenVpnConfigParser.parse(config)
        assertEquals("219.100.37.240", parsed.remoteHost)
        assertEquals(1487, parsed.remotePort)
        assertEquals("udp", parsed.protocol)
    }
}
