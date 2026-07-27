package com.interceptx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interceptx.data.model.Project
import com.interceptx.data.model.ProxySettings
import com.interceptx.data.model.ScopeRule
import com.interceptx.data.model.ScopeType
import com.interceptx.data.repository.InterceptXRepository
import com.interceptx.proxy.CertificateAuthority
import com.interceptx.proxy.ProxyEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CertificatesViewModel(private val ca: CertificateAuthority) : ViewModel() {
    val fingerprintSha256: String get() = ca.rootFingerprintSha256()
    fun exportPem(): String = ca.exportRootCertPem()
    fun certificateBytes(): ByteArray = ca.rootCertificate().encoded
}

class ProjectsViewModel(private val repository: InterceptXRepository) : ViewModel() {
    val projects: StateFlow<List<Project>> = repository.observeProjects()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val scopeRules: StateFlow<List<ScopeRule>> = repository.observeScope(1L)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createProject(name: String) {
        viewModelScope.launch { repository.createProject(name) }
    }

    fun setActive(id: Long) {
        viewModelScope.launch { repository.setActiveProject(id) }
    }

    fun addScopeRule(pattern: String, type: ScopeType) {
        viewModelScope.launch { repository.addScopeRule(ScopeRule(projectId = 1L, pattern = pattern, type = type)) }
    }

    fun removeScopeRule(rule: ScopeRule) {
        viewModelScope.launch { repository.removeScopeRule(rule) }
    }
}

class SettingsViewModel(
    private val repository: InterceptXRepository,
    private val proxyEngine: ProxyEngine
) : ViewModel() {
    val settings: StateFlow<ProxySettings?> = repository.observeSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun update(settings: ProxySettings) {
        viewModelScope.launch { repository.updateSettings(settings) }
        proxyEngine.interceptEnabled = settings.interceptEnabled
    }
}
