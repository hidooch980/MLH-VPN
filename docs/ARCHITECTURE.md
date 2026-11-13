# Molla VPN Architecture

## Principles
1. Separate the control plane from the VPN data plane.
2. Keep this product independent from MolidoVPN, vpn-sub, Forex, and Voice-AI application code.
3. Do not route generic VPN payloads through Workers as if Workers were a universal TCP/UDP VPN relay.
4. Make protocol support capability-driven and test it on each target OS before advertising it.
5. Keep credentials and private server inventory out of Git.

## Logical components
- Client applications: shared Flutter UI for Android, iOS, Windows and macOS.
- Tunnel adapters: platform-specific integration for WireGuard and sing-box. OS VPN APIs own tunnel lifecycle and permissions.
- Control-plane API: authentication, device enrollment, subscription entitlements, server catalog, configuration delivery, revocation and telemetry.
- Admin console: operator roles, server health, subscription management, abuse controls and audit trail.
- Data plane: dedicated Linux VPN nodes running selected protocol services.
- Cloudflare: authoritative DNS, TLS, WAF/API protection, bot controls and optional private access to management services. Cloudflare Tunnel may publish selected management endpoints; it is not the default transport for end-user VPN packets.

## Protocol plan
| Protocol | Role | Initial status |
|---|---|---|
| WireGuard | Primary high-performance tunnel | Planned |
| sing-box | Client/server orchestration and routing layer | Planned; validate platform support |
| VLESS + REALITY | Optional proxy transport through Xray-compatible stack | Planned; security and interoperability review required |
| Hysteria 2 | Optional QUIC-based transport | Planned; network-condition testing required |
| TUIC | Candidate for later evaluation | Deferred |

Avoid shipping multiple overlapping tunnel engines until benchmarks demonstrate a user benefit. sing-box is a routing/orchestration component, not itself one wire protocol.

## Proposed service boundaries
- auth: accounts, sessions, MFA and device identity
- subscriptions: plans, entitlements, renewals and revocation
- catalog: regions, nodes, protocol capabilities and maintenance state
- provisioning: short-lived client configuration issuance and rotation
- telemetry: opt-in, minimized connection health metrics
- admin: RBAC, audit events and operational controls

## Data and privacy
- Store only data required for account, billing, abuse prevention and service operation.
- Do not log browsing destinations or DNS query contents by default.
- Minimize and define retention for source IP and connection metadata; document lawful requirements per operating jurisdiction.
- Use short-lived enrollment and configuration tokens; never embed server private keys in clients.
- Encrypt secrets at rest and in transit; rotate credentials and support immediate device revocation.

## Reliability and routing
Clients should probe eligible endpoints, select by measured latency and recent success, and fail over only when the active tunnel is unhealthy. Measure throughput, packet loss, handshake time, battery use, reconnect behavior and DNS leak resistance per OS.

## Deployment
Production deployment is blocked until domain, regions, server providers, billing provider, legal/privacy review, and operational ownership are decided. Cloudflare resources are not created by this bootstrap commit.
