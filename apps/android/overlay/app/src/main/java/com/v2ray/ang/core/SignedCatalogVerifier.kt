package com.v2ray.ang.core

import java.security.PublicKey
import java.security.Signature

object SignedCatalogVerifier {
    private const val ALGORITHM = "SHA256withRSA"

    /** Verifies the exact downloaded bytes; callers must not parse or rewrite them first. */
    fun verify(catalogBytes: ByteArray, signatureBytes: ByteArray, publicKey: PublicKey): Boolean {
        return runCatching {
            Signature.getInstance(ALGORITHM).run {
                initVerify(publicKey)
                update(catalogBytes)
                verify(signatureBytes)
            }
        }.getOrDefault(false)
    }
}
