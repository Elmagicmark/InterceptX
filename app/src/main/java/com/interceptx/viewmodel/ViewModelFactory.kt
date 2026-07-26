package com.interceptx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.interceptx.InterceptXApp

class ViewModelFactory(private val app: InterceptXApp) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when (modelClass) {
        DashboardViewModel::class.java -> DashboardViewModel(app.repository, app.proxyEngine) as T
        InterceptViewModel::class.java -> InterceptViewModel(app.repository, app.interceptQueue) as T
        HistoryViewModel::class.java -> HistoryViewModel(app.repository) as T
        RepeaterViewModel::class.java -> RepeaterViewModel(app.repository) as T
        CertificatesViewModel::class.java -> CertificatesViewModel(app.certificateAuthority) as T
        ProjectsViewModel::class.java -> ProjectsViewModel(app.repository) as T
        SettingsViewModel::class.java -> SettingsViewModel(app.repository, app.proxyEngine) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: $modelClass")
    }
}
