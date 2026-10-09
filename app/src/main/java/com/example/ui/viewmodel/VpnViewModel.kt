package com.example.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.VpnRepository
import com.example.model.FilterSortOptions
import com.example.model.SortOption
import com.example.model.VpnServer
import com.example.model.VpnStatistics
import com.example.model.VpnStatus
import com.example.vpn.VpnController
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class VpnViewModel(
    private val repository: VpnRepository
) : ViewModel() {

    val vpnStatus: StateFlow<VpnStatus> = VpnController.vpnStatus
    val statistics: StateFlow<VpnStatistics> = VpnController.statistics

    private val _filterSort = MutableStateFlow(FilterSortOptions())
    val filterSort: StateFlow<FilterSortOptions> = _filterSort.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val _showSecurityWarning = MutableStateFlow(false)
    val showSecurityWarning: StateFlow<Boolean> = _showSecurityWarning.asStateFlow()

    private val _selectedConfigServer = MutableStateFlow<VpnServer?>(null)
    val selectedConfigServer: StateFlow<VpnServer?> = _selectedConfigServer.asStateFlow()

    // الخادم المستهدف المحدد يدوياً أو تلقائياً للاتصال السريع
    private val _selectedTargetServer = MutableStateFlow<VpnServer?>(null)
    val selectedTargetServer: StateFlow<VpnServer?> = _selectedTargetServer.asStateFlow()

    val availableCountries: StateFlow<List<String>> = repository.availableCountries
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredServers: StateFlow<List<VpnServer>> = combine(
        repository.allServers,
        _filterSort
    ) { servers, filter ->
        var list = servers

        // فلترة بالبحث
        if (filter.searchQuery.isNotBlank()) {
            val q = filter.searchQuery.trim().lowercase()
            list = list.filter {
                it.countryLong.lowercase().contains(q) ||
                    it.countryShort.lowercase().contains(q) ||
                    it.ip.contains(q) ||
                    it.hostName.lowercase().contains(q)
            }
        }

        // فلترة بالدولة
        if (filter.selectedCountry != "ALL") {
            list = list.filter { it.countryShort.equals(filter.selectedCountry, ignoreCase = true) }
        }

        // فلترة بالحد الأدنى للسرعة
        if (filter.minSpeedMbps > 0) {
            list = list.filter { it.speedMbps >= filter.minSpeedMbps }
        }

        // فلترة بالمفضلة
        if (filter.showOnlyFavorites) {
            list = list.filter { it.isFavorite }
        }

        // الترتيب
        when (filter.sortOption) {
            SortOption.FASTEST_PING -> list.sortedBy { it.effectivePing }
            SortOption.HIGHEST_SPEED -> list.sortedByDescending { it.speedBps }
            SortOption.BEST_SCORE -> list.sortedByDescending { it.score }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteServers: StateFlow<List<VpnServer>> = repository.favoriteServers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            // ضمان وجود خوادم فورية للمستخدم دون انتظار
            repository.ensureInitialServers()
            // جلب أحدث الخوادم من الإنترنت في الخلفية
            refreshServers(isSilent = true)
        }
    }

    fun selectTargetServer(server: VpnServer?) {
        _selectedTargetServer.value = server
    }

    fun refreshServers(isSilent: Boolean = false) {
        viewModelScope.launch {
            _isRefreshing.value = true
            val result = repository.refreshServers()
            _isRefreshing.value = false

            result.fold(
                onSuccess = { count ->
                    if (!isSilent) {
                        _userMessage.emit("تم تحديث $count خادم بنجاح")
                    }
                },
                onFailure = { error ->
                    if (!isSilent) {
                        _userMessage.emit("تعذر تحديث الخوادم: ${error.localizedMessage}")
                    }
                }
            )
        }
    }

    fun toggleFavorite(server: VpnServer) {
        viewModelScope.launch {
            repository.toggleFavorite(server.ip, server.isFavorite)
        }
    }

    fun testPing(server: VpnServer) {
        viewModelScope.launch {
            val latency = repository.testPing(server)
            if (latency > 0) {
                _userMessage.emit("${server.countryLong} (${server.ip}): ${latency} ms")
            } else {
                _userMessage.emit("${server.countryLong}: تعذر الوصول للخادم حالياً")
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _filterSort.value = _filterSort.value.copy(searchQuery = query)
    }

    fun updateCountryFilter(country: String) {
        _filterSort.value = _filterSort.value.copy(selectedCountry = country)
    }

    fun updateSortOption(sortOption: SortOption) {
        _filterSort.value = _filterSort.value.copy(sortOption = sortOption)
    }

    fun updateMinSpeed(minSpeedMbps: Double) {
        _filterSort.value = _filterSort.value.copy(minSpeedMbps = minSpeedMbps)
    }

    fun setShowSecurityWarning(show: Boolean) {
        _showSecurityWarning.value = show
    }

    fun selectConfigServer(server: VpnServer?) {
        _selectedConfigServer.value = server
    }

    fun connectToServer(context: Context, server: VpnServer) {
        _selectedTargetServer.value = server
        VpnController.connect(context, server)
    }

    fun disconnect(context: Context) {
        VpnController.disconnect(context)
    }

    class Factory(private val repository: VpnRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return VpnViewModel(repository) as T
        }
    }
}
