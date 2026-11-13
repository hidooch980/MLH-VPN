package com.v2ray.ang.handler

import com.v2ray.ang.dto.entities.SubscriptionItem
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class CommunitySubscriptionsTest {
    private class MemoryStore : CommunitySubscriptions.Store {
        var installed = false
        var legacyInstalled = false
        var failAt: String? = null
        val items = linkedMapOf<String, SubscriptionItem>()
        override fun isInstalled() = installed
        override fun isLegacyInstalled() = legacyInstalled
        override fun current(): Map<String, SubscriptionItem> = items.toMap()
        override fun put(id: String, item: SubscriptionItem) {
            items[id] = item
            if (failAt == id) error("storage failure")
        }
        override fun remove(id: String) { items.remove(id) }
        override fun markInstalled() {
            if (failAt == "marker") error("marker failure")
            installed = true
        }
    }

    @Test fun firstInstallSchedulesHourlyHttpsSources() {
        val store = MemoryStore()
        assertTrue(CommunitySubscriptions.installOnce(store))
        assertTrue(store.installed)
        assertEquals(CommunitySubscriptions.sources.size, store.items.size)
        store.items.values.forEach {
            assertTrue(it.url.startsWith("https://"))
            assertTrue(it.enabled && it.autoUpdate)
            assertEquals(60L, it.updateInterval)
            assertFalse(it.allowInsecureUrl)
            assertEquals(-1L, it.lastUpdated)
        }
    }

    @Test fun relaunchDoesNotRestoreDeletedSourcesOrOverwritePreferences() {
        val store = MemoryStore()
        CommunitySubscriptions.installOnce(store)
        val id = CommunitySubscriptions.sources.first().id
        store.items.remove(id)
        store.items.values.first().autoUpdate = false
        assertFalse(CommunitySubscriptions.installOnce(store))
        assertFalse(id in store.items)
        assertFalse(store.items.values.first().autoUpdate)
    }

    @Test fun existingSameUrlKeepsItsGuidNameAndUpdatePreference() {
        val store = MemoryStore()
        val custom = SubscriptionItem(remarks = "My source", url = CommunitySubscriptions.sources.first().url)
        store.items["user-guid"] = custom
        CommunitySubscriptions.installOnce(store)
        assertSame(custom, store.items["user-guid"])
        assertEquals(CommunitySubscriptions.sources.size, store.items.size)
        assertFalse(custom.autoUpdate)
    }

    @Test fun existingStableIdIsNeverOverwritten() {
        val store = MemoryStore()
        val id = CommunitySubscriptions.sources.first().id
        val custom = SubscriptionItem(url = "https://example.org/my-sub")
        store.items[id] = custom
        CommunitySubscriptions.installOnce(store)
        assertSame(custom, store.items[id])
    }

    @Test fun partialWriteOrMarkerFailureRollsBackOnlyNewSourcesAndCanRetry() {
        for (failure in listOf(CommunitySubscriptions.sources.last().id, "marker")) {
            val store = MemoryStore()
            val existing = SubscriptionItem(url = "https://example.org/existing")
            store.items["user"] = existing
            store.failAt = failure
            assertThrows(IllegalStateException::class.java) { CommunitySubscriptions.installOnce(store) }
            assertEquals(mapOf("user" to existing), store.items)
            assertFalse(store.installed)
            store.failAt = null
            assertTrue(CommunitySubscriptions.installOnce(store))
        }
    }

    @Test fun interruptedPreviousInstallDoesNotDuplicateStableSource() {
        val store = MemoryStore()
        val source = CommunitySubscriptions.sources.first()
        val partial = SubscriptionItem(url = source.url)
        store.items[source.id] = partial
        CommunitySubscriptions.installOnce(store)
        assertEquals(CommunitySubscriptions.sources.size, store.items.size)
        assertSame(partial, store.items[source.id])
    }
    @Test fun upgradeAddsNewSourcesWithoutRestoringDeletedV1OrOverwritingPreferences() {
        val store = MemoryStore()
        store.legacyInstalled = true
        val kept = CommunitySubscriptions.sources.first()
        val custom = SubscriptionItem(url = kept.url, remarks = "My settings", autoUpdate = false)
        store.items[kept.id] = custom
        assertTrue(CommunitySubscriptions.installOnce(store))
        assertSame(custom, store.items[kept.id])
        assertFalse(custom.autoUpdate)
        assertFalse(CommunitySubscriptions.sources[1].id in store.items)
        assertEquals(3, store.items.size)
        val newId = CommunitySubscriptions.sources.last().id
        store.items.remove(newId)
        assertFalse(CommunitySubscriptions.installOnce(store))
        assertFalse(newId in store.items)
    }

    @Test fun upgradeFailureKeepsLegacyPreferencesAndRetriesOnlyNewCatalog() {
        val store = MemoryStore()
        store.legacyInstalled = true
        store.failAt = "marker"
        assertThrows(IllegalStateException::class.java) { CommunitySubscriptions.installOnce(store) }
        assertTrue(store.items.isEmpty())
        assertTrue(store.legacyInstalled)
        store.failAt = null
        assertTrue(CommunitySubscriptions.installOnce(store))
        assertEquals(CommunitySubscriptions.sources.filter { it.catalogVersion == 2 }.map { it.id }.toSet(), store.items.keys)
    }
}
