# MolidoVPN AI — Redesign Blueprint

**Status:** implementation foundation / not a release  
**Target product name:** MolidoVPN AI  
**Design direction:** futuristic, premium, intelligent  
**Primary target:** Android ARM64 first; other platforms remain later phases  
**Baseline artifact:** user-supplied `MolidoMeshAI-android-arm64.apk`

## 1. Baseline evidence and limits

The supplied APK was available in the conversation and its archive inventory was partially inspected. The observed archive has 905 entries, is 61,551,646 bytes, and has SHA-256:

`846b9e3dbc6c6aee2f987bed778ed97a8e7b260a4b9dcc9a863b89f967b936fd`

The following ARM64 native libraries were observed in that APK:

- `libaether.so`, `libaether_jni.so`, `libgojni.so`
- `libobfs4proxy.so`
- `libsingbox.so`
- `libtor.so`
- `libtun2socks.so`
- `libxray.so`

This is evidence that the named native components are packaged, not proof that each is initialized, correctly configured, secure, or able to establish a working tunnel. The APK manifest, signing certificate, package ID, DEX call graph, bundled configuration assets, licenses, and on-device behavior still require a full audit. Do not copy bundled server credentials or endpoint secrets into source control.

## 2. Repository relationship

The currently accessible VPN source repository is `hidooch980/MLH-VPN` (default branch `main`). Its current Android app is a reproducible v2rayNG 2.3.15 derivative. The repository README explicitly says the hosted service has no live VPN node, native hosted-service tunnel adapter, customer configuration issuance, or verified end-to-end data plane yet.

This redesign is being staged on a separate feature branch. Do not overwrite `main`, silently rename the repository, or assume that the supplied APK is byte-for-byte produced from this source tree. A source-to-binary provenance comparison is a release gate.

## 3. Product principles

1. Android-first, native Kotlin/Jetpack Compose UI.
2. Offline-capable client: importing and using a valid user/provider configuration must not require GitHub.
3. No bundled public server credentials, hard-coded subscriptions, or private keys.
4. Protocols are shown as supported only after engine integration and device acceptance tests.
5. AI selection is explainable, local-first, deterministic and safe; no black-box model can bypass security policy.
6. Stable connection beats frequent switching. Never switch solely for a tiny latency difference.
7. Fail closed when tunnel health is unknown; provide Android Always-on VPN / Block connections without VPN guidance.
8. Privacy by default: do not upload browsing destinations, DNS queries, payloads, or raw configuration strings for quality learning.

## 4. Target architecture

- **Presentation:** Compose design system, dashboard, server catalogue, import flow, diagnostics, settings, privacy and update screens.
- **Domain:** connection state machine, profile validation, protocol capability registry, server selection policy, subscription lifecycle.
- **Engine adapters:** isolated adapters for Xray/sing-box/WireGuard and separately integrated Tor/obfs4/Aether components where their APIs and licenses permit.
- **Android platform:** VpnService lifecycle, foreground notification, network callbacks, permission handling, boot behavior, per-app routing and always-on compatibility.
- **Intelligence:** quality probes, rolling latency/jitter/loss estimates, protocol capability checks, conservative ranking, circuit breaker, cooldown and user override.
- **Storage:** encrypted-at-rest profile secrets using Android Keystore-backed encryption; redact secrets from logs, crash reports and analytics.
- **Update:** signed metadata, pinned signing key, SHA-256 artifact verification, staged rollout and rollback. No self-update by downloading arbitrary APKs from untrusted URLs.
- **Optional control plane:** account/device/subscription APIs only when deployed; the core client must remain useful without it.

## 5. Protocol capability policy

The Android source documentation currently describes these formats/cores: VMess, VLESS (including TLS/REALITY where supported by the pinned Xray core), Trojan, Shadowsocks, SOCKS, HTTP, WireGuard and Hysteria2. Actual combinations depend on pinned core versions and must be verified on devices.

The supplied APK additionally contains native libraries named for Tor, obfs4proxy, sing-box, tun2socks and Aether. These are audit candidates, not automatically enabled features.

A capability registry must record, per protocol and core version:
- parser/import support;
- runtime adapter availability;
- supported transports and security modes;
- ARM64 ABI availability;
- health-check method;
- licensing/notice obligations;
- device test status.

Do not label Tor/obfs4, SSH, TUIC, AmneziaWG or any other feature as working until its adapter, dependency provenance, UI flow and end-to-end tests exist. Avoid presenting multiple protocol names as six independent protocols when they are only URI/config variants of the same engine.

## 6. AI selection v1

The first release uses a transparent weighted score, not a remote LLM:

- reachability and recent connection success;
- median latency and jitter from multiple probes;
- packet-loss estimate when measurable;
- throughput sample only when measured safely;
- protocol/transport compatibility with the current network;
- recent failures, cooldown and server quarantine;
- user preferences (country, cost, battery, speed, privacy).

Hard safety constraints always override score. Require a meaningful score margin and minimum dwell time before switching. Expose a short reason such as `stable_connection`, `current_server_unhealthy` or `material_quality_gain`. Store only coarse, opt-in metrics; do not collect browsing activity.

## 7. Visual design system

- Deep midnight navy background, restrained electric-cyan and violet accents, high-contrast readable text.
- Premium connection orb/ring with clear connected / connecting / disconnected / error states.
- Dashboard cards: connection status, selected region/server, latency, download/upload rates, session traffic and protection status.
- Primary Connect/Disconnect action is large and unambiguous.
- Server catalogue has search, region, latency, quality and recent-use filters.
- Motion is subtle, reduced-motion aware and never conveys false connection status.
- Persian RTL, English, Arabic RTL and Turkish localization; all layouts must be tested at large font scale.
- No fabricated live speed, ping, country, IP or security status. Unknown values display as unknown.

## 8. Delivery phases and acceptance gates

### Phase A — source and APK audit
- Decode manifest and package metadata; inspect certificate/signature and SDK levels.
- Inventory DEX packages, native ABI, library versions, licenses and configuration assets.
- Identify endpoints/tokens/secrets and rotate exposed credentials.
- Compare APK package ID, version and code fingerprints with repository source.
- Produce an auditable feature matrix.

### Phase B — visual shell
- Implement Compose theme, navigation and dashboard states.
- Add accessible server list, import, settings and diagnostics.
- Add screenshot/UI tests for RTL/LTR and common screen sizes.

### Phase C — connection domain
- Formalize tunnel state machine and service lifecycle.
- Add profile validation and adapter interfaces.
- Retain known-good upstream engine implementations; do not rewrite cryptographic transports.

### Phase D — intelligent selection
- Implement explainable local quality ranking, hysteresis, cooldown and quarantine.
- Add deterministic unit tests and simulated network conditions.
- Add user override and disable-smart-selection option.

### Phase E — security and release
- Validate kill-switch/always-on behavior, DNS/IPv4/IPv6 leaks, reconnect, handover, process death and permission revocation.
- Add signed reproducible builds, SBOM, dependency/license review and artifact checksums.
- Run real-device tests with user-owned valid endpoints.
- Publish only after all release gates pass.

## 9. Non-goals for the first milestone

- No promise of free VPN servers or connectivity without a valid remote endpoint.
- No hidden traffic interception, certificate installation, or bypass of Android permission prompts.
- No cloud AI requirement for basic connection.
- No production billing/account system until the data plane and privacy/legal requirements are ready.
- No claim of anonymity or censorship circumvention without independent tests.

## 10. Current implementation status

This document is the first tracked design artifact on the redesign branch. It is not an APK, and no native source code has yet been changed by this commit. The next implementation unit is the Compose visual shell and the protocol capability registry, followed by source-to-APK provenance analysis.
