# Protocol Evaluation Matrix

This is a plan, not a claim of implemented support.

| Candidate | Strengths to test | Risks / validation |
|---|---|---|
| WireGuard | Small codebase, efficient cryptography, low overhead | UDP reachability, roaming, key lifecycle, native OS integration |
| sing-box | Unified routing and multiple outbound/inbound types | Version pinning, platform packaging, supported features by OS |
| VLESS + REALITY | Flexible proxy deployment | Xray interoperability, configuration security, operational complexity |
| Hysteria 2 | QUIC transport and congestion-control options | UDP availability, loss/latency behavior, fair-use controls |
| TUIC | QUIC-based alternative | Defer until comparative benchmarks justify it |

## Acceptance tests
- Functional: handshake, traffic forwarding, DNS handling, reconnect and revocation.
- Network: Wi-Fi/mobile handoff, captive portals, UDP-blocked networks, MTU and IPv6.
- Security: no plaintext DNS leak, no traffic after kill switch activation, key rotation, replay and downgrade checks.
- Performance: median and p95 latency, sustained throughput, packet loss, CPU and battery.
- Compatibility: supported OS versions and hardware architectures.

A protocol is enabled in production only after passing the test matrix on the relevant client and server versions.
