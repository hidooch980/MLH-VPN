package com.v2ray.ang.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MlhNetworkBudgetTest {
    @Test fun importedAndInvalidConcurrencyCannotExhaustHundredsOfWorkers() {
        for ((input, expected) in listOf(null to 16, "invalid" to 16, "0" to 1, "-99" to 1,
            "1" to 1, "8" to 8, "64" to 64, "128" to 64, "2147483647" to 64)) {
            assertEquals(expected, SettingsManager.boundedRealPingConcurrency(input))
        }
    }
}
