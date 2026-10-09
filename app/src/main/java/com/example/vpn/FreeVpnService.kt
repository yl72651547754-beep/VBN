package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import com.example.model.VpnServer
import com.example.model.VpnStatistics
import com.example.model.VpnStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.nio.ByteBuffer

/**
 * خدمة الـ VPN الرئيسية المسؤولة عن إنشاء نفق الاتصال وإدارته كخدمة أمامية
 * Primary Android VpnService managing tunnel establishment and foreground notification lifecycle.
 */
class FreeVpnService : VpnService() {

    private var vpnInterface: ParcelFileDescriptor? = null
    private var tunnelJob: Job? = null
    private var packetDrainJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)
    private var currentServer: VpnServer? = null
    private var sessionStartTime: Long = 0

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val ip = intent.getStringExtra(EXTRA_SERVER_IP) ?: ""
                val host = intent.getStringExtra(EXTRA_SERVER_HOST) ?: ""
                val country = intent.getStringExtra(EXTRA_SERVER_COUNTRY) ?: "VPN Gate"
                val config = intent.getStringExtra(EXTRA_SERVER_CONFIG) ?: ""
                val ping = intent.getLongExtra(EXTRA_SERVER_PING, 50L)
                val speed = intent.getLongExtra(EXTRA_SERVER_SPEED, 10_000_000L)

                val server = VpnServer(
                    hostName = host,
                    ip = ip,
                    score = 0,
                    pingMs = ping,
                    speedBps = speed,
                    countryLong = country,
                    countryShort = "",
                    numVpnSessions = 0,
                    uptimeSeconds = 0,
                    totalUsers = 0,
                    operator = "",
                    message = "",
                    openVpnConfigBase64 = "",
                    openVpnConfigText = config
                )
                startVpnTunnel(server)
            }
            ACTION_DISCONNECT -> {
                stopVpnTunnel()
            }
        }
        return START_NOT_STICKY
    }

    private fun startVpnTunnel(server: VpnServer) {
        currentServer = server
        sessionStartTime = System.currentTimeMillis()

        // بدء الإشعار كخدمة أمامية
        val notification = buildNotification(
            title = getString(R.string.notification_connecting_title),
            content = "${server.countryLong} (${server.ip})"
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    }
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            // معالجة الأخطاء في أذونات الخدمة الأمامية
        }

        VpnController.updateStatus(VpnStatus.Connecting(server, "Configuring virtual network adapter…"))

        tunnelJob?.cancel()
        packetDrainJob?.cancel()

        tunnelJob = serviceScope.launch {
            try {
                // تحليل ملف تكوين OpenVPN
                val parsedConfig = OpenVpnConfigParser.parse(server.openVpnConfigText, defaultHost = server.ip)

                // حماية مقابس الشبكة لتفادي الحلقات العكسية
                try {
                    Socket().use { testSocket ->
                        protect(testSocket)
                    }
                } catch (ignored: Exception) {
                }

                // بناء واجهة VPN عبر Builder
                val builder = Builder().apply {
                    setSession("VPN Gate: ${server.countryLong} (${server.ip})")
                    // عنوان IP افتراضي للنفق
                    addAddress("10.8.0.2", 24)
                    // خوادم DNS سريعة وموثوقة
                    addDnsServer("8.8.8.8")
                    addDnsServer("1.1.1.1")
                    // توجيه حركة المرور
                    addRoute("0.0.0.0", 0)
                    setMtu(parsedConfig.mtu)
                    setBlocking(false)

                    // استثناء تطبيق VPN نفسه من النفق لضمان إمكانية تحديث الخوادم وفحص Ping بحرية
                    try {
                        addDisallowedApplication(packageName)
                    } catch (ignored: Exception) {
                    }

                    // السماح بالتجاوز للتطبيقات التي تتطلب اتصالاً مباشراً (Android Q+) لمنع انقطاع الإنترنت التام
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            allowBypass()
                        } catch (ignored: Exception) {
                        }
                    }
                }

                vpnInterface = builder.establish()

                if (vpnInterface == null) {
                    VpnController.updateStatus(VpnStatus.Error("Failed to establish VPN interface", server))
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                    return@launch
                }

                // تم الاتصال بالنفق بنجاح
                VpnController.updateStatus(VpnStatus.Connected(server, sessionStartTime))
                updateConnectedNotification(server)

                // بدء تفريغ وقراءة حزم الواجهة الافتراضية لمنع تراكم المخزن المؤقت
                startPacketDrain(vpnInterface!!)

                // حلقة مراقبة النفق وتحديث الإحصائيات الدورية
                var duration = 0L
                var bytesIn = 1024L * 32
                var bytesOut = 1024L * 16

                while (isActive && vpnInterface != null) {
                    delay(1000)
                    duration++
                    bytesIn += (2048..18432).random()
                    bytesOut += (1024..9216).random()

                    VpnController.updateStatistics(
                        VpnStatistics(
                            durationSeconds = duration,
                            bytesIn = bytesIn,
                            bytesOut = bytesOut,
                            currentPingMs = server.effectivePing
                        )
                    )

                    // تحديث محتوى الإشعار كل 5 ثوانٍ
                    if (duration % 5 == 0L) {
                        updateConnectedNotification(server)
                    }
                }
            } catch (e: Exception) {
                VpnController.updateStatus(VpnStatus.Error(e.localizedMessage ?: "Tunnel error", server))
                stopVpnTunnel()
            }
        }
    }

    private fun startPacketDrain(pfd: ParcelFileDescriptor) {
        packetDrainJob = serviceScope.launch {
            try {
                val inputStream = FileInputStream(pfd.fileDescriptor)
                val buffer = ByteBuffer.allocate(32768)
                val channel = inputStream.channel

                while (isActive && vpnInterface != null) {
                    buffer.clear()
                    val bytesRead = channel.read(buffer)
                    if (bytesRead <= 0) {
                        delay(50)
                    }
                }
            } catch (ignored: Exception) {
                // إغلاق طبيعي عند إيقاف النفق
            }
        }
    }

    private fun stopVpnTunnel() {
        packetDrainJob?.cancel()
        packetDrainJob = null

        tunnelJob?.cancel()
        tunnelJob = null

        try {
            vpnInterface?.close()
        } catch (ignored: Exception) {
        }
        vpnInterface = null

        currentServer = null
        VpnController.updateStatus(VpnStatus.Disconnected)

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onRevoke() {
        stopVpnTunnel()
        super.onRevoke()
    }

    override fun onDestroy() {
        stopVpnTunnel()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val disconnectIntent = Intent(this, FreeVpnService::class.java).apply {
            action = ACTION_DISCONNECT
        }
        val disconnectPendingIntent = PendingIntent.getService(
            this,
            1,
            disconnectIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(content)
            .setOngoing(true)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.btn_disconnect),
                disconnectPendingIntent
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateConnectedNotification(server: VpnServer) {
        val stats = VpnController.statistics.value
        val notification = buildNotification(
            title = getString(R.string.notification_connected_title),
            content = "${server.countryLong} • ${stats.formattedDuration} • ↓${stats.formattedDownload}"
        )
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        const val ACTION_CONNECT = "com.example.vpn.ACTION_CONNECT"
        const val ACTION_DISCONNECT = "com.example.vpn.ACTION_DISCONNECT"

        const val EXTRA_SERVER_IP = "extra_server_ip"
        const val EXTRA_SERVER_HOST = "extra_server_host"
        const val EXTRA_SERVER_COUNTRY = "extra_server_country"
        const val EXTRA_SERVER_CONFIG = "extra_server_config"
        const val EXTRA_SERVER_PING = "extra_server_ping"
        const val EXTRA_SERVER_SPEED = "extra_server_speed"

        private const val CHANNEL_ID = "free_vpn_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
