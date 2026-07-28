package com.interceptx.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.interceptx.data.model.HttpTransaction
import com.interceptx.data.repository.InterceptXRepository
import com.interceptx.proxy.InterceptDecision
import com.interceptx.proxy.InterceptQueue
import com.interceptx.proxy.PendingRequest
import com.interceptx.proxy.ProxyEngine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InterceptViewModel(
    private val repository: InterceptXRepository,
    private val interceptQueue: InterceptQueue,
    private val proxyEngine: ProxyEngine
) : ViewModel() {

    private val projectId = 1L

    val interceptedQueue: StateFlow<List<HttpTransaction>> =
        repository.observeIntercepted(projectId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Reflects the engine's real state — flipping this switch actually pauses traffic. */
    val interceptOn: StateFlow<Boolean> = proxyEngine.interceptEnabledState

    fun setInterceptOn(enabled: Boolean) {
        proxyEngine.interceptEnabled = enabled
        if (!enabled) interceptQueue.resetForwardAll()
    }

    fun forward(transaction: HttpTransaction, editedHeaders: Map<String, String>, editedBody: String?) {
        interceptQueue.resolve(
            transaction.id,
            InterceptDecision.Forward(
                PendingRequest(transaction.id, transaction.method, transaction.url, editedHeaders.toMutableMap(), editedBody)
            )
        )
        viewModelScope.launch {
            repository.updateTransaction(
                transaction.copy(state = com.interceptx.data.model.TransactionState.FORWARDED)
            )
        }
    }

    fun drop(transaction: HttpTransaction) {
        interceptQueue.resolve(transaction.id, InterceptDecision.Drop)
        viewModelScope.launch {
            repository.updateTransaction(
                transaction.copy(state = com.interceptx.data.model.TransactionState.DROPPED)
            )
        }
    }

    fun forwardAll() {
        interceptQueue.forwardAll()
    }
}
