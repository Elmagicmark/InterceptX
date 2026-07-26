package com.interceptx.data.repository

import com.interceptx.data.db.AppDatabase
import com.interceptx.data.model.*
import kotlinx.coroutines.flow.Flow

class InterceptXRepository(private val db: AppDatabase) {

    // ---- Transactions / History ----
    fun observeRecent(projectId: Long, limit: Int = 20): Flow<List<HttpTransaction>> =
        db.transactionDao().observeRecent(projectId, limit)

    fun observeIntercepted(projectId: Long): Flow<List<HttpTransaction>> =
        db.transactionDao().observeIntercepted(projectId)

    fun observeHistory(
        projectId: Long,
        searchText: String = "",
        methodFilter: String = "",
        statusClass: String = "",
        sortMode: String = "NEWEST"
    ): Flow<List<HttpTransaction>> =
        db.transactionDao().observeHistory(projectId, searchText, methodFilter, statusClass, sortMode)

    fun observeTotalCount(projectId: Long) = db.transactionDao().observeTotalCount(projectId)
    fun observeInterceptedCount(projectId: Long) = db.transactionDao().observeInterceptedCount(projectId)
    fun observeBandwidth(projectId: Long) = db.transactionDao().observeTotalBandwidth(projectId)

    suspend fun insertTransaction(t: HttpTransaction) = db.transactionDao().insert(t)
    suspend fun updateTransaction(t: HttpTransaction) = db.transactionDao().update(t)
    suspend fun deleteTransactions(ids: List<Long>) = db.transactionDao().deleteByIds(ids)
    suspend fun getTransaction(id: Long) = db.transactionDao().getById(id)
    suspend fun exportProject(projectId: Long) = db.transactionDao().getAllForExport(projectId)
    suspend fun importTransactions(list: List<HttpTransaction>) = db.transactionDao().insertAll(list)

    // ---- Projects / Scope ----
    fun observeProjects(): Flow<List<Project>> = db.projectDao().observeAll()
    suspend fun createProject(name: String): Long =
        db.projectDao().insert(Project(name = name, createdAt = System.currentTimeMillis()))
    suspend fun setActiveProject(id: Long) {
        db.projectDao().clearActive()
        db.projectDao().setActive(id)
    }

    fun observeScope(projectId: Long): Flow<List<ScopeRule>> = db.scopeRuleDao().observeForProject(projectId)
    suspend fun addScopeRule(rule: ScopeRule) = db.scopeRuleDao().insert(rule)
    suspend fun removeScopeRule(rule: ScopeRule) = db.scopeRuleDao().delete(rule)

    // ---- Repeater ----
    fun observeRepeaterTabs(projectId: Long): Flow<List<RepeaterTab>> =
        db.repeaterTabDao().observeForProject(projectId)
    suspend fun saveRepeaterTab(tab: RepeaterTab): Long =
        if (tab.id == 0L) db.repeaterTabDao().insert(tab) else { db.repeaterTabDao().update(tab); tab.id }
    suspend fun deleteRepeaterTab(tab: RepeaterTab) = db.repeaterTabDao().delete(tab)

    // ---- Settings ----
    fun observeSettings(): Flow<ProxySettings?> = db.proxySettingsDao().observe()
    suspend fun getSettings(): ProxySettings = db.proxySettingsDao().get() ?: ProxySettings().also {
        db.proxySettingsDao().upsert(it)
    }
    suspend fun updateSettings(settings: ProxySettings) = db.proxySettingsDao().upsert(settings)
}
