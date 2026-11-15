package com.v2ray.ang.core

import com.v2ray.ang.ui.main.ConnectionMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionModePolicyTest {
    @Test fun fastModeAcceptsOnlyLowOverheadProfiles() {
        assertTrue(ConnectionModePolicy.supports(ConnectionMode.FAST, "HYSTERIA2"))
        assertTrue(ConnectionModePolicy.supports(ConnectionMode.FAST, "WIREGUARD"))
        assertFalse(ConnectionModePolicy.supports(ConnectionMode.FAST, "HTTP"))
    }

    @Test fun compatibleModeRejectsUdpOnlyProfiles() {
        assertTrue(ConnectionModePolicy.supports(ConnectionMode.COMPATIBLE, "TROJAN"))
        assertFalse(ConnectionModePolicy.supports(ConnectionMode.COMPATIBLE, "WIREGUARD"))
        assertFalse(ConnectionModePolicy.supports(ConnectionMode.COMPATIBLE, "HYSTERIA2"))
    }

    @Test fun unmeasuredProfileNeverBeatsMeasuredProfile() {
        val measured = ConnectionModePolicy.rank(ConnectionMode.STABLE, "TROJAN", 120)
        val unknown = ConnectionModePolicy.rank(ConnectionMode.STABLE, "WIREGUARD", 0)
        assertTrue(measured < unknown)
        assertEquals(Long.MAX_VALUE, ConnectionModePolicy.rank(ConnectionMode.FAST, "HTTP", 1))
    }
}
