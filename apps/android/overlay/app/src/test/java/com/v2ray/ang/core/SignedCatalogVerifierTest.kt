package com.v2ray.ang.core

import java.security.KeyPairGenerator
import java.security.Signature
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SignedCatalogVerifierTest {
    @Test fun `valid signature passes and modified catalog fails`() {
        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val catalog = "{\"version\":1,\"services\":[]}".toByteArray()
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(keyPair.private)
            update(catalog)
            sign()
        }
        assertTrue(SignedCatalogVerifier.verify(catalog, signature, keyPair.public))
        assertFalse(SignedCatalogVerifier.verify(catalog + 0, signature, keyPair.public))
        assertFalse(SignedCatalogVerifier.verify(catalog, byteArrayOf(1, 2, 3), keyPair.public))
    }
}
