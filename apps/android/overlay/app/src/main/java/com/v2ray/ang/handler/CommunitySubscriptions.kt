// SPDX-License-Identifier: GPL-3.0-only
package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.SubscriptionItem

/** Owns only the one-time public-source catalog; MMKV and scheduling retain their existing owners. */
internal object CommunitySubscriptions {
    const val INSTALLED_KEY = "mlh_community_sources_installed_v1"
    data class Source(val id: String, val name: String, val url: String)

    val sources = listOf(
        Source(
            "mlh-community-solvpn",
            "SolVPN · Community",
            "https://raw.githubusercontent.com/SoliSpirit/SolVPN/main/Subscribes/sub1.txt"
        ),
        Source(
            "mlh-community-radikal",
            "Radikal · Community",
            "https://raw.githubusercontent.com/0xRadikal/Free-v2ray-Configs/main/verified/configs_base64.txt"
        )
    )

    interface Store {
        fun isInstalled(): Boolean
        fun current(): Map<String, SubscriptionItem>
        fun put(id: String, item: SubscriptionItem)
        fun remove(id: String)
        fun markInstalled()
    }

    /** Runs on the caller's IO dispatcher. Never re-adds a source after user deletion. */
    @Synchronized
    fun installOnce(store: Store): Boolean {
        if (store.isInstalled()) return false
        val existing = store.current()
        val added = mutableListOf<String>()
        try {
            sources.forEach { source ->
                if (source.id !in existing && existing.values.none { it.url == source.url }) {
                    // Record first so even a partially completed storage write is rolled back.
                    added += source.id
                    store.put(source.id, SubscriptionItem(
                        remarks = source.name,
                        url = source.url,
                        enabled = true,
                        autoUpdate = true,
                        updateInterval = 60,
                        allowInsecureUrl = false
                    ))
                }
            }
            store.markInstalled()
        } catch (error: Exception) {
            added.asReversed().forEach { id ->
                try { store.remove(id) } catch (cleanup: Exception) { error.addSuppressed(cleanup) }
            }
            throw error
        }
        return added.isNotEmpty()
    }
}
