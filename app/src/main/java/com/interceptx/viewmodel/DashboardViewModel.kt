package com.interceptx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interceptx.data.model.HttpTransaction
import com.interceptx.data.repository.InterceptXRepository
import com.interceptx.proxy.ProxyEngine
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val proxyRunning: Boolean = false,
    val totalRequests: Int = 0,
    val interceptedRequests: Int = 0,
    val activeConnections: Int = 0,
    val bandwidthBytes: Long = 0,
    val recentTraffic: List<HttpTransaction> = emptyList()
)

class DashboardViewModel(
    private val repository: InterceptXRepository,
    private val proxyEngine: ProxyEngine
) : ViewModel() {

    private val projectId = 1L
    private val proxyRunningFlow = MutableStateFlow(false)

    val uiState: StateFlow<DashboardUiState> = combine(
        proxyRunningFlow,
        repository.observeTotalCount(projectId),
        repository.observeInterceptedCount(projectId),
        repository.observeBandwidth(projectId),
        repository.observeRecent(projectId, 15)
    ) { running, total, intercepted, bandwidth, recent ->
        DashboardUiState(
            proxyRunning = running,
            totalRequests = total,
            interceptedRequests = intercepted,
            activeConnections = if (running) recent.count { it.responseTimeMs == null } else 0,
            bandwidthBytes = bandwidth,
            recentTraffic = recent
        )
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun toggleProxy(port: Int) {
        viewModelScope.launch {
            if (proxyEngine.isRunning) {
                proxyEngine.stop()
                proxyRunningFlow.value = false
            } else {
                proxyEngine.projectId = projectId
                proxyEngine.start(port)
                proxyRunningFlow.value = true
            }
        }
    }
}
