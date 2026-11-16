package com.v2ray.ang.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProbeFreshnessTest {
    @Test fun persistedOrUnknownProbeIsNotFresh() {
        assertFalse(ProbeFreshness().isFresh("server-a", 10_000))
    }

    @Test fun recentProbeIsFresh() {
        val freshness = ProbeFreshness(1_000)
        freshness.record("server-a", 10_000)
        assertTrue(freshness.isFresh("server-a", 11_000))
    }

    @Test fun staleProbeIsRejected() {
        val freshness = ProbeFreshness(1_000)
        freshness.record("server-a", 10_000)
        assertFalse(freshness.isFresh("server-a", 11_001))
    }

    @Test fun futureTimestampIsRejected() {
        val freshness = ProbeFreshness(1_000)
        freshness.record("server-a", 10_001)
        assertFalse(freshness.isFresh("server-a", 10_000))
    }

    @Test fun blankServerIdsAreIgnored() {
        val freshness = ProbeFreshness()
        freshness.record(" ", 10_000)
        assertFalse(freshness.isFresh(" ", 10_000))
    }
}
