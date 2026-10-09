package com.example

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.VpnServer
import com.example.model.VpnStatus
import com.example.ui.screens.FavoritesScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.FreeVpnTheme
import com.example.ui.theme.PrimaryCyan
import com.example.ui.viewmodel.VpnViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: VpnViewModel by viewModels {
        VpnViewModel.Factory(FreeVpnApplication.instance.repository)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            FreeVpnTheme(darkTheme = true) {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun MainAppScreen(
    viewModel: VpnViewModel
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }
    var pendingServerToConnect by remember { mutableStateOf<VpnServer?>(null) }

    // الاستماع لرسائل المستخدم من ViewModel
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // إطلاق إذن الإشعارات لنظام أندرويد 13 فما فوق
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    // مشغل إذن الـ VPN من نظام أندرويد
    val vpnPrepareLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            pendingServerToConnect?.let { server ->
                viewModel.connectToServer(context, server)
                pendingServerToConnect = null
            }
        } else {
            pendingServerToConnect = null
        }
    }

    // دالة محاولة الاتصال بالخادم والتحقق من إذن VpnService
    fun requestConnect(targetContext: Context, server: VpnServer) {
        val prepareIntent: Intent? = VpnService.prepare(targetContext)
        if (prepareIntent != null) {
            pendingServerToConnect = server
            vpnPrepareLauncher.launch(prepareIntent)
        } else {
            viewModel.connectToServer(targetContext, server)
        }
    }

    // حالات الشاشة من ViewModel
    val vpnStatus by viewModel.vpnStatus.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val filteredServers by viewModel.filteredServers.collectAsStateWithLifecycle()
    val favoriteServers by viewModel.favoriteServers.collectAsStateWithLifecycle()
    val availableCountries by viewModel.availableCountries.collectAsStateWithLifecycle()
    val filterSort by viewModel.filterSort.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val showSecurityWarning by viewModel.showSecurityWarning.collectAsStateWithLifecycle()
    val selectedConfigServer by viewModel.selectedConfigServer.collectAsStateWithLifecycle()
    val selectedTargetServer by viewModel.selectedTargetServer.collectAsStateWithLifecycle()

    // الخادم المستهدف النشط أو المقترح
    val effectiveTarget = selectedTargetServer ?: filteredServers.firstOrNull() ?: favoriteServers.firstOrNull()

    // التعامل مع زر الرجوع
    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = DarkSurface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_navigation_bar")
            ) {
                // تبويب الخوادم
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 0) Icons.Filled.Dns else Icons.Outlined.Dns,
                            contentDescription = stringResource(R.string.tab_servers)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_servers)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PrimaryCyan,
                        indicatorColor = PrimaryCyan,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_servers")
                )

                // تبويب المفضلة
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 1) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = stringResource(R.string.tab_favorites)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_favorites)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PrimaryCyan,
                        indicatorColor = PrimaryCyan,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_favorites")
                )

                // تبويب الإعدادات
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(
                            imageVector = if (selectedTab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = stringResource(R.string.tab_settings)
                        )
                    },
                    label = { Text(stringResource(R.string.tab_settings)) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = PrimaryCyan,
                        indicatorColor = PrimaryCyan,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> HomeScreen(
                servers = filteredServers,
                availableCountries = availableCountries,
                filterSort = filterSort,
                vpnStatus = vpnStatus,
                targetServer = effectiveTarget,
                statistics = statistics,
                isRefreshing = isRefreshing,
                showSecurityWarning = showSecurityWarning,
                selectedConfigServer = selectedConfigServer,
                onRefresh = { viewModel.refreshServers() },
                onConnectServer = { ctx, server ->
                    viewModel.selectTargetServer(server)
                    requestConnect(ctx, server)
                },
                onToggleConnection = {
                    if (vpnStatus is VpnStatus.Connected || vpnStatus is VpnStatus.Connecting) {
                        viewModel.disconnect(context)
                    } else if (effectiveTarget != null) {
                        requestConnect(context, effectiveTarget)
                    } else {
                        viewModel.refreshServers()
                    }
                },
                onServerSelect = { viewModel.selectTargetServer(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onTestPing = { viewModel.testPing(it) },
                onSearchChange = { viewModel.updateSearchQuery(it) },
                onCountryChange = { viewModel.updateCountryFilter(it) },
                onSortChange = { viewModel.updateSortOption(it) },
                onOpenSecurityWarning = { viewModel.setShowSecurityWarning(true) },
                onDismissSecurityWarning = { viewModel.setShowSecurityWarning(false) },
                onSelectConfigServer = { viewModel.selectConfigServer(it) },
                modifier = Modifier.padding(innerPadding)
            )
            1 -> FavoritesScreen(
                favoriteServers = favoriteServers,
                vpnStatus = vpnStatus,
                targetServer = effectiveTarget,
                selectedConfigServer = selectedConfigServer,
                onConnectServer = { ctx, server ->
                    viewModel.selectTargetServer(server)
                    requestConnect(ctx, server)
                },
                onServerSelect = { viewModel.selectTargetServer(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onTestPing = { viewModel.testPing(it) },
                onSelectConfigServer = { viewModel.selectConfigServer(it) },
                modifier = Modifier.padding(innerPadding)
            )
            2 -> SettingsScreen(
                filterSort = filterSort,
                onMinSpeedChange = { viewModel.updateMinSpeed(it) },
                onManualRefresh = { viewModel.refreshServers() },
                onOpenSecurityWarning = { viewModel.setShowSecurityWarning(true) },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
