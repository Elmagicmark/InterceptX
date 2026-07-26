package com.interceptx.proxy

data class PendingRequest(
    val id: Long,
    var method: String,
    var url: String,
    var headers: MutableMap<String, String>,
    var body: String?
)

sealed class InterceptDecision {
    data class Forward(val edited: PendingRequest) : InterceptDecision()
    object Drop : InterceptDecision()
}

/**
 * Holds requests that are paused waiting for a human decision (Forward / Drop / edit)
 * from the Intercept screen. The proxy engine's worker thread blocks on [await] while
 * the Compose UI thread resolves it via [resolve] or [forwardAll].
 */
class InterceptQueue {
    private data class Waiting(val request: PendingRequest, val channel: java.util.concurrent.SynchronousQueue<InterceptDecision>)

    private val pending = java.util.concurrent.ConcurrentHashMap<Long, Waiting>()
    @Volatile private var forwardAllActive = false

    fun await(request: PendingRequest): InterceptDecision {
        if (forwardAllActive) return InterceptDecision.Forward(request)
        val channel = java.util.concurrent.SynchronousQueue<InterceptDecision>()
        pending[request.id] = Waiting(request, channel)
        return try {
            channel.take()
        } finally {
            pending.remove(request.id)
        }
    }

    fun resolve(id: Long, decision: InterceptDecision) {
        pending[id]?.channel?.offer(decision)
    }

    /** Releases every currently-queued request unedited, and flips future requests to pass straight through. */
    fun forwardAll() {
        forwardAllActive = true
        pending.values.forEach { it.channel.offer(InterceptDecision.Forward(it.request)) }
    }

    fun resetForwardAll() {
        forwardAllActive = false
    }
}
