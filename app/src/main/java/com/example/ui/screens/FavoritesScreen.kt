package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.VpnServer
import com.example.model.VpnStatus
import com.example.ui.components.ConfigPreviewDialog
import com.example.ui.components.ServerCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    favoriteServers: List<VpnServer>,
    vpnStatus: VpnStatus,
    selectedConfigServer: VpnServer?,
    onConnectServer: (Context, VpnServer) -> Unit,
    onToggleFavorite: (VpnServer) -> Unit,
    onTestPing: (VpnServer) -> Unit,
    onSelectConfigServer: (VpnServer?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    selectedConfigServer?.let { server ->
        ConfigPreviewDialog(
            server = server,
            onDismiss = { onSelectConfigServer(null) }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.tab_favorites),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        if (favoriteServers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "⭐", fontSize = 48.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.empty_favorites),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp)
            ) {
                items(favoriteServers, key = { it.ip }) { server ->
                    val isConnectedServer = (vpnStatus is VpnStatus.Connected && vpnStatus.server.ip == server.ip) ||
                        (vpnStatus is VpnStatus.Connecting && vpnStatus.server.ip == server.ip)

                    ServerCard(
                        server = server,
                        isSelected = isConnectedServer,
                        onConnectClick = { onConnectServer(context, server) },
                        onFavoriteToggle = { onToggleFavorite(server) },
                        onTestPingClick = { onTestPing(server) },
                        onViewConfigClick = { onSelectConfigServer(server) }
                    )
                }
            }
        }
    }
}
