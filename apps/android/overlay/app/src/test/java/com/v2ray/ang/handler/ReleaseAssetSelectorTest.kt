package com.v2ray.ang.handler

import com.v2ray.ang.dto.GitHubRelease
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class ReleaseAssetSelectorTest {
    private fun asset(abi: String, version: String = "0.5.1"): GitHubRelease.Asset {
        val name = "MLHVPN_${version}-fdroid_${abi}.apk"
        return GitHubRelease.Asset(name, "https://github.com/hidooch980/MLH-VPN/releases/download/v$version/$name")
    }
    private fun release(vararg assets: GitHubRelease.Asset, version: String = "0.5.1") = GitHubRelease("v$version", "", assets.toList())

    @Test fun `daily checks handle first launch recent attempts and clock rollback`() {
        assertTrue(ReleaseAssetSelector.shouldCheck(1000, 0))
        assertFalse(ReleaseAssetSelector.shouldCheck(2000, 1000))
        assertTrue(ReleaseAssetSelector.shouldCheck(86_401_000, 1000))
        assertTrue(ReleaseAssetSelector.shouldCheck(1000, 2000))
    }

    @Test fun `arm devices prefer the matching small package`() {
        val arm = asset("arm64-v8a")
        assertEquals(arm.browserDownloadUrl, ReleaseAssetSelector.select(release(asset("universal"), arm), listOf("arm64-v8a", "armeabi-v7a")))
    }
    @Test fun `intel devices fall back to universal`() {
        val universal = asset("universal")
        assertEquals(universal.browserDownloadUrl, ReleaseAssetSelector.select(release(asset("arm64-v8a"), universal), listOf("x86_64", "x86")))
    }
    @Test fun `beta assets are accepted and selected`() {
        val beta = asset("arm64-v8a", "0.6.0-beta.2")
        assertEquals(beta.browserDownloadUrl, ReleaseAssetSelector.select(release(beta, version = "0.6.0-beta.2"), listOf("arm64-v8a")))
    }
    @Test fun `semantic versions order beta rc and stable correctly`() {
        assertTrue(ReleaseAssetSelector.compareVersions("0.6.0-beta.2", "0.6.0-beta.1") > 0)
        assertTrue(ReleaseAssetSelector.compareVersions("0.6.0-rc.1", "0.6.0-beta.9") > 0)
        assertTrue(ReleaseAssetSelector.compareVersions("0.6.0", "0.6.0-rc.9") > 0)
        assertTrue(ReleaseAssetSelector.compareVersions("0.7.0-beta.1", "0.6.9") > 0)
        assertEquals(0, ReleaseAssetSelector.compareVersions("0.6.0-beta.2", "0.6.0-beta.2"))
    }
    @Test fun `missing APK and non release links are rejected`() {
        assertNull(ReleaseAssetSelector.select(release(), listOf("arm64-v8a")))
        val fake = asset("universal").copy(browserDownloadUrl = "https://example.com/app.apk")
        assertNull(ReleaseAssetSelector.select(release(fake), listOf("x86")))
        assertNull(ReleaseAssetSelector.select(release(asset("universal"), asset("universal")), emptyList()))
    }
}
