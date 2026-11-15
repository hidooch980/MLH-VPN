package com.v2ray.ang.core

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class MolidoProtocolRegistryTest {
    @Test fun `initial catalogue contains only source documented protocols`() {
        val catalogue = MolidoProtocolRegistry.initialClientCatalogue()
        assertTrue(catalogue.isNotEmpty())
        assertTrue(catalogue.all { it.evidence == ProtocolEvidence.SOURCE_DOCUMENTED })
        assertFalse(catalogue.any { it.protocol == MolidoProtocol.TOR })
        assertFalse(catalogue.any { it.protocol == MolidoProtocol.OBFS4 })
        assertFalse(catalogue.any { it.protocol == MolidoProtocol.SING_BOX })
    }

    @Test fun `packaged APK libraries are not silently promoted to working protocols`() {
        listOf(MolidoProtocol.TOR, MolidoProtocol.OBFS4, MolidoProtocol.SING_BOX).forEach { id ->
            val capability = MolidoProtocolRegistry.find(id)
            assertNotNull(capability)
            assertEquals(ProtocolEvidence.APK_LIBRARY_OBSERVED, capability?.evidence)
            assertFalse(capability!!.canBeOfferedInClient)
            assertFalse(capability.deviceVerified)
        }
    }

    @Test fun `documented protocols are not marked device verified by default`() {
        assertTrue(MolidoProtocolRegistry.initialClientCatalogue().all { !it.deviceVerified })
    }
}
