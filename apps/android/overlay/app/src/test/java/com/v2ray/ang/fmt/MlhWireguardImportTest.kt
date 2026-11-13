package com.v2ray.ang.fmt

import org.junit.jupiter.api.Assertions.*
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
    @Test fun protonStyleCommentsAndIpv6EndpointAreAccepted() {
        val key = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        val text = "\uFEFF# Proton-style synthetic export\n" + """
            [Interface]
            # Account-specific key, never bundled in MLH
            PrivateKey = $key
            Address = 10.2.0.2/32
            DNS = 10.2.0.1
            [Peer]
            PublicKey = $key
            Endpoint = [2001:db8::1]:51820
            AllowedIPs = 0.0.0.0/0, ::/0
        """.trimIndent()
        assertTrue(WireguardFmt.isWireguardConfFile(text))
        val profile = WireguardFmt.parseWireguardConfFile(text)
        assertEquals("2001:db8::1", profile.server)
        assertEquals("51820", profile.serverPort)
        assertEquals("10.2.0.1", profile.remoteDNS)
        assertEquals(key, profile.secretKey)
    }

    @Test fun nonWireguardTextAndMissingPeerAreNotDetected() {
        assertFalse(WireguardFmt.isWireguardConfFile("# [Interface]\n[Peer]"))
        assertFalse(WireguardFmt.isWireguardConfFile("[Interface]\nAddress = 10.2.0.2/32"))
        assertFalse(WireguardFmt.isWireguardConfFile("some text\n[Interface]\n[Peer]"))
    }

    @Test fun invalidEndpointsFailBeforeProfileIsStored() {
        for (endpoint in listOf("", "host", "host:0", "host:65536", "[2001:db8::1]", "host:443/path")) {
            assertThrows(Exception::class.java) {
                WireguardFmt.parseWireguardConfFile("[Interface]\n[Peer]\nEndpoint = $endpoint")
            }
        }
    }
}
