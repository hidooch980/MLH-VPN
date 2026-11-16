package com.v2ray.ang.handler

import com.v2ray.ang.dto.GitHubRelease

/** APK selection is separate from networking so device fallbacks can be tested. */
internal object ReleaseAssetSelector {
    private val versionPattern = Regex("^(\\d+)\\.(\\d+)\\.(\\d+)(?:-([0-9A-Za-z.-]+))?$")
    internal val apkFilenamePattern = Regex("^MLHVPN_[0-9A-Za-z.-]+-fdroid_(arm64-v8a|armeabi-v7a|x86|x86_64|universal)\\.apk$")

    fun shouldCheck(now: Long, last: Long): Boolean = last <= 0 || now < last || now - last >= 86_400_000L

    /** Semantic-version ordering including beta/rc builds. Stable is newer than a prerelease. */
    fun compareVersions(first: String, second: String): Int {
        val a = parseVersion(first) ?: return 0
        val b = parseVersion(second) ?: return 0
        for (index in 0..2) {
            if (a.numbers[index] != b.numbers[index]) return a.numbers[index].compareTo(b.numbers[index])
        }
        if (a.preRelease == null && b.preRelease == null) return 0
        if (a.preRelease == null) return 1
        if (b.preRelease == null) return -1
        val size = maxOf(a.preRelease.size, b.preRelease.size)
        for (index in 0 until size) {
            val left = a.preRelease.getOrNull(index) ?: return -1
            val right = b.preRelease.getOrNull(index) ?: return 1
            val leftNumber = left.toIntOrNull()
            val rightNumber = right.toIntOrNull()
            val comparison = when {
                leftNumber != null && rightNumber != null -> leftNumber.compareTo(rightNumber)
                leftNumber != null -> -1
                rightNumber != null -> 1
                else -> left.compareTo(right, ignoreCase = true)
            }
            if (comparison != 0) return comparison
        }
        return 0
    }

    fun select(release: GitHubRelease, abis: List<String>): String? {
        val version = release.tagName.removePrefix("v")
        if (parseVersion(version) == null) return null
        val prefix = "https://github.com/hidooch980/MLH-VPN/releases/download/${release.tagName}/"
        for (abi in abis + "universal") {
            if (abi !in setOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64", "universal")) continue
            val name = "MLHVPN_${version}-fdroid_${abi}.apk"
            val asset = release.assets.singleOrNull { it.name == name && it.browserDownloadUrl == prefix + name }
            if (asset != null) return asset.browserDownloadUrl
        }
        return null
    }

    private fun parseVersion(value: String): ParsedVersion? {
        val match = versionPattern.matchEntire(value.removePrefix("v")) ?: return null
        return ParsedVersion(
            numbers = listOf(match.groupValues[1].toInt(), match.groupValues[2].toInt(), match.groupValues[3].toInt()),
            preRelease = match.groupValues[4].takeIf(String::isNotEmpty)?.split('.'),
        )
    }

    private data class ParsedVersion(val numbers: List<Int>, val preRelease: List<String>?)
}
