# MolidoVPN AI — Final Product Decisions

**Status:** approved product baseline; implementation and device verification remain in progress  
**Primary milestone:** Android ARM64 standalone client  
**Repository:** `hidooch980/MLH-VPN`  
**Development branch:** `feature/molidovpn-ai-redesign`

This document consolidates the product decisions for implementation. It does not claim that unimplemented features are available or tested.

## 1. Product identity and scope

- Product name: **MolidoVPN AI**.
- Visual direction: futuristic, premium, calm, and intelligent; prioritize clarity over decorative effects.
- First release target: Android ARM64.
- Persian and English are launch priorities; Arabic and Turkish follow in the localization pass.
- The Android client must remain usable without a Molido account, cloud AI, or newly deployed Molido server.
- A valid user/provider configuration and reachable remote endpoint are required for VPN connectivity.
- Do not bundle shared public credentials, private keys, or undocumented subscription endpoints.

## 2. Preserve and audit before replacing

- Keep the pinned upstream v2rayNG/Xray tunnel, parser, routing, and permission lifecycle unless a measured defect requires a change.
- Do not rewrite cryptographic transports or tunnel engines as part of a UI redesign.
- Audit the supplied APK independently: package/application ID, signing certificate, version, manifest, DEX, native ABI, library provenance, licenses, embedded endpoints/configuration, and runtime behavior.
- Compare APK provenance with repository sources before claiming the APK is reproducible from this repository.
- Treat packaged Tor, obfs4, sing-box, tun2socks, and Aether libraries as audit candidates until their adapters, licenses, UI paths, and end-to-end tests are confirmed.

## 3. Protocol capability policy

The product target is to evaluate and expose these six protocol families:

1. VLESS
2. VMess
3. Trojan
4. Shadowsocks
5. Hysteria2
6. WireGuard

The existing pinned Android implementation also documents SOCKS and HTTP profile support. These must not be presented as equivalent independent VPN tunnel protocols.

For every protocol, maintain a capability record covering import/parser support, runtime engine, supported transports/security modes, ABI availability, health checks, dependency/license notices, and physical-device test status. A protocol may be advertised as verified only after a valid profile has connected and passed traffic, DNS, IPv4/IPv6, reconnect, and handover tests on a real device.

Tor/obfs4 and other additional transports remain experimental until separately integrated and accepted.

## 4. Local-first intelligence

Version 1 must use a deterministic, explainable on-device quality engine rather than requiring a cloud LLM.

Inputs may include:
- recent successful connection rate;
- median latency and jitter from repeated probes;
- packet loss when measurable;
- measured throughput when safe and available;
- network type and protocol compatibility;
- recent failures, cooldown, and quarantine state;
- explicit user preferences.

Safety and user-selected constraints always override ranking. Add hysteresis, a minimum dwell time, and a meaningful quality margin to prevent rapid switching. Provide a manual server/protocol override and a complete smart-selection off switch.

Show concise decision reasons such as `stable_connection`, `current_server_unhealthy`, or `material_quality_gain`. Do not infer or display speed, latency, country, IP, or protection status when the value is unknown.

## 5. User experience

Required screens:
- Home dashboard with unmistakable disconnected, connecting, connected, and error states.
- Large Connect/Disconnect action.
- Server/profile catalogue with search, filters, quality and recent-use information.
- Import flow for QR, clipboard, file, URL/subscription, and supported profile formats.
- Connection diagnostics and per-protocol capability status.
- Settings for smart selection, protocol preference, language, privacy, and Android VPN safety guidance.
- Update and release-notes screen.

Use a midnight navy base with restrained cyan/violet accents, readable contrast, subtle motion, and reduced-motion support. RTL/LTR and large font scaling must be tested. Never use animations or decorative indicators to imply a tunnel is connected when it is not.

## 6. Privacy and security baseline

- Encrypt locally stored profile secrets with Android Keystore-backed keys where feasible.
- Redact profile URLs, credentials, private keys, DNS queries, browsing destinations, and payloads from logs and crash reports.
- Do not upload browsing history, DNS requests, payloads, or raw configurations for AI learning.
- Keep quality learning local by default; any optional telemetry must be coarse, opt-in, documented, and revocable.
- Verify Android Always-on VPN and Block connections without VPN behavior; provide clear setup instructions.
- Test DNS, IPv4, IPv6, reconnect, process death, permission revocation, airplane mode, and Wi-Fi/mobile transitions.
- Do not claim anonymity, censorship circumvention, or leak protection without evidence.

## 7. Updates and release integrity

- Use signed release artifacts and signed update metadata with a pinned verification key.
- Verify artifact hashes before installation and use staged rollout plus rollback.
- Never download and execute arbitrary APKs from untrusted URLs.
- Keep signing keys out of GitHub source, CI logs, and the repository.
- Publish source archives, upstream notices, license notices, SBOM/dependency inventory, and SHA-256 checksums with releases.
- Keep the existing application ID until upgrade compatibility and signing continuity are deliberately verified. Do not change package identity as a cosmetic rebrand.

## 8. Delivery order and exit criteria

### Gate A — baseline audit
Complete APK metadata/signature/DEX/native-library/license review; map every observed capability to repository source and record unknowns.

### Gate B — visual shell
Implement Compose design tokens, navigation, dashboard states, import and settings screens; add RTL/LTR screenshot and accessibility tests.

### Gate C — connection and capability layer
Define connection state machine and capability registry; preserve proven upstream engine paths; add adapter-level tests.

### Gate D — smart quality engine
Implement explainable ranking, cooldown, quarantine, hysteresis, manual override, and deterministic simulated-network tests.

### Gate E — device acceptance
Use user-owned valid endpoints and physical Android devices. Test each advertised protocol and all leak, lifecycle, permission, network-transition, and subscription failure cases. Record device/OS/core versions without storing secrets.

### Gate F — release
Produce reproducible signed APK/AAB as appropriate, verify signing and upgrade path, publish checksums and notices, and complete privacy/security review.

A successful CI build is not proof of live VPN connectivity. A release is not production-ready until all advertised features pass their acceptance tests.

## 9. Explicit non-goals for the first milestone

- No requirement for a large or continuously running AI server.
- No promise of free VPN endpoints or connectivity without a remote server.
- No production billing or customer subscription service until a live data plane, support, legal, privacy, and abuse controls are ready.
- No claim that every library found in the supplied APK is integrated or operational.
- No merge to `main` and no public production release before review of the implementation and acceptance evidence.
