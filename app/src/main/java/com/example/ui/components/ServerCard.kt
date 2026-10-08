package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.VpnServer
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SecondaryEmerald
import com.example.ui.theme.StatusAmber
import com.example.ui.theme.StatusRed

@Composable
fun ServerCard(
    server: VpnServer,
    isSelected: Boolean,
    onConnectClick: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onTestPingClick: () -> Unit,
    onViewConfigClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isSelected) PrimaryCyan else Color.Transparent

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(if (isSelected) 2.dp else 0.dp, borderColor, RoundedCornerShape(16.dp))
            .testTag("server_card_${server.ip}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // الصف العلوي: علم الدولة + الاسم + زر المفضلة
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = server.flagEmoji,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = server.countryLong,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${server.ip} • ${server.hostName.take(24)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = onFavoriteToggle,
                    modifier = Modifier.testTag("favorite_button_${server.ip}")
                ) {
                    Icon(
                        imageVector = if (server.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (server.isFavorite) StatusAmber else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // شارات القياسات: Ping + Speed + Sessions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // شارة Ping
                val ping = server.effectivePing
                val pingColor = when {
                    ping < 60 -> SecondaryEmerald
                    ping < 150 -> StatusAmber
                    else -> StatusRed
                }
                MetricBadge(
                    label = if (server.measuredPingMs != null) "Ping ${ping}ms (live)" else "Ping ${ping}ms",
                    color = pingColor
                )

                // شارة السرعة
                MetricBadge(
                    label = server.formattedSpeed,
                    color = PrimaryCyan
                )

                // شارة عدد الجلسات
                if (server.numVpnSessions > 0) {
                    MetricBadge(
                        label = "${server.numVpnSessions} sessions",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // الصف السفلي: أزرار التحكم
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // زر اختبار Ping السريع
                    IconButton(
                        onClick = onTestPingClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("test_ping_${server.ip}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NetworkCheck,
                            contentDescription = stringResource(R.string.btn_test_ping),
                            tint = PrimaryCyan,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // زر عرض ملف OpenVPN
                    IconButton(
                        onClick = onViewConfigClick,
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("view_config_${server.ip}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = stringResource(R.string.btn_view_config),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // زر الاتصال
                Button(
                    onClick = onConnectClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) SecondaryEmerald else PrimaryCyan,
                        contentColor = Color.Black
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("connect_button_${server.ip}")
                ) {
                    Text(
                        text = if (isSelected) stringResource(R.string.status_connected) else stringResource(R.string.btn_connect),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBadge(
    label: String,
    color: Color
) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(color, CircleShape)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}
