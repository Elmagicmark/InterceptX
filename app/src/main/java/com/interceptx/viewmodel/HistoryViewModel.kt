package com.interceptx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interceptx.data.model.HttpTransaction
import com.interceptx.data.repository.InterceptXRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class SortMode { NEWEST, OLDEST, SLOWEST, STATUS }

data class HistoryFilterState(
    val searchText: String = "",
    val methodFilter: String = "",
    val statusClass: String = "",
    val sortMode: SortMode = SortMode.NEWEST
)

class HistoryViewModel(private val repository: InterceptXRepository) : ViewModel() {

    private val projectId = 1L
    private val filterState = MutableStateFlow(HistoryFilterState())
    val filter: StateFlow<HistoryFilterState> = filterState

    val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val transactions: StateFlow<List<HttpTransaction>> = filterState.flatMapLatest { f ->
        repository.observeHistory(projectId, f.searchText, f.methodFilter, f.statusClass, f.sortMode.name)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearch(text: String) { filterState.value = filterState.value.copy(searchText = text) }
    fun updateMethod(method: String) { filterState.value = filterState.value.copy(methodFilter = method) }
    fun updateStatusClass(cls: String) { filterState.value = filterState.value.copy(statusClass = cls) }
    fun updateSort(mode: SortMode) { filterState.value = filterState.value.copy(sortMode = mode) }

    fun toggleSelection(id: Long) {
        selectedIds.value = if (id in selectedIds.value) selectedIds.value - id else selectedIds.value + id
    }

    fun clearSelection() { selectedIds.value = emptySet() }

    fun deleteSelected() {
        viewModelScope.launch {
            repository.deleteTransactions(selectedIds.value.toList())
            clearSelection()
        }
    }

    /** Serializes the whole project's history to a JSON string suitable for file export. */
    suspend fun exportToJson(): String {
        val all = repository.exportProject(projectId)
        val array = JSONArray()
        all.forEach { t ->
            array.put(
                JSONObject().apply {
                    put("method", t.method); put("url", t.url); put("host", t.host); put("scheme", t.scheme)
                    put("requestHeaders", t.requestHeaders); put("requestBody", t.requestBody ?: JSONObject.NULL)
                    put("responseStatusCode", t.responseStatusCode ?: JSONObject.NULL)
                    put("responseHeaders", t.responseHeaders ?: JSONObject.NULL)
                    put("responseBody", t.responseBody ?: JSONObject.NULL)
                    put("responseTimeMs", t.responseTimeMs ?: JSONObject.NULL)
                    put("sizeBytes", t.sizeBytes); put("timestamp", t.timestamp); put("state", t.state.name)
                }
            )
        }
        return array.toString(2)
    }

    /** Parses a previously-exported JSON array back into transactions for this project. */
    fun importFromJson(json: String) {
        viewModelScope.launch {
            val array = JSONArray(json)
            val list = mutableListOf<HttpTransaction>()
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                list += HttpTransaction(
                    projectId = projectId,
                    method = o.getString("method"),
                    url = o.getString("url"),
                    host = o.optString("host"),
                    scheme = o.optString("scheme", "http"),
                    requestHeaders = o.optString("requestHeaders", "{}"),
                    requestBody = o.optString("requestBody", null).takeIf { it != "null" },
                    responseStatusCode = o.optInt("responseStatusCode", -1).takeIf { it != -1 },
                    responseHeaders = o.optString("responseHeaders", null).takeIf { it != "null" },
                    responseBody = o.optString("responseBody", null).takeIf { it != "null" },
                    responseTimeMs = o.optLong("responseTimeMs", -1).takeIf { it != -1L },
                    sizeBytes = o.optLong("sizeBytes", 0),
                    timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                    state = runCatching { com.interceptx.data.model.TransactionState.valueOf(o.optString("state")) }
                        .getOrDefault(com.interceptx.data.model.TransactionState.COMPLETED),
                    inScope = true
                )
            }
            repository.importTransactions(list)
        }
    }
}
