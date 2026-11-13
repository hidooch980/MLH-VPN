# MLH VPN — standalone Android

This is a reproducible branding/build overlay for **v2rayNG 2.3.15**. It runs locally on Android 7+ without the Molla FastAPI service, an account, billing, or a server deployment. Import an existing provider's configuration to connect. It does not create free servers or provide connectivity without a remote endpoint.

The exact upstream commit, native core checksum, application ID and toolchain are recorded in `upstream.lock.json`. The tunnel, parser, transport and routing implementations are retained. A one-time catalog adds two public subscriptions to the existing storage and hourly updater; user deletions and preferences are preserved. See [SOURCES.md](SOURCES.md). The native app is the Android product; `../client` remains the earlier Flutter prototype.

## Compatibility

- VMess, VLESS (including TLS/REALITY), Trojan, Shadowsocks, SOCKS, HTTP, WireGuard and Hysteria2 as implemented by the pinned v2rayNG/Xray versions.
- Clipboard, QR, subscription URL, file and custom JSON imports; existing upstream profile editing, routing, latency testing and VPN permission flow.
- Upstream Persian localization is retained.
- Transports and options are accepted only when supported by the pinned Xray core. UI entries are not proof that every historical protocol/transport combination works. TUIC/AmneziaWG and arbitrary newer Xray features are not claimed.
- No real-server connectivity, censorship-resistance, DNS/IPv6-leak or kill-switch guarantee is implied by a successful APK build. Device acceptance remains required.

## Build

Use Linux, Git, Python 3.10+, Java 21, Android SDK platform 37.0, build tools 37.0.0 and NDK 30.0.16248370. The versions match pinned upstream. Set `ANDROID_HOME` and `JAVA_HOME`.

```sh
git clone https://github.com/hidooch980/MLH-VPN.git
cd MLH-VPN
mkdir -p work
git clone --no-checkout https://github.com/2dust/v2rayNG.git work/android
REVISION=$(python3 -c 'import json; print(json.load(open("apps/android/upstream.lock.json"))["commit"])')
git -C work/android checkout --detach "$REVISION"
git -C work/android submodule update --init --recursive
python3 apps/android/prepare.py work/android
bash apps/android/build.sh "$PWD/work/android" debug
```

Use a fresh upstream checkout for each overlay application: it deliberately refuses dirty source. `Standalone Android APK` runs the same build in GitHub Actions and uploads APKs, tests, source and SHA-256 checksums. The debug build uses an automatically generated development key, not a production identity. Builds from different runners may require uninstall/reinstall; export profiles first. Do not distribute this preview as a signed production release. Release builds are unsigned until signed with your own protected key; no private signing key is stored here.

The application ID is `io.github.hidooch980.mlhvpn.fdroid`, so the preview can coexist with official v2rayNG. The Kotlin namespace intentionally stays `com.v2ray.ang` for JNI compatibility. App name/output names and update/source/privacy URLs point to Molla; the upstream promotional menu is replaced by official WARP setup guidance. Upstream credits and translation acknowledgements remain.

## License and source

The generated Android derivative, including this overlay, is **GPL-3.0-only**; see `LICENSE`. v2rayNG contributors retain their copyrights. This does not change the license of unrelated backend/Flutter files. The license and upstream notice are added to the app's existing open-source-license screen after dependency notices are generated. The source archive and lockfile must accompany APK distribution. Native dependencies retain their own licenses; the pinned AndroidLibXrayLite release provides the AAR and its source, and the HEV submodule is built from source.

## Device acceptance (not replaced by CI)

Install on a physical Android device; import one valid configuration for every protocol to be offered; approve VPN permission; verify HTTP/DNS/IPv4/IPv6 through the intended exit; test disconnect, process death, permission revoke, airplane mode, Wi-Fi/mobile handover and Android always-on/block-without-VPN. Test subscription refresh with unavailable URLs and malformed entries. Record device, OS, core version and configuration type without credentials. Never label a protocol verified solely because its import succeeded.
