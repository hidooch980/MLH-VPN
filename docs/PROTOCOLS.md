# Molla VPN — Six-Protocol Strategy

**Decision:** Molla VPN will support a six-protocol product strategy with automatic protocol selection enabled as the intended default. This is a product/architecture decision, not a claim that the protocols are currently operational.

## Protocol portfolio

| Priority | Protocol | Transport | Engine / integration | Intended role | Current implementation status |
|---:|---|---|---|---|---|
| 1 | WireGuard | UDP | Native WireGuard adapter | Primary high-performance tunnel | Planned; disabled |
| 2 | VLESS + REALITY | TCP-based transport | Xray-compatible integration | Fallback for networks where the primary UDP path is unavailable | Planned; disabled |
| 3 | Hysteria 2 | QUIC/UDP | sing-box | Fallback candidate for lossy/high-latency paths | Planned; disabled |
| 4 | TUIC | QUIC/UDP | sing-box | Additional QUIC fallback | Planned; disabled |
| 5 | AmneziaWG | UDP | Dedicated compatible client/server adapter | Specialized WireGuard-derived option | Planned; disabled |
| 6 | Shadowsocks 2022 | TCP/UDP depending on deployment mode | sing-box | Specialized encrypted proxy option | Planned; disabled |

**Important distinction:** sing-box is a multi-protocol engine/orchestrator, not a seventh protocol. It is the planned integration engine for selected transports (Hysteria 2, TUIC and Shadowsocks). WireGuard and AmneziaWG require compatible native/platform adapters; VLESS + REALITY requires an Xray-compatible integration. Exact platform support and packaging must be validated before choosing dependencies.

## Automatic protocol selection

Automatic selection is enabled in product policy, but runtime selection is **not implemented or ready** yet. The client must not attempt to select or connect to a protocol solely because it appears in the catalog.

The future selector must consider:
1. Client OS, CPU architecture and installed/verified adapter capabilities.
2. Server/node health and the exact protocol versions/capabilities advertised by that node.
3. Reachability probes and recent success/failure history.
4. Handshake/connection setup latency.
5. Packet loss and measured throughput (where measurable).
6. Transport reachability, including whether UDP/QUIC is blocked or degraded.
7. User plan, region, operator policy and maintenance state.

Selection should be staged: filter to eligible protocols first, then rank by measured reliability and latency, and use throughput/packet loss as additional signals. Avoid hard-coded universal protocol rankings: real performance depends on region, network, server load and client platform. Use hysteresis and repeated failures before switching to avoid flapping. Keep a user-visible manual override and explain why a protocol was selected.

## Mandatory safety rules

- Never select a protocol whose client adapter or server data plane is unavailable, unverified, unhealthy or disallowed.
- A planned catalog entry is not an available service.
- Never downgrade to plaintext or disable encryption to force connectivity.
- Preserve OS kill-switch / lockdown protection throughout reconnect and failover.
- Do not change routes or DNS outside the active secure tunnel during transition.
- Do not switch on one transient probe failure; use thresholds, cooldown and bounded retries.
- Do not leak credentials, private keys, REALITY secrets or node endpoints in public catalog responses or logs.
- Enforce subscription, device, region, abuse and operator policies before issuing a profile.
- Permit immediate user disconnect and device revocation.

## sing-box and adapter boundaries

Use sing-box for supported proxy protocol orchestration only after checking its official support matrix, release integrity, licensing, platform APIs and packaging obligations. Do not assume sing-box implements WireGuard/AmneziaWG integration identically on every OS. Keep protocol-specific adapters behind a typed interface owned by the platform VPN service/extension. The OS VPN API remains the owner of tunnel lifecycle and permission prompts.

## Rollout sequence

1. Implement and verify WireGuard on Android with a real Linux node.
2. Add telemetry and deterministic selector tests; selector initially runs in shadow mode and cannot change active connections.
3. Integrate VLESS + REALITY and validate its client/server compatibility and security configuration.
4. Add Hysteria 2 and TUIC through pinned sing-box builds, one at a time.
5. Add AmneziaWG with compatible client/server code and licensing review.
6. Add Shadowsocks 2022 through sing-box and validate supported ciphers/modes.
7. Enable automatic selection for an internal cohort only after each relevant protocol/platform pair passes acceptance gates.
8. Expand to iOS, Windows and macOS only after native VPN APIs, signing and packaging are independently validated.

## Acceptance matrix

For every protocol × platform × server version, test:
- Connection setup, authenticated handshake, real bidirectional traffic and clean disconnect.
- DNS, IPv4 and IPv6 leak resistance; split tunneling and route ownership.
- Kill switch during connect, failure, protocol switch, sleep/wake and network handoff.
- UDP-blocked, TCP-restricted, captive portal, roaming, MTU, high latency and packet loss.
- Revocation, expired subscription, key rotation, replay/downgrade resistance and abuse limits.
- Median/p95 latency, sustained throughput, packet loss, CPU, memory, battery and reconnect rate.

A protocol is advertised as available only after real end-to-end tests pass on the exact supported platform and server build. The current backend catalog intentionally reports all six as planned, disabled and not traffic-plane-ready.
