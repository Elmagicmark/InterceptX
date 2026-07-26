package com.interceptx.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
    val isActive: Boolean = false
)

enum class ScopeType { IN_SCOPE, OUT_OF_SCOPE }

@Entity(tableName = "scope_rules")
data class ScopeRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val pattern: String,      // e.g. "*.example.com/api/*"
    val type: ScopeType
)

@Entity(tableName = "repeater_tabs")
data class RepeaterTab(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val label: String,
    val method: String,
    val url: String,
    val headers: String,          // JSON-encoded map
    val body: String,
    val bodyType: String,         // JSON | TEXT | FORM_DATA
    val lastStatusCode: Int? = null,
    val lastResponseHeaders: String? = null,
    val lastResponseBody: String? = null,
    val lastResponseTimeMs: Long? = null,
    val orderIndex: Int = 0
)

@Entity(tableName = "proxy_settings")
data class ProxySettings(
    @PrimaryKey val id: Int = 1,           // singleton row
    val proxyHost: String = "127.0.0.1",
    val proxyPort: Int = 8080,
    val interceptEnabled: Boolean = false,
    val interceptRequests: Boolean = true,
    val interceptResponses: Boolean = false,
    val upstreamProxyEnabled: Boolean = false,
    val upstreamHost: String = "",
    val upstreamPort: Int = 0,
    val tlsBypassEnabled: Boolean = true,
    val activeProjectId: Long = 1
)
