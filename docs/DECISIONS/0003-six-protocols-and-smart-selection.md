# ADR 0003: Six-protocol portfolio and automatic selection

- Status: Accepted
- Date: 2026-11-13

## Context
Molla VPN targets Android, iOS, Windows and macOS, with personal and commercial subscription plans. A single protocol will not provide the best availability and experience across all networks, platforms and server regions.

## Decision
Adopt a six-protocol product portfolio:
1. WireGuard — primary high-performance tunnel.
2. VLESS + REALITY — TCP-based fallback candidate.
3. Hysteria 2 — QUIC/UDP fallback candidate.
4. TUIC — additional QUIC/UDP fallback candidate.
5. AmneziaWG — specialized WireGuard-derived UDP option.
6. Shadowsocks 2022 — specialized encrypted proxy option.

Enable automatic protocol selection as the intended default product policy. Use measured eligibility and connection-quality signals, not a hard-coded universal ranking. Keep a user manual override.

sing-box is an integration/orchestration engine for selected protocols, not a seventh protocol. WireGuard and AmneziaWG may require dedicated native adapters; VLESS + REALITY requires an Xray-compatible integration. Platform-specific support and licensing must be verified before selecting final dependencies.

## Safety constraints
- Catalog policy is not live availability: all six protocols remain planned, disabled and not traffic-plane-ready until their exact client/server/platform paths pass acceptance tests.
- Runtime automatic selection remains disabled until verified adapters, real nodes and end-to-end traffic tests exist.
- Never downgrade encryption or bypass user/operator/subscription policy.
- Preserve OS kill-switch, DNS and route integrity through reconnect/failover.
- Use eligibility filtering, repeated probes, hysteresis, cooldown and bounded retries to avoid flapping.
- Minimize telemetry; never use browsing destinations or DNS query contents as selector inputs.

## Consequences
The API catalog and docs describe six protocols and the automatic policy. The roadmap now sequences adapter work and per-protocol/platform acceptance. This increases engineering, QA, packaging, licensing and operations scope; release must be staged rather than enabling all six at once.
