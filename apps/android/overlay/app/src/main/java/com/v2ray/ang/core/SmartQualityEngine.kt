package com.v2ray.ang.core

import kotlin.math.abs
import kotlin.math.roundToLong

enum class QualityGrade { EXCELLENT, GOOD, FAIR, POOR, UNAVAILABLE }

data class ProbeEvidence(
    val serverId: String,
    val attempted: Int,
    val latencyMillis: List<Long>,
    val transferredBytes: Long = 0,
    val transferDurationMillis: Long = 0,
    val verifiedCountryCode: String? = null,
    val consecutiveFailures: Int = 0,
)

data class QualityAssessment(
    val serverId: String,
    val reachable: Boolean,
    val medianLatencyMillis: Long?,
    val jitterMillis: Long?,
    val failurePercent: Int,
    val throughputBytesPerSecond: Long?,
    val verifiedCountryCode: String?,
    val score: Int,
    val grade: QualityGrade,
    val quarantined: Boolean,
)

data class SelectionDecision(
    val selected: QualityAssessment?,
    val shouldSwitch: Boolean,
    val reason: String,
)

object SmartQualityEngine {
    const val QUARANTINE_FAILURES = 3
    private const val MIN_SWITCH_IMPROVEMENT = 25
    private const val MAX_PROBE_LATENCY_MILLIS = 120_000L

    fun assess(evidence: ProbeEvidence): QualityAssessment {
        // Reject negative and implausibly large samples; count them as failed attempts.
        // A bounded input also keeps median/jitter arithmetic overflow-safe.
        val successful = evidence.latencyMillis
            .filter { it in 0..MAX_PROBE_LATENCY_MILLIS }
            .sorted()
        val attempted = evidence.attempted.coerceAtLeast(evidence.latencyMillis.size).coerceAtLeast(1)
        val failures = (attempted - successful.size).coerceAtLeast(0)
        val failurePercent = ((failures.toLong() * 100L) / attempted.toLong()).toInt()
        val median = successful.medianOrNull()
        val jitter = median?.let { center ->
            successful.map { abs(it - center) }.average().roundToLong()
        }
        val throughput = if (evidence.transferredBytes > 0 && evidence.transferDurationMillis > 0) {
            ((evidence.transferredBytes.toDouble() * 1000.0) /
                evidence.transferDurationMillis.toDouble())
                .coerceAtMost(Long.MAX_VALUE.toDouble()).toLong()
        } else null
        val quarantined = evidence.consecutiveFailures >= QUARANTINE_FAILURES
        val reachable = median != null && !quarantined
        val score = if (!reachable) 0 else {
            val latencyPoints = (100 - median.coerceAtMost(500) / 5).toInt()
            val jitterPenalty = (jitter ?: 0).coerceAtMost(100).toInt()
            val throughputBonus = when {
                throughput == null -> 0
                throughput >= 2_000_000 -> 10
                throughput >= 500_000 -> 5
                else -> 0
            }
            (latencyPoints - jitterPenalty - failurePercent + throughputBonus).coerceIn(1, 100)
        }
        val grade = when {
            !reachable -> QualityGrade.UNAVAILABLE
            score >= 85 -> QualityGrade.EXCELLENT
            score >= 65 -> QualityGrade.GOOD
            score >= 40 -> QualityGrade.FAIR
            else -> QualityGrade.POOR
        }
        return QualityAssessment(
            evidence.serverId, reachable, median, jitter, failurePercent, throughput,
            evidence.verifiedCountryCode?.takeIf { it.matches(Regex("[A-Za-z]{2}")) }
                ?.uppercase(), score, grade, quarantined,
        )
    }

    fun selectBest(candidates: List<QualityAssessment>, currentServerId: String?): SelectionDecision {
        val available = candidates.filter { it.reachable && !it.quarantined }
        val best = available.maxWithOrNull(compareBy<QualityAssessment> { it.score }
            .thenBy { -(it.medianLatencyMillis ?: Long.MAX_VALUE) })
            ?: return SelectionDecision(null, currentServerId != null, "no_reachable_server")
        val current = candidates.firstOrNull { it.serverId == currentServerId }
        if (current == null) return SelectionDecision(best, true, "no_current_server")
        if (!current.reachable || current.quarantined || current.grade == QualityGrade.POOR) {
            return SelectionDecision(best, best.serverId != current.serverId, "current_unhealthy")
        }
        val improvement = best.score - current.score
        return if (best.serverId != current.serverId && improvement >= MIN_SWITCH_IMPROVEMENT) {
            SelectionDecision(best, true, "material_quality_gain")
        } else {
            SelectionDecision(current, false, "keep_stable_connection")
        }
    }
}

private fun List<Long>.medianOrNull(): Long? {
    if (isEmpty()) return null
    val middle = size / 2
    return if (size % 2 == 1) this[middle] else (this[middle - 1] + this[middle]) / 2
}
