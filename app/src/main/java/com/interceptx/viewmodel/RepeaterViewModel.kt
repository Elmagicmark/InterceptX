package com.interceptx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interceptx.data.model.RepeaterTab
import com.interceptx.data.repository.InterceptXRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class RepeaterSendResult(val statusCode: Int, val headers: String, val body: String, val timeMs: Long)

class RepeaterViewModel(private val repository: InterceptXRepository) : ViewModel() {

    private val projectId = 1L

    val tabs: StateFlow<List<RepeaterTab>> = repository.observeRepeaterTabs(projectId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sending = MutableStateFlow<Long?>(null)   // tab id currently in flight

    fun newTab() {
        viewModelScope.launch {
            val count = tabs.value.size
            repository.saveRepeaterTab(
                RepeaterTab(
                    projectId = projectId,
                    label = "Request ${count + 1}",
                    method = "GET",
                    url = "https://",
                    headers = JSONObject().apply { put("User-Agent", "InterceptX/1.0") }.toString(),
                    body = "",
                    bodyType = "JSON",
                    orderIndex = count
                )
            )
        }
    }

    fun updateTab(tab: RepeaterTab) {
        viewModelScope.launch { repository.saveRepeaterTab(tab) }
    }

    fun deleteTab(tab: RepeaterTab) {
        viewModelScope.launch { repository.deleteRepeaterTab(tab) }
    }

    /** Loads a captured/history transaction into a new repeater tab ("Send to Repeater"). */
    fun sendToRepeater(method: String, url: String, headersJson: String, body: String?) {
        viewModelScope.launch {
            val count = tabs.value.size
            repository.saveRepeaterTab(
                RepeaterTab(
                    projectId = projectId, label = "From History ${count + 1}", method = method, url = url,
                    headers = headersJson, body = body ?: "", bodyType = "JSON", orderIndex = count
                )
            )
        }
    }

    fun sendRequest(tab: RepeaterTab) {
        viewModelScope.launch {
            sending.value = tab.id
            val result = withContext(Dispatchers.IO) { performRequest(tab) }
            sending.value = null
            result?.let {
                repository.saveRepeaterTab(
                    tab.copy(
                        lastStatusCode = it.statusCode,
                        lastResponseHeaders = it.headers,
                        lastResponseBody = it.body,
                        lastResponseTimeMs = it.timeMs
                    )
                )
            }
        }
    }

    private fun performRequest(tab: RepeaterTab): RepeaterSendResult? = try {
        val start = System.currentTimeMillis()
        val conn = URL(tab.url).openConnection() as HttpURLConnection
        conn.requestMethod = tab.method
        conn.connectTimeout = 15_000
        conn.readTimeout = 15_000

        val headerMap = runCatching { JSONObject(tab.headers) }.getOrDefault(JSONObject())
        headerMap.keys().forEach { key -> conn.setRequestProperty(key, headerMap.getString(key)) }

        if (tab.method in listOf("POST", "PUT", "PATCH") && tab.body.isNotEmpty()) {
            conn.doOutput = true
            conn.outputStream.use { it.write(tab.body.toByteArray(Charsets.UTF_8)) }
        }

        val status = conn.responseCode
        val respHeaders = JSONObject().apply {
            conn.headerFields.forEach { (k, v) -> if (k != null) put(k, v.joinToString("; ")) }
        }
        val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val respBody = stream?.bufferedReader()?.use { it.readText() } ?: ""
        val elapsed = System.currentTimeMillis() - start
        RepeaterSendResult(status, respHeaders.toString(), respBody, elapsed)
    } catch (e: Exception) {
        RepeaterSendResult(-1, "{}", "Request failed: ${e.message}", 0)
    }
}
