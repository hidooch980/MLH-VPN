package com.v2ray.ang.core

/**
 * Evidence-based protocol catalogue.
 *
 * A protocol being listed here does not mean a server is bundled or that a
 * connection has passed real-device acceptance. Keep these states explicit.
 */
enum class MolidoProtocol(val label: String) {
    VLESS("VLESS"),
    VMESS("VMess"),
    TROJAN("Trojan"),
    SHADOWSOCKS("Shadowsocks"),
    SOCKS("SOCKS"),
    HTTP("HTTP proxy"),
    WIREGUARD("WireGuard"),
    HYSTERIA2("Hysteria 2"),
    TOR("Tor"),
    OBFS4("obfs4"),
    SING_BOX("sing-box engine"),
}

enum class ProtocolEvidence {
    /** Listed by the Android source documentation for the pinned upstream client. */
    SOURCE_DOCUMENTED,
    /** A matching native library was observed in the user-supplied APK only. */
    APK_LIBRARY_OBSERVED,
}

data class MolidoProtocolCapability(
    val protocol: MolidoProtocol,
    val evidence: ProtocolEvidence,
    val canBeOfferedInClient: Boolean,
    /** Always false until a reproducible physical-device acceptance test is recorded. */
    val deviceVerified: Boolean = false,
    val evidenceNote: String,
)

object MolidoProtocolRegistry {
    val all: List<MolidoProtocolCapability> = listOf(
        documented(MolidoProtocol.VLESS),
        documented(MolidoProtocol.VMESS),
        documented(MolidoProtocol.TROJAN),
        documented(MolidoProtocol.SHADOWSOCKS),
        documented(MolidoProtocol.SOCKS),
        documented(MolidoProtocol.HTTP),
        documented(MolidoProtocol.WIREGUARD),
        documented(MolidoProtocol.HYSTERIA2),
        apkOnly(MolidoProtocol.TOR, "libtor.so was observed in the supplied APK; runtime wiring is unverified."),
        apkOnly(MolidoProtocol.OBFS4, "libobfs4proxy.so was observed in the supplied APK; runtime wiring is unverified."),
        apkOnly(MolidoProtocol.SING_BOX, "libsingbox.so was observed in the supplied APK; runtime wiring is unverified."),
    )

    fun find(protocol: MolidoProtocol): MolidoProtocolCapability? =
        all.firstOrNull { it.protocol == protocol }

    /** Only source-documented protocols may appear in the initial client catalogue. */
    fun initialClientCatalogue(): List<MolidoProtocolCapability> =
        all.filter { it.canBeOfferedInClient }

    private fun documented(protocol: MolidoProtocol) = MolidoProtocolCapability(
        protocol = protocol,
        evidence = ProtocolEvidence.SOURCE_DOCUMENTED,
        canBeOfferedInClient = true,
        evidenceNote = "Documented by apps/android/README.md; physical-device verification is still required.",
    )

    private fun apkOnly(protocol: MolidoProtocol, note: String) = MolidoProtocolCapability(
        protocol = protocol,
        evidence = ProtocolEvidence.APK_LIBRARY_OBSERVED,
        canBeOfferedInClient = false,
        evidenceNote = note,
    )
}
