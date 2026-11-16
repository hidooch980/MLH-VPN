package com.v2ray.ang.core

import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks latency observations made during this app process using Android's monotonic clock.
 * Persisted latency values are intentionally not considered fresh after process restart.
 */
class ProbeFreshness(private val maxAgeMillis: Long = DEFAULT_MAX_AGE_MILLIS) {
    private val observedAt = ConcurrentHashMap<String, Long>()

    init {
        require(maxAgeMillis > 0) { "maxAgeMillis must be positive" }
    }

    fun record(serverId: String, elapsedRealtimeMillis: Long) {
        if (serverId.isNotBlank() && elapsedRealtimeMillis >= 0) {
            observedAt[serverId] = elapsedRealtimeMillis
        }
    }

    fun isFresh(serverId: String, nowElapsedRealtimeMillis: Long): Boolean {
        val observed = observedAt[serverId] ?: return false
        val age = nowElapsedRealtimeMillis - observed
        return age in 0..maxAgeMillis
    }

    fun clear(serverId: String) {
        observedAt.remove(serverId)
    }

    companion object {
        const val DEFAULT_MAX_AGE_MILLIS = 5 * 60 * 1000L
    }
}
