package com.interceptx.viewmodel

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interceptx.data.model.HttpTransaction
import com.interceptx.data.repository.InterceptXRepository
import com.interceptx.proxy.ProxyEngine
import com.interceptx.proxy.ProxyForegroundService
import kotlinx.coroutines.flow.*

data class DashboardUiState(
    val proxyRunning: Boolean = false,
    val totalRequests: Int = 0,
    val interceptedRequests: Int = 0,
    val activeConnections: Int = 0,
    val bandwidthBytes: Long = 0,
    val recentTraffic: List<HttpTransaction> = emptyList(),
    val lastError: String? = null
)

class DashboardViewModel(
    private val appContext: Context,
    private val repository: InterceptXRepository,
    private val proxyEngine: ProxyEngine
) : ViewModel() {

    private val projectId = 1L

    val uiState: StateFlow<DashboardUiState> = combine(
        proxyEngine.isRunningState,
        proxyEngine.lastError,
        repository.observeTotalCount(projectId),
        repository.observeInterceptedCount(projectId),
        repository.observeBandwidth(projectId),
        repository.observeRecent(projectId, 15)
    ) { flows ->
        val running = flows[0] as Boolean
        val error = flows[1] as String?
        val total = flows[2] as Int
        val intercepted = flows[3] as Int
        val bandwidth = flows[4] as Long
        @Suppress("UNCHECKED_CAST")
        val recent = flows[5] as List<HttpTransaction>
        DashboardUiState(
            proxyRunning = running,
            totalRequests = total,
            interceptedRequests = intercepted,
            activeConnections = if (running) recent.count { it.responseTimeMs == null } else 0,
            bandwidthBytes = bandwidth,
            recentTraffic = recent,
            lastError = error
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    /**
     * Starts/stops the proxy through [ProxyForegroundService] rather than calling
     * [ProxyEngine] directly, so a persistent notification keeps the process alive
     * while you're tabbed away in a browser configuring FoxyProxy or similar.
     */
    fun toggleProxy(port: Int) {
        if (proxyEngine.isRunningState.value) {
            val intent = Intent(appContext, ProxyForegroundService::class.java).apply {
                action = ProxyForegroundService.ACTION_STOP
            }
            appContext.startService(intent)
        } else {
            proxyEngine.projectId = projectId
            val intent = Intent(appContext, ProxyForegroundService::class.java).apply {
                action = ProxyForegroundService.ACTION_START
                putExtra(ProxyForegroundService.EXTRA_PORT, port)
            }
            ContextCompat.startForegroundService(appContext, intent)
        }
    }
}
