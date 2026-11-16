# MolidoVPN AI — Implementation Audit (2026-11-16)

**Scope:** source review on `feature/molidovpn-ai-redesign` after the first hardening pass.  
**Important:** this is a source-level review, not a physical-device security certification.

## Confirmed source components

- Android client is a pinned v2rayNG 2.3.15 overlay.
- Compose dashboard and navigation already exist in `MlhDashboard.kt`.
- A local quality scoring engine exists in `SmartQualityEngine.kt`.
- A protocol evidence registry exists in `MolidoProtocolRegistry.kt`.
- Unit tests exist for quality scoring, protocol registry, local selection and related helpers.
- The standalone Android GitHub Actions workflow builds a debug preview, runs tests, checks package label/application ID, and runs an emulator smoke test.

## Changes made in this audit pass

1. Quality probe inputs now reject negative and implausibly large latency values; rejected values count against attempted probes.
2. Throughput calculation now uses bounded floating-point conversion to avoid integer multiplication overflow.
3. Country codes are accepted only when they are exactly two ASCII letters.
4. Dashboard wording no longer presents a connected state as proof of stable quality, claims DNS has been verified through the tunnel, or labels a country-code-derived illustrative map as a verified precise location.
5. Added regression tests for invalid probe samples and throughput overflow.

## Remaining findings / release blockers

### P0 — physical-device and leak acceptance
- No source-level review can prove that a real tunnel works with user-owned endpoints.
- Test every advertised protocol on a physical ARM64 Android device.
- Verify DNS, IPv4, IPv6, kill-switch/Always-on, process death, permission revoke, airplane mode, Wi-Fi/mobile handover and reconnect.

### P1 — localization and accessibility
- The current branded dashboard contains Persian hard-coded labels and forces RTL layout.
- Complete resource-based localization for Persian, English, Arabic and Turkish; choose layout direction from the active locale rather than hard-coding RTL.
- Add large-font, TalkBack, contrast, reduced-motion and RTL/LTR screenshot tests.

### P1 — UI truthfulness and telemetry
- Country-based map illustration is not a geolocation measurement. Keep it clearly labeled approximate or replace it with a non-geographic region card.
- Do not show DNS protection, stable quality, country, IP, or speed as verified unless the underlying measurement exists and is fresh.
- Ensure stale traffic samples are visibly marked unavailable rather than reused.

### P1 — engine lifecycle and switching
- Verify that smart selection respects manual user choice, minimum dwell time, cooldown and hysteresis in the service lifecycle.
- Ensure server changes do not interrupt an otherwise healthy connection for marginal score changes.
- Confirm quarantine recovery policy and persistence across process restarts.

### P1 — signing and upgrade continuity
- Keep the current application ID unchanged until the signing certificate and upgrade path are verified against the APK supplied by the user.
- A debug APK is not a production release. The production signing key must remain outside the repository and CI logs.
- Test install-over-upgrade with the exact production signing identity before distribution.

### P2 — privacy and supply chain
- Complete APK manifest/DEX/native library provenance and license review.
- Verify that profile URLs, credentials, private keys, DNS queries and browsing destinations never enter logs, analytics or crash reports.
- Review community subscription source trust, update integrity, user consent and removal controls.

## Current CI state

The Android standalone workflow was triggered by the latest Android source changes. Its run is currently **in progress**; no successful build/test result is claimed in this audit. Follow the run here:

https://github.com/hidooch980/MLH-VPN/actions/runs/43819934493

## Release decision

**Not ready for production release.** Continue only on the feature branch. Do not merge to `main`, publish a production APK, or advertise all six protocol families as verified until CI, source review, signing continuity and physical-device acceptance are complete.

## UI redesign work added after the source audit

- Added `docs/MOLIDOVPN_AI_UI_SPEC_FA.md` as the Persian RTL/LTR screen and interaction specification.
- Expanded the Android bottom navigation from four to five destinations: Home, Servers, AI, Protocols and More.
- Added a protocol catalogue screen driven by `MolidoProtocolRegistry`; it lists VLESS, VMess, Trojan, Shadowsocks, Hysteria2 and WireGuard as source-documented and explicitly marks physical-device verification as still required.
- Added a clear explanation that protocol choice comes from an imported profile; the catalogue does not fabricate endpoints or rewrite profile protocols.
- Added an AI page heading/context around the existing on-device quality assessment and real server-test action.
- Linked the Persian UI specification from the repository README.

**Verification status for this UI change:** source edits were committed to the feature branch. No Android build, unit test, emulator run, or physical-device test result has yet been retrieved for these exact commits. Do not treat the new UI as build-verified until the Standalone Android APK workflow completes successfully.


## Finalization review — 2026-11-16 (follow-up)

### Verified CI for the smart-selection integration
- Android standalone workflow run: [43920252812](https://github.com/hidooch980/MLH-VPN/actions/runs/43920252812).
- Tested source commit: `82298004e58719dd100090982dd40bea27f162c0`.
- Build job: success; APK preview packaging and native-engine identity checks: success.
- Android emulator smoke job: success.
- Artifact `MLHVPN-Android-preview`: ID `14254117139`, SHA-256 `48c6639ca0aafbf3140070a9ef688d1d29a9bf127414e74d2b96743a9e0f9d1d`.
- This is a CI preview build, not a production-signed release and not a physical-device VPN acceptance result.

### Confirmed architecture boundaries
- The active tunnel lifecycle is still centered on `CoreServiceManager` and the pinned Xray-compatible `CoreController` (`startLoop` / `stopLoop`). No common multi-engine adapter abstraction is currently wired into that service lifecycle.
- `SmartEngineOrchestrator` is called by `MainViewModel` for mode selection and after bulk tests, but its result changes the selected profile; it does not start, stop, or switch a running tunnel itself.
- Candidate construction currently reads `ServersCache.testDelayMillis` and creates a one-attempt assessment. The cache model shown here has no explicit measurement timestamp/TTL at this call site. Therefore freshness is not proven and stale positive latency may be treated as current evidence. This is a correctness blocker for autonomous selection until freshness is enforced.
- The selection mode's `FAST`/ `STABLE`/ `COMPATIBLE` protocol allowlists are policy heuristics, not measured engine capability declarations. They must not be described as engine-runtime verification.
- When a tunnel is running, bulk-test completion intentionally avoids changing the selected profile; no automatic live engine handover is proven.

### Engine and license evidence
- The Android source is pinned to v2rayNG 2.3.15 at `57907cf354d9769200ed4717b4430d605abe0952`; the Xray-compatible AAR and HEV source are pinned by commit/checksum in `apps/android/upstream.lock.json` and the engine inventory.
- The Android upstream license file is GPL-3.0. Distribution obligations (corresponding source, notices, modifications, and installation information where applicable) must be checked for the exact distribution model.
- The AAR, HEV, AndroidX/Gradle transitive dependencies, and any native objects require a per-component license/NOTICE/SBOM review. The current evidence is not enough to declare the entire binary bundle legally cleared.
- WireGuard and Hysteria2 are source-documented/configuration paths; physical runtime acceptance is not demonstrated. sing-box, Tor and obfs4 are not enabled by the current evidence registry as verified independent runtime engines.

### Release blockers still open
1. Define and integrate a common engine adapter contract with truthful capability reporting and lifecycle ownership; keep the current Xray adapter as the only enabled adapter until another engine passes acceptance.
2. Add freshness metadata/TTL to probe evidence and reject stale samples before smart selection; test stale, missing, future-dated and failed probes.
3. Add lifecycle tests for start, stop, failure, timeout, cancellation, reconnect, network handover and service/process restart.
4. Complete per-component license and NOTICE inventory/SBOM, including AAR and native submodules.
5. Complete Persian/English/Arabic/Turkish resource localization and RTL/LTR tests; the current branded screen still has hard-coded Persian and forced RTL.
6. Run physical ARM64 device tests with a valid user-owned endpoint for each advertised protocol: handshake, real traffic, public egress IP, DNS over tunnel, IPv4/IPv6 leak, kill switch, Always-on, airplane mode, Wi-Fi/mobile handover, force-stop/restart and battery/performance.
7. Verify upgrade continuity with the existing production signing certificate. Debug preview success does not satisfy release signing.

**Decision:** CI preview build is green for commit `82298004e58719dd100090982dd40bea27f162c0`. MolidoVPN AI is **not production-final**. Do not claim a unified multi-engine runtime, verified automatic live switching, leak-proof operation, or production signing until the blockers above have evidence. Keep all work on the feature branch.
