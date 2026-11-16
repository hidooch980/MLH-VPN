package com.v2ray.ang.core

enum class EnginePreference { AUTO, FAST, STABLE, COMPATIBLE }

data class EngineCandidate(
    val quality: QualityAssessment,
    val protocol: MolidoProtocol,
    val favorite: Boolean = false,
    val countryCode: String? = null,
    val learnedAdjustment: Int = 0,
)

data class EngineSelection(
    val candidate: EngineCandidate?,
    val reason: String,
    val shouldSwitch: Boolean,
    val effectiveScore: Int?,
)

/**
 * Local-only coordinator. It ranks measured, reachable profiles; it never discovers
 * endpoints, fabricates a server, or changes a live tunnel itself.
 */
object SmartEngineOrchestrator {
    private const val MIN_SWITCH_GAIN = 12
    private const val FAVORITE_BONUS = 2
    private const val COUNTRY_BONUS = 3

    fun select(
        candidates: List<EngineCandidate>,
        currentServerId: String?,
        preference: EnginePreference = EnginePreference.AUTO,
        manuallySelectedServerId: String? = null,
        preferredCountryCode: String? = null,
    ): EngineSelection {
        val reachable = candidates.filter {
            it.quality.reachable && !it.quality.quarantined &&
                it.quality.medianLatencyMillis != null &&
                it.quality.score in 1..100 &&
                it.quality.serverId.isNotBlank() &&
                supports(preference, it.protocol)
        }
        if (manuallySelectedServerId != null) {
            val manual = reachable.firstOrNull { it.quality.serverId == manuallySelectedServerId }
            return if (manual != null) {
                EngineSelection(manual, "manual_selection", manual.quality.serverId != currentServerId,
                    effectiveScore(manual, preferredCountryCode))
            } else {
                EngineSelection(null, "manual_profile_unavailable_or_incompatible", currentServerId != null, null)
            }
        }
        if (reachable.isEmpty()) {
            return EngineSelection(null, "no_compatible_reachable_profile", currentServerId != null, null)
        }

        val ranked = reachable.sortedWith(
            compareByDescending<EngineCandidate> { effectiveScore(it, preferredCountryCode) }
                .thenBy { it.quality.medianLatencyMillis ?: Long.MAX_VALUE }
                .thenBy { it.quality.serverId }
        )
        val best = ranked.first()
        val current = ranked.firstOrNull { it.quality.serverId == currentServerId }
        if (current == null) {
            return EngineSelection(best, "best_available_profile", best.quality.serverId != currentServerId,
                effectiveScore(best, preferredCountryCode))
        }
        val gain = effectiveScore(best, preferredCountryCode) - effectiveScore(current, preferredCountryCode)
        return if (best.quality.serverId != current.quality.serverId && gain >= MIN_SWITCH_GAIN) {
            EngineSelection(best, "material_quality_improvement", true, effectiveScore(best, preferredCountryCode))
        } else {
            EngineSelection(current, "keep_current_connection_stable", false,
                effectiveScore(current, preferredCountryCode))
        }
    }

    private fun effectiveScore(candidate: EngineCandidate, preferredCountryCode: String?): Int {
        val countryBonus = if (
            preferredCountryCode != null &&
            candidate.countryCode?.equals(preferredCountryCode, ignoreCase = true) == true
        ) COUNTRY_BONUS else 0
        return (candidate.quality.score + candidate.learnedAdjustment.coerceIn(-10, 10) +
            (if (candidate.favorite) FAVORITE_BONUS else 0) + countryBonus).coerceIn(0, 100)
    }

    private fun supports(preference: EnginePreference, protocol: MolidoProtocol): Boolean {
        val type = protocol.name
        return when (preference) {
            EnginePreference.AUTO -> true
            EnginePreference.FAST -> protocol in setOf(
                MolidoProtocol.WIREGUARD, MolidoProtocol.HYSTERIA2
            )
            EnginePreference.STABLE -> protocol in setOf(
                MolidoProtocol.VLESS, MolidoProtocol.VMESS, MolidoProtocol.TROJAN,
                MolidoProtocol.WIREGUARD
            )
            EnginePreference.COMPATIBLE -> protocol in setOf(
                MolidoProtocol.SHADOWSOCKS, MolidoProtocol.TROJAN,
                MolidoProtocol.HTTP, MolidoProtocol.SOCKS
            )
        }
    }
}
