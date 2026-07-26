package com.interceptx.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class TransactionState { INTERCEPTED, FORWARDED, DROPPED, COMPLETED, FAILED }

@Entity(tableName = "http_transactions")
data class HttpTransaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val method: String,
    val url: String,
    val host: String,
    val scheme: String,               // http | https
    val requestHeaders: String,       // JSON-encoded map
    val requestBody: String?,
    val responseStatusCode: Int?,
    val responseHeaders: String?,     // JSON-encoded map
    val responseBody: String?,
    val responseTimeMs: Long?,
    val sizeBytes: Long,
    val timestamp: Long,
    val state: TransactionState,
    val inScope: Boolean,
    val tag: String? = null           // e.g. "repeater", "composer"
)
