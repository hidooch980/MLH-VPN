package com.v2ray.ang.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SmartQualityEngineTest {
    @Test fun `assessment uses median jitter failures throughput and verified country`() {
        val result = SmartQualityEngine.assess(ProbeEvidence(
            serverId = "de-1", attempted = 4, latencyMillis = listOf(40, 45, 50),
            transferredBytes = 2_000_000, transferDurationMillis = 1_000,
            verifiedCountryCode = "de",
        ))
        assertEquals(45, result.medianLatencyMillis)
        assertEquals(3, result.jitterMillis)
        assertEquals(25, result.failurePercent)
        assertEquals(2_000_000, result.throughputBytesPerSecond)
        assertEquals("DE", result.verifiedCountryCode)
        assertTrue(result.reachable)
    }

    @Test fun `three consecutive failures quarantine a server`() {
        val result = SmartQualityEngine.assess(ProbeEvidence("bad", 3, emptyList(), consecutiveFailures = 3))
        assertTrue(result.quarantined)
        assertEquals(QualityGrade.UNAVAILABLE, result.grade)
    }

    @Test fun `small score difference does not interrupt a healthy connection`() {
        val current = SmartQualityEngine.assess(ProbeEvidence("current", 3, listOf(45, 50, 55)))
        val candidate = SmartQualityEngine.assess(ProbeEvidence("candidate", 3, listOf(35, 40, 45)))
        val decision = SmartQualityEngine.selectBest(listOf(current, candidate), "current")
        assertFalse(decision.shouldSwitch)
        assertEquals("current", decision.selected?.serverId)
    }

    @Test fun `invalid latency samples count as failed probes and do not poison scoring`() {
        val result = SmartQualityEngine.assess(ProbeEvidence(
            serverId = "sanitized", attempted = 3,
            latencyMillis = listOf(-1, 45, 999_999),
            verifiedCountryCode = "not-a-country",
        ))
        assertEquals(45, result.medianLatencyMillis)
        assertEquals(66, result.failurePercent)
        assertNull(result.verifiedCountryCode)
    }

    @Test fun `throughput calculation saturates instead of overflowing`() {
        val result = SmartQualityEngine.assess(ProbeEvidence(
            serverId = "large", attempted = 1, latencyMillis = listOf(20),
            transferredBytes = Long.MAX_VALUE, transferDurationMillis = 1,
        ))
        assertEquals(Long.MAX_VALUE, result.throughputBytesPerSecond)
    }

    @Test fun `unhealthy connection switches to best reachable candidate`() {
        val current = SmartQualityEngine.assess(ProbeEvidence("current", 3, emptyList(), consecutiveFailures = 3))
        val candidate = SmartQualityEngine.assess(ProbeEvidence("candidate", 3, listOf(60, 65, 70)))
        val decision = SmartQualityEngine.selectBest(listOf(current, candidate), "current")
        assertTrue(decision.shouldSwitch)
        assertEquals("candidate", decision.selected?.serverId)
        assertEquals("current_unhealthy", decision.reason)
    }
}
