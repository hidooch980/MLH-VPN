package com.v2ray.ang.core

import com.v2ray.ang.handler.MmkvManager
import java.security.MessageDigest

object SmartSelectionStore {
    private const val FAVORITES = "mlh_smart_favorites"
    private const val LAST_SUCCESSFUL = "mlh_last_successful_server"
    private const val COUNTRY_PREFIX = "mlh_verified_country_"
    private const val SCORE_PREFIX = "mlh_quality_score_"

    fun favorites(): Set<String> = MmkvManager.decodeSettingsStringSet(FAVORITES)?.toSet().orEmpty()
    fun isFavorite(serverId: String?): Boolean = serverId != null && serverId in favorites()

    fun toggleFavorite(serverId: String): Boolean {
        val values = favorites().toMutableSet()
        val favorite = if (serverId in values) { values.remove(serverId); false }
            else { values.add(serverId); true }
        MmkvManager.encodeSettings(FAVORITES, values)
        return favorite
    }

    fun recordSuccessfulConnection(serverId: String, verifiedCountryCode: String?, score: Int?) {
        MmkvManager.encodeSettings(LAST_SUCCESSFUL, serverId)
        val suffix = keySuffix(serverId)
        verifiedCountryCode?.takeIf { it.matches(Regex("[A-Za-z]{2}")) }?.let {
            MmkvManager.encodeSettings(COUNTRY_PREFIX + suffix, it.uppercase())
        }
        score?.coerceIn(0, 100)?.let { MmkvManager.encodeSettings(SCORE_PREFIX + suffix, it) }
    }

    fun lastSuccessfulServer(): String? = MmkvManager.decodeSettingsString(LAST_SUCCESSFUL)
    fun verifiedCountry(serverId: String): String? = MmkvManager.decodeSettingsString(COUNTRY_PREFIX + keySuffix(serverId))
    fun qualityScore(serverId: String): Int? =
        MmkvManager.decodeSettingsInt(SCORE_PREFIX + keySuffix(serverId), -1).takeIf { it >= 0 }

    private fun keySuffix(serverId: String): String = MessageDigest.getInstance("SHA-256")
        .digest(serverId.toByteArray(Charsets.UTF_8)).take(12)
        .joinToString("") { "%02x".format(it) }
}
