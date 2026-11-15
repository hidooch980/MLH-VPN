package com.v2ray.ang.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LocalSelectionPolicyTest {
    @Test fun `background probes stop for battery call game and media activity`() {
        assertTrue(LocalSelectionPolicy.shouldRunBackgroundProbe(DeviceActivityState()))
        assertFalse(LocalSelectionPolicy.shouldRunBackgroundProbe(DeviceActivityState(batterySaver = true)))
        assertFalse(LocalSelectionPolicy.shouldRunBackgroundProbe(DeviceActivityState(callActive = true)))
        assertFalse(LocalSelectionPolicy.shouldRunBackgroundProbe(DeviceActivityState(gameActive = true)))
        assertFalse(LocalSelectionPolicy.shouldRunBackgroundProbe(DeviceActivityState(mediaPlaybackActive = true)))
    }

    @Test fun `learning is local to hashed network carrier and nearby hour`() {
        val now = 1_000_000_000L
        val context = LocalNetworkContext("43211", "sha256:wifi", 23)
        val history = listOf(
            LocalQualityObservation("a", LocalNetworkContext("43211", "sha256:wifi", 0), true, 90, now - 1_000),
            LocalQualityObservation("a", LocalNetworkContext("other", "sha256:wifi", 23), false, 1, now - 1_000),
            LocalQualityObservation("b", context, false, 1, now - 1_000),
        )
        assertTrue(LocalSelectionPolicy.learnedScoreAdjustment("a", context, history, now) > 0)
        assertEquals(0, LocalSelectionPolicy.learnedScoreAdjustment("missing", context, history, now))
    }
}
