package com.v2ray.ang.ui.main

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MlhDashboardTest {
    @Test fun `timer handles zero negative and sessions exceeding a day`() {
        assertEquals("00:00:00", formatSessionTime(-1))
        assertEquals("01:01:01", formatSessionTime(3_661_000))
        assertEquals("25:00:00", formatSessionTime(90_000_000))
    }
    @Test fun `traffic units use binary thresholds`() {
        assertEquals("0 B", formatTraffic(0.0))
        assertEquals("1.0 KiB", formatTraffic(1024.0))
        assertEquals("1.0 MiB", formatTraffic(1048576.0))
    }
    @Test fun `latency score uses stable quality bands`() {
        assertEquals(0, latencyScore(-1))
        assertEquals(96, latencyScore(80))
        assertEquals(88, latencyScore(81))
        assertEquals(76, latencyScore(200))
        assertEquals(35, latencyScore(900))
    }
}
