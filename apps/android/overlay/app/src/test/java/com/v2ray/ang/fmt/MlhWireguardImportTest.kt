package com.v2ray.ang.fmt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MlhWireguardImportTest {
    @Test fun warpStyleProfilePreservesKeyAddressEndpointDnsAndReservedBytes() {
        // Synthetic fixture, never a live WARP credential.
        val key = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        val profile = WireguardFmt.parseWireguardConfFile("""
            [Interface]
            PrivateKey = $key
            Address = 172.16.0.2/32, 2606:4700:110:1234::2/128
            DNS = 1.1.1.1
            MTU = 1280
            [Peer]
            PublicKey = $key
            Endpoint = engage.cloudflareclient.com:2408
            AllowedIPs = 0.0.0.0/0, ::/0
            Reserved = 1,2,3
        """.trimIndent())
        assertEquals(key, profile.secretKey)
        assertEquals(key, profile.publicKey)
        assertEquals("engage.cloudflareclient.com", profile.server)
        assertEquals("2408", profile.serverPort)
        assertEquals("1.1.1.1", profile.remoteDNS)
        assertEquals("1,2,3", profile.reserved)
        assertEquals(1280, profile.mtu)
        assertEquals("172.16.0.2/32, 2606:4700:110:1234::2/128", profile.localAddress)
    }
}
