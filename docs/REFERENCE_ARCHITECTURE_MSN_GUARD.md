# Molla VPN — Architecture lessons from MSN-GUARD

**Status:** adopted as an architecture reference, not a code import.

Reference reviewed: https://github.com/mbm110/MSN-GUARD  
Reference license: GNU AGPL-3.0. Review dependency licenses before any code reuse. This document records design patterns only; no source files, branding, assets, credentials, endpoint lists, or proprietary operating parameters are copied.

## Product direction

Molla VPN remains a subscription-backed, multi-platform VPN product. The control plane manages users, subscriptions, devices, server inventory and short-lived connection entitlements. Dedicated VPN nodes carry user traffic. Cloudflare protects DNS and public control-plane/API surfaces; it is not treated as a generic VPN traffic relay.

## Reference patterns worth adopting

1. **One platform VPN owner per device.** Android uses one Android `VpnService` and TUN owner. Transport engines attach behind that owner rather than competing to create VPN interfaces.
2. **Transport abstraction.** A common lifecycle contract covers capability discovery, prepare, connect, health, disconnect, cleanup and structured diagnostics. WireGuard is the first production transport; sing-box/Xray-based transports are added behind adapters.
3. **Verified connection state.** A handshake or process start is not a successful connection. Mark connected only after bounded end-to-end traffic checks succeed.
4. **Endpoint health and selection.** Maintain a server registry with region, protocol, capacity, health timestamp, latency sample and operational state. Select only eligible nodes; cache choices with expiry and re-check after network changes.
5. **MTU and network-change handling.** Keep MTU configurable per transport/platform; start conservatively, measure where supported, and handle Wi-Fi/mobile transitions with bounded retries.
6. **Profiles.** Store named, versioned client preferences (region, protocol preference, DNS mode, split-tunnel policy, auto-connect). Never store private keys or bearer tokens in exported profiles.
7. **Privacy and leak protection.** Define DNS routing, IPv4/IPv6 behavior, split tunnelling, kill-switch behavior and protected socket handling explicitly. Test for leaks; do not claim protection before tests pass.
8. **Diagnostics.** Expose connection phase, selected node identifier, latency, byte counters, uptime and sanitized failure reason. Never log private keys, access tokens, full user traffic or sensitive identity material.
9. **Native engine with platform shell.** Keep Flutter as the cross-platform UI where useful, but use native platform VPN APIs and a shared, well-defined engine boundary. Android requires a native `VpnService`; iOS requires Network Extension entitlements and native integration; Windows requires a Windows tunnel/driver integration; macOS uses its supported Network Extension/system-extension model.

## Molla target architecture

```text
Flutter UI (Android / iOS / Windows / macOS)
             |
       typed platform API
             |
   Native VPN lifecycle adapter
   Android VpnService | Apple Network Extension
   Windows tunnel adapter | macOS Network Extension
             |
       Transport manager
       - WireGuard (first)
       - sing-box adapter
       - VLESS + REALITY adapter (later)
       - Hysteria 2 adapter (later)
             |
       verified data-plane health
             |
       dedicated VPN node(s)

Control plane (FastAPI)
  accounts / sessions / subscriptions / devices
  node registry / entitlement issuance / revocation
             |
  Cloudflare DNS + TLS + WAF/API protection
```

## Delivery sequence

### Phase A — control plane foundation
- Persist accounts, sessions, subscriptions and devices.
- Add login/logout/current-user APIs with token revocation and rate limiting.
- Bind each peer to an authenticated account and device.
- Enforce subscription status and device limits server-side.
- Add audit events for login, device enrollment, revocation and entitlement issuance.

### Phase B — Android WireGuard pilot
- Replace the current placeholder Flutter connection control with a platform-channel contract.
- Implement Android `VpnService` lifecycle and a maintained WireGuard userspace/kernel integration.
- Generate client keys on-device; send only public keys to the control plane.
- Issue short-lived, device-bound configuration material from a trusted node registry.
- Add node provisioning and peer reconciliation; revoke peers on device/session entitlement removal.
- Verify DNS, IPv4, IPv6, reconnect, kill switch and real traffic before showing Connected.

### Phase C — quality and resilience
- Add bounded endpoint health checks, latency measurement and MTU configuration.
- Add profiles, network-change recovery, split tunnelling and sanitized diagnostics.
- Add multiple nodes and controlled failover; avoid aggressive unbounded scanning.

### Phase D — additional transports and platforms
- Add sing-box/Xray and Hysteria 2 only after adapter contract, security review and automated integration tests exist.
- Add iOS, Windows and macOS native tunnel adapters independently; a Flutter UI alone does not implement VPN capability.
- Add signed releases, reproducible build checks, SBOM and dependency/license review.

## Non-negotiable security and licensing rules

- Client private keys are generated and retained on the device. The API receives public keys only.
- Never commit server private keys, client configs, real production endpoints, signing keys or tokens.
- No unauthenticated peer creation, profile issuance or server mutation endpoints.
- Server endpoint data comes only from trusted operator-managed inventory, never from client-supplied arbitrary URLs.
- All connection claims require integration tests that prove packets traverse the tunnel.
- The reference repository is AGPL-3.0. Molla must not copy or vendor its code unless the team intentionally accepts the license obligations and separately reviews every bundled dependency's license. Architectural ideas and independently implemented interfaces are the intended reference.

## Current limitations

This document does not mean any server is provisioned, any transport is active, any platform VPN adapter exists, or any production connection has passed. Those statuses change only after implementation and recorded test evidence.
