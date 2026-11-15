package com.v2ray.ang.core

import com.v2ray.ang.ui.main.ConnectionMode

/** Keeps technical protocol details out of UI while selecting only compatible profiles. */
object ConnectionModePolicy {
    fun supports(mode: ConnectionMode, configType: String): Boolean {
        val type = configType.uppercase()
        return when (mode) {
            ConnectionMode.AUTO -> true
            ConnectionMode.FAST -> type in setOf("WIREGUARD", "HYSTERIA", "HYSTERIA2")
            ConnectionMode.STABLE -> type in setOf("VLESS", "VMESS", "TROJAN", "WIREGUARD")
            ConnectionMode.COMPATIBLE -> type in setOf("SHADOWSOCKS", "TROJAN", "HTTP", "SOCKS")
        }
    }

    fun rank(mode: ConnectionMode, configType: String, delayMillis: Long): Long {
        if (!supports(mode, configType)) return Long.MAX_VALUE
        val measured = delayMillis.takeIf { it > 0 } ?: 60_000L
        val type = configType.uppercase()
        val preference = when (mode) {
            ConnectionMode.AUTO -> 0L
            ConnectionMode.FAST -> if (type.startsWith("HYSTERIA")) 0L else 500L
            ConnectionMode.STABLE -> if (type == "WIREGUARD") 0L else 250L
            ConnectionMode.COMPATIBLE -> if (type == "TROJAN") 0L else 250L
        }
        return measured + preference
    }
}
