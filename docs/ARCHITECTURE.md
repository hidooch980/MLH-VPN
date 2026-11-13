# Molla VPN Architecture

## Principles
1. Separate the control plane from the VPN data plane.
2. Keep this product independent from MolidoVPN, vpn-sub, Forex, and Voice-AI application code.
3. Do not route generic VPN payloads through Workers as if Workers were a universal TCP/UDP VPN relay.
4. Make protocol support capability-driven and test it on each target OS before advertising it.
5. Keep credentials and private server inventory out of Git.
6. Treat automatic protocol selection as policy-enabled but runtime-disabled until verified adapters and server paths exist.

## Logical components
- Client applications: shared Flutter UI for Android, iOS, Windows and macOS.
- Smart Connection Manager: evaluates eligible protocol/node pairs using platform capability, node health, reachability, handshake latency, packet loss, throughput and policy; supports hysteresis, cooldown, manual override and reason reporting.
- Platform VPN owner: Android VpnService, Apple Network Extension, and supported Windows/macOS VPN APIs own TUN/packet routing, permissions, DNS and kill-switch lifecycle.
- Protocol adapters: WireGuard, Xray-compatible VLESS + REALITY, sing-box-managed Hysteria 2/TUIC/Shadowsocks 2022, and a compatible AmneziaWG adapter.
- sing-box: multi-protocol engine/orchestrator for supported transports, not itself a VPN protocol and not assumed to replace every native WireGuard/AmneziaWG adapter.
- Control-plane API: authentication, device enrollment, subscription entitlements, server catalog, configuration delivery, revocation and minimized telemetry.
- Admin console: operator roles, server health, subscription management, abuse controls and audit trail.
- Data plane: dedicated Linux VPN nodes running selected protocol services.
- Cloudflare: authoritative DNS, TLS, WAF/API protection, bot controls and optional private access to management services. Cloudflare Tunnel may publish selected management endpoints; it is not the default transport for end-user VPN packets.

## Six-protocol portfolio

| Protocol | Role | Engine/integration | Status |
|---|---|---|---|
| WireGuard | Primary high-performance tunnel | Native adapter | Planned, disabled |
| VLESS + REALITY | TCP-based fallback | Xray-compatible adapter | Planned, disabled |
| Hysteria 2 | QUIC fallback | sing-box | Planned, disabled |
| TUIC | QUIC fallback | sing-box | Planned, disabled |
| AmneziaWG | Specialized UDP option | Compatible dedicated adapter | Planned, disabled |
| Shadowsocks 2022 | Specialized encrypted proxy | sing-box | Planned, disabled |

sing-box is an orchestration/transport engine, not a seventh protocol. Protocol/platform compatibility, licensing, build provenance and packaging must be verified independently.

## Smart selection policy

The product policy is set to automatic selection. Runtime selection remains unavailable until actual adapters and healthy nodes exist. The selector must:
1. Filter out protocols unsupported by the current OS/client build, unavailable on the node, unhealthy, disabled by operator policy or not entitled by the subscription.
2. Probe only approved endpoints and collect minimized connection-quality signals.
3. Rank eligible candidates by reliability and measured setup latency, then consider loss/throughput and user policy.
4. Apply hysteresis, cooldown and bounded retries to prevent rapid switching.
5. Preserve the OS kill switch and secure DNS/routing throughout transitions.
6. Offer manual override, explain selection reasons and honor immediate disconnect.

Never infer availability from a catalog entry. Never downgrade encryption or bypass user/operator policy to force a connection.

## Proposed service boundaries
- auth: accounts, sessions, MFA and device identity
- subscriptions: plans, entitlements, renewals and revocation
- catalog: regions, nodes, protocol capabilities and maintenance state
- provisioning: short-lived client configuration issuance and rotation
- selection: candidate eligibility, probes, scoring, hysteresis and failover policy
- telemetry: opt-in, minimized connection health metrics
- admin: RBAC, audit events and operational controls

## Data and privacy
- Store only data required for account, billing, abuse prevention and service operation.
- Do not log browsing destinations or DNS query contents by default.
- Minimize and define retention for source IP and connection metadata; document lawful requirements per operating jurisdiction.
- Use short-lived enrollment and configuration tokens; never embed server private keys in clients.
- Encrypt secrets at rest and in transit; rotate credentials and support immediate device revocation.
- Do not use browsing history or DNS contents as smart-selection inputs.

## Reliability and routing
Probe eligible endpoints and select by measured reliability and connection quality. Fail over only when the active tunnel is unhealthy and a verified candidate exists. Preserve kill-switch, DNS and route integrity during every transition. Measure throughput, packet loss, handshake time, battery use, reconnect behavior and DNS leak resistance per OS.

## Deployment
Production deployment is blocked until domain, regions, server providers, billing provider, legal/privacy review, and operational ownership are decided. Cloudflare resources are not created by this bootstrap commit.
