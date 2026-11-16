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
