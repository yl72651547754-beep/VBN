package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.FilterSortOptions
import com.example.model.SortOption
import com.example.model.VpnServer
import com.example.model.VpnStatistics
import com.example.model.VpnStatus
import com.example.ui.components.ConfigPreviewDialog
import com.example.ui.components.ConnectionControlCard
import com.example.ui.components.SecurityDisclaimerDialog
import com.example.ui.components.ServerCard
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.PrimaryCyan
import com.example.ui.theme.SecondaryEmerald

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    servers: List<VpnServer>,
    availableCountries: List<String>,
    filterSort: FilterSortOptions,
    vpnStatus: VpnStatus,
    targetServer: VpnServer?,
    statistics: VpnStatistics,
    isRefreshing: Boolean,
    showSecurityWarning: Boolean,
    selectedConfigServer: VpnServer?,
    onRefresh: () -> Unit,
    onConnectServer: (Context, VpnServer) -> Unit,
    onToggleConnection: () -> Unit,
    onServerSelect: (VpnServer) -> Unit,
    onToggleFavorite: (VpnServer) -> Unit,
    onTestPing: (VpnServer) -> Unit,
    onSearchChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onSortChange: (SortOption) -> Unit,
    onOpenSecurityWarning: () -> Unit,
    onDismissSecurityWarning: () -> Unit,
    onSelectConfigServer: (VpnServer?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showCountryDropdown by remember { mutableStateOf(false) }

    // حوار تحذير الأمان للشبكات العامة
    if (showSecurityWarning) {
        SecurityDisclaimerDialog(onDismiss = onDismissSecurityWarning)
    }

    // حوار عرض ونسخ إعدادات OpenVPN
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GATE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryCyan,
                            modifier = Modifier
                                .background(
                                    color = PrimaryCyan.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onOpenSecurityWarning,
                        modifier = Modifier.testTag("security_warning_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = stringResource(R.string.security_warning_title),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    IconButton(
                        onClick = onRefresh,
                        enabled = !isRefreshing,
                        modifier = Modifier.testTag("refresh_servers_button")
                    ) {
                        if (isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = PrimaryCyan
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.btn_refresh),
                                tint = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        containerColor = DarkBackground,
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // بطاقة التحكم بالاتصال العلوية (Hero Card)
            item {
                ConnectionControlCard(
                    status = vpnStatus,
                    targetServer = targetServer,
                    statistics = statistics,
                    onToggleConnection = onToggleConnection
                )
            }

            // شريط البحث
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = filterSort.searchQuery,
                        onValueChange = onSearchChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("search_servers_input"),
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search_servers_hint),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (filterSort.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = PrimaryCyan,
                            unfocusedBorderColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedContainerColor = DarkSurface,
                            unfocusedContainerColor = DarkSurface
                        )
                    )
                }
            }

            // خيارات الترتيب والفلترة
            item {
                val scrollState = rememberScrollState()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(scrollState)
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // قائمة اختيار الدولة
                    Box {
                        FilterChip(
                            selected = filterSort.selectedCountry != "ALL",
                            onClick = { showCountryDropdown = true },
                            label = {
                                Text(
                                    text = if (filterSort.selectedCountry == "ALL") {
                                        stringResource(R.string.filter_all)
                                    } else {
                                        "${VpnServer.getFlagEmoji(filterSort.selectedCountry)} ${filterSort.selectedCountry}"
                                    }
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = PrimaryCyan.copy(alpha = 0.2f),
                                selectedLabelColor = PrimaryCyan
                            )
                        )

                        DropdownMenu(
                            expanded = showCountryDropdown,
                            onDismissRequest = { showCountryDropdown = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(text = stringResource(R.string.filter_all)) },
                                onClick = {
                                    onCountryChange("ALL")
                                    showCountryDropdown = false
                                }
                            )
                            availableCountries.forEach { code ->
                                DropdownMenuItem(
                                    text = { Text(text = "${VpnServer.getFlagEmoji(code)} $code") },
                                    onClick = {
                                        onCountryChange(code)
                                        showCountryDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // شريحة ترتيب: الأسرع (Ping)
                    FilterChip(
                        selected = filterSort.sortOption == SortOption.FASTEST_PING,
                        onClick = { onSortChange(SortOption.FASTEST_PING) },
                        label = { Text(text = stringResource(R.string.sort_ping)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SecondaryEmerald.copy(alpha = 0.2f),
                            selectedLabelColor = SecondaryEmerald
                        )
                    )

                    // شريحة ترتيب: أعلى سرعة
                    FilterChip(
                        selected = filterSort.sortOption == SortOption.HIGHEST_SPEED,
                        onClick = { onSortChange(SortOption.HIGHEST_SPEED) },
                        label = { Text(text = stringResource(R.string.sort_speed)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryCyan.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryCyan
                        )
                    )

                    // شريحة ترتيب: أفضل تقييم
                    FilterChip(
                        selected = filterSort.sortOption == SortOption.BEST_SCORE,
                        onClick = { onSortChange(SortOption.BEST_SCORE) },
                        label = { Text(text = stringResource(R.string.sort_score)) }
                    )
                }
            }

            // قائمة الخوادم أو الحالة الفارغة
            if (servers.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "🌐",
                                fontSize = 48.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.empty_servers),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryCyan,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(text = stringResource(R.string.btn_refresh), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            } else {
                items(servers, key = { it.ip }) { server ->
                    val isConnectedServer = (vpnStatus is VpnStatus.Connected && vpnStatus.server.ip == server.ip) ||
                        (vpnStatus is VpnStatus.Connecting && vpnStatus.server.ip == server.ip)
                    val isTargetServer = targetServer?.ip == server.ip

                    ServerCard(
                        server = server,
                        isSelected = isConnectedServer || isTargetServer,
                        onCardClick = { onServerSelect(server) },
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
