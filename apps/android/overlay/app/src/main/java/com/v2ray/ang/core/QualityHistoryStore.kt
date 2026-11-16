package com.v2ray.ang.core

import android.content.Context

/**
 * Stores only aggregate connection-quality measurements. No server IDs, IPs, endpoints,
 * configuration strings, DNS queries or traffic destinations are persisted.
 */
data class QualityHistorySample(
    val timestampMillis: Long,
    val score: Int,
    val medianLatencyMillis: Long?,
    val jitterMillis: Long?,
    val failurePercent: Int,
    val throughputBytesPerSecond: Long?,
)

class QualityHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    @Synchronized
    fun record(sample: QualityHistorySample) {
        if (!sample.isValid()) return
        val updated = (read() + sample).takeLast(MAX_SAMPLES)
        preferences.edit().putString(KEY, QualityHistoryCodec.encode(updated)).apply()
    }

    @Synchronized
    fun read(): List<QualityHistorySample> =
        QualityHistoryCodec.decode(preferences.getString(KEY, null).orEmpty()).takeLast(MAX_SAMPLES)

    companion object {
        private const val PREFERENCES = "molido_quality_history_v1"
        private const val KEY = "samples"
        const val MAX_SAMPLES = 100
    }
}

internal object QualityHistoryCodec {
    fun encode(samples: List<QualityHistorySample>): String = samples.joinToString("\n") { sample ->
        listOf(
            sample.timestampMillis,
            sample.score.toLong(),
            sample.medianLatencyMillis ?: -1L,
            sample.jitterMillis ?: -1L,
            sample.failurePercent.toLong(),
            sample.throughputBytesPerSecond ?: -1L,
        ).joinToString(",")
    }

    fun decode(value: String): List<QualityHistorySample> = value.lineSequence().mapNotNull { line ->
        val fields = line.split(",")
        if (fields.size != 6) return@mapNotNull null
        val values = fields.map { it.toLongOrNull() ?: return@mapNotNull null }
        val sample = QualityHistorySample(
            timestampMillis = values[0],
            score = values[1].toInt(),
            medianLatencyMillis = values[2].takeIf { it >= 0 },
            jitterMillis = values[3].takeIf { it >= 0 },
            failurePercent = values[4].toInt(),
            throughputBytesPerSecond = values[5].takeIf { it >= 0 },
        )
        sample.takeIf { it.isValid() }
    }.toList()

}

private fun QualityHistorySample.isValid(): Boolean =
    timestampMillis > 0 &&
        score in 0..100 &&
        (medianLatencyMillis == null || medianLatencyMillis >= 0) &&
        (jitterMillis == null || jitterMillis >= 0) &&
        failurePercent in 0..100 &&
        (throughputBytesPerSecond == null || throughputBytesPerSecond >= 0)
