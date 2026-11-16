package com.v2ray.ang.core

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class EngineAdapterExceptionTest {
    @Test
    fun `failure identifies engine and operation without exposing cause text`() {
        val secretLikeCause = IllegalStateException("token=do-not-log config=private-server")
        val failure = EngineAdapterException(
            engineId = "xray-compatible",
            operation = EngineOperation.START,
            cause = secretLikeCause,
        )

        assertEquals("xray-compatible", failure.engineId)
        assertEquals(EngineOperation.START, failure.operation)
        assertSame(secretLikeCause, failure.cause)
        assertFalse(failure.message.orEmpty().contains("do-not-log"))
        assertFalse(failure.message.orEmpty().contains("private-server"))
    }
}
