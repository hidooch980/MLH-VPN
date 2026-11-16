package com.v2ray.ang.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SmartEngineOrchestratorTest {
    private fun candidate(
        id: String,
        protocol: MolidoProtocol = MolidoProtocol.VLESS,
        score: Int,
        latency: Long = 50,
        favorite: Boolean = false,
        country: String? = null,
        adjustment: Int = 0,
    ) = EngineCandidate(
        QualityAssessment(id, true, latency, 2, 0, null, country, score,
            if (score >= 85) QualityGrade.EXCELLENT else QualityGrade.GOOD, false),
        protocol, favorite, country, adjustment
    )

    @Test fun `keeps current server when quality gain is too small`() {
        val result = SmartEngineOrchestrator.select(
            listOf(candidate("current", score = 70), candidate("other", score = 80)),
            currentServerId = "current"
        )
        assertEquals("current", result.candidate?.quality?.serverId)
        assertFalse(result.shouldSwitch)
    }

    @Test fun `switches when a materially better profile is available`() {
        val result = SmartEngineOrchestrator.select(
            listOf(candidate("current", score = 50), candidate("better", score = 90)),
            currentServerId = "current"
        )
        assertEquals("better", result.candidate?.quality?.serverId)
        assertTrue(result.shouldSwitch)
    }

    @Test fun `manual selection is honored only for reachable compatible profile`() {
        val options = listOf(
            candidate("wg", MolidoProtocol.WIREGUARD, 70),
            candidate("vless", MolidoProtocol.VLESS, 90)
        )
        val valid = SmartEngineOrchestrator.select(options, "vless",
            manuallySelectedServerId = "wg", preference = EnginePreference.FAST)
        assertEquals("wg", valid.candidate?.quality?.serverId)
        val invalid = SmartEngineOrchestrator.select(options, "vless",
            manuallySelectedServerId = "vless", preference = EnginePreference.FAST)
        assertNull(invalid.candidate)
        assertEquals("manual_profile_unavailable_or_incompatible", invalid.reason)
    }

    @Test fun `quarantined and unreachable profiles are never selected`() {
        val broken = candidate("broken", score = 99).copy(
            quality = candidate("broken", score = 99).quality.copy(reachable = false, quarantined = true)
        )
        val result = SmartEngineOrchestrator.select(listOf(broken), null)
        assertNull(result.candidate)
    }

    @Test fun `local learning and preference are bounded ranking signals`() {
        val result = SmartEngineOrchestrator.select(
            listOf(candidate("a", score = 70), candidate("b", score = 65,
                favorite = true, country = "DE", adjustment = 10)),
            currentServerId = null, preferredCountryCode = "DE"
        )
        assertEquals("b", result.candidate?.quality?.serverId)
        assertTrue(result.effectiveScore!! <= 100)
    }
}
