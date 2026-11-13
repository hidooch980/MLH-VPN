package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.SubscriptionItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SubscriptionRefreshPolicyTest {
    @Test fun deletedDisabledManualAndEmptySubscriptionsNeverSchedule() {
        assertFalse(SubscriptionRefreshPolicy.shouldSchedule(null))
        for (enabled in listOf(true, false)) {
            for (automatic in listOf(true, false)) {
                for (url in listOf("", "   ", "https://example.org/sub")) {
                    val item = SubscriptionItem(enabled = enabled, autoUpdate = automatic, url = url)
                    assertEquals(enabled && automatic && url.isNotBlank(), SubscriptionRefreshPolicy.shouldSchedule(item))
                }
            }
        }
    }

    @Test fun checkingEligibilityDoesNotFalsifySuccessTimestamp() {
        val item = SubscriptionItem(enabled = true, autoUpdate = true, url = "https://example.org/sub", lastUpdated = 123L)
        assertTrue(SubscriptionRefreshPolicy.shouldSchedule(item))
        assertEquals(123L, item.lastUpdated)
    }
}
