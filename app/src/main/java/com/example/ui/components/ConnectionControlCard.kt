package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.VpnServer
import com.example.model.VpnStatistics
import com.example.model.VpnStatus
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SecondaryEmerald
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusRed

@Composable
fun ConnectionControlCard(
    status: VpnStatus,
    statistics: VpnStatistics,
    onToggleConnection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isConnected = status is VpnStatus.Connected
    val isConnecting = status is VpnStatus.Connecting
    val isDisconnecting = status is VpnStatus.Disconnecting

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isConnected || isConnecting) 1.08f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val buttonColor by animateColorAsState(
        targetValue = when {
            isConnected -> SecondaryEmerald
            isConnecting -> StatusAmber
            isDisconnecting -> StatusRed
            else -> PrimaryCyan
        },
        label = "btnColor"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // شارة حالة الاتصال
            StatusPill(status = status)

            Spacer(modifier = Modifier.height(20.dp))

            // زر الاتصال الدائري الكبير
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                // هالة النبض عند الاتصال
                if (isConnected || isConnecting) {
                    Box(
                        modifier = Modifier
                            .size(126.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(buttonColor.copy(alpha = 0.2f))
                    )
                }

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(buttonColor, buttonColor.copy(alpha = 0.75f))
                            )
                        )
                        .border(3.dp, Color.White.copy(alpha = 0.3f), CircleShape)
                        .clickable(enabled = !isDisconnecting) { onToggleConnection() }
                        .testTag("vpn_power_button")
                ) {
                    if (isConnecting || isDisconnecting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(46.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = if (isConnected) stringResource(R.string.btn_disconnect) else stringResource(R.string.btn_connect),
                            tint = Color.Black,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // معلومات الخادم المتصل
            when (status) {
                is VpnStatus.Connected -> {
                    ServerInfoDisplay(server = status.server)
                }
                is VpnStatus.Connecting -> {
                    Text(
                        text = "${status.server.flagEmoji} ${status.server.countryLong}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.status_connecting),
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusAmber
                    )
                }
                is VpnStatus.Error -> {
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = StatusRed
                    )
                }
                else -> {
                    Text(
                        text = stringResource(R.string.status_disconnected),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // إحصائيات الجلسة الحية عند الاتصال
            AnimatedVisibility(visible = isConnected) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        MetricItem(
                            icon = Icons.Default.Timer,
                            label = stringResource(R.string.duration),
                            value = statistics.formattedDuration
                        )
                        MetricItem(
                            icon = Icons.Default.ArrowDownward,
                            label = "Download",
                            value = statistics.formattedDownload
                        )
                        MetricItem(
                            icon = Icons.Default.ArrowUpward,
                            label = "Upload",
                            value = statistics.formattedUpload
                        )
                        MetricItem(
                            icon = Icons.Default.Speed,
                            label = stringResource(R.string.server_ping),
                            value = "${statistics.currentPingMs} ms"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusPill(status: VpnStatus) {
    val (labelRes, dotColor) = when (status) {
        is VpnStatus.Connected -> R.string.status_connected to SecondaryEmerald
        is VpnStatus.Connecting -> R.string.status_connecting to StatusAmber
        is VpnStatus.Disconnecting -> R.string.status_disconnecting to StatusRed
        is VpnStatus.Error -> R.string.status_error to StatusRed
        VpnStatus.Disconnected -> R.string.status_disconnected to Color.Gray
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(dotColor.copy(alpha = 0.15f))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = dotColor
        )
    }
}

@Composable
private fun ServerInfoDisplay(server: VpnServer) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "${server.flagEmoji} ${server.countryLong}",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "${server.ip} • ${server.formattedSpeed}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MetricItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PrimaryCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
