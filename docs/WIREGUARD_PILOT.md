# WireGuard Pilot: Implementation Gate

Status: design only. No server is provisioned and no tunnel is live.

## Target topology

- Control plane: Molla VPN API, account/device entitlements, short-lived configuration issuance and revocation.
- Data plane: a dedicated Linux VPN node running WireGuard.
- Client: native WireGuard tunnel integration per supported operating system, managed by the Molla VPN application.
- Cloudflare: DNS, TLS and protection for control-plane HTTP endpoints. It is not used as a generic WireGuard UDP relay.

## Pilot scope

The first end-to-end proof should be deliberately small:

1. One Linux server with a static public IPv4 address and working UDP ingress.
2. One WireGuard interface and one isolated client peer.
3. Explicit firewall rules: allow the chosen WireGuard UDP port; deny unintended management exposure.
4. IP forwarding and NAT configured only for the intended VPN interface and egress.
5. Client profile delivered through a one-time authenticated flow; never place private keys in logs, Git, API responses or analytics.
6. Revoke the test peer and prove that its traffic stops.
7. Verify DNS behavior, IPv4 routing, IPv6 leak behavior, reconnect, sleep/wake and network switching.
8. Record throughput, latency, packet loss, CPU and memory under a controlled test.

## Security gates

- Generate client private keys on the client device where the platform permits it; the server receives only the public key.
- Treat server private keys as deployment secrets, not application configuration.
- Use unique peer keys per device and support immediate peer revocation.
- Do not expose SSH or management ports to the public internet without source restrictions and key-only authentication.
- Keep API authorization and server-side subscription/device limits authoritative.
- Do not return a configuration until account and device authorization are complete.
- Redact peer keys, tokens and configuration payloads from application logs.
- Back up server configuration securely and test restoration before pilot use.

## Required deployment inputs

Before provisioning, the owner must select:

- Hosting provider or confirm an existing server.
- VPN exit region/country.
- Server budget and expected initial concurrent users.
- Whether a static IPv4 address is included.
- Whether the provider permits VPN traffic and the required UDP port.

Do not guess these values or silently create billable infrastructure.

## Acceptance criteria

The pilot is not complete until a real client connects to the real node, public egress is verified, DNS and IPv6 leak checks pass, peer revocation is proven, and reconnect/network-transition tests pass. A green API or UI test alone is not evidence of a working VPN.
