package com.v2ray.ang.core

data class LocalNetworkContext(
    val carrierCode: String?,
    /** One-way application hash only; never persist the Wi-Fi SSID or BSSID. */
    val wifiIdentityHash: String?,
    val hourBucket: Int,
)

data class LocalQualityObservation(
    val serverId: String,
    val context: LocalNetworkContext,
    val successful: Boolean,
    val score: Int,
    val recordedAtMillis: Long,
)

data class DeviceActivityState(
    val batterySaver: Boolean = false,
    val callActive: Boolean = false,
    val gameActive: Boolean = false,
    val mediaPlaybackActive: Boolean = false,
)

object LocalSelectionPolicy {
    fun shouldRunBackgroundProbe(state: DeviceActivityState): Boolean =
        !state.batterySaver && !state.callActive && !state.gameActive && !state.mediaPlaybackActive

    fun learnedScoreAdjustment(
        serverId: String,
        context: LocalNetworkContext,
        observations: List<LocalQualityObservation>,
        nowMillis: Long,
    ): Int {
        val maximumAge = 14L * 24 * 60 * 60 * 1000
        val relevant = observations.filter {
            it.serverId == serverId &&
                nowMillis - it.recordedAtMillis in 0..maximumAge &&
                it.context.carrierCode == context.carrierCode &&
                it.context.wifiIdentityHash == context.wifiIdentityHash &&
                circularHourDistance(it.context.hourBucket, context.hourBucket) <= 1
        }
        if (relevant.isEmpty()) return 0
        val successRate = relevant.count { it.successful } * 100 / relevant.size
        val averageScore = relevant.sumOf { it.score } / relevant.size
        return ((successRate - 50) / 10 + (averageScore - 50) / 10).coerceIn(-10, 10)
    }

    private fun circularHourDistance(first: Int, second: Int): Int {
        val direct = kotlin.math.abs(first.coerceIn(0, 23) - second.coerceIn(0, 23))
        return minOf(direct, 24 - direct)
    }
}
