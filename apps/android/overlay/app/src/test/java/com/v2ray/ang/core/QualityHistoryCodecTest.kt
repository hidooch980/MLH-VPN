package com.v2ray.ang.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class QualityHistoryCodecTest {
    @Test
    fun `round trip preserves only aggregate measurements`() {
        val samples = listOf(
            QualityHistorySample(1_800_000_000_000, 87, 42, 4, 0, 2_400_000),
            QualityHistorySample(1_800_000_060_000, 64, 130, 22, 8, null),
        )

        assertEquals(samples, QualityHistoryCodec.decode(QualityHistoryCodec.encode(samples)))
    }

    @Test
    fun `malformed and out of range samples are discarded`() {
        val encoded = listOf(
            "1800000000000,101,10,1,0,1000",
            "1800000000000,80,-1,-1,0,-1",
            "not-a-sample",
            "1800000000000,70,20,2,101,500",
        ).joinToString("\n")

        assertEquals(1, QualityHistoryCodec.decode(encoded).size)
        assertNull(QualityHistoryCodec.decode(encoded).single().medianLatencyMillis)
    }
}
