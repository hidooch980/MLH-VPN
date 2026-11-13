# Roadmap

## Phase 0 — repository foundation
- [x] Confirm repository access and create isolated development branch.
- [x] Add product README, architecture boundaries and protocol decisions.
- [x] Add backend, client and infrastructure CI.
- [x] Add API contract, protocol evaluation and WireGuard pilot design.
- [x] Merge reviewed foundation into `main`.
- [x] Add dependency audit, Bandit, CodeQL and Dependabot.
- [x] Pin GitHub Actions to immutable commit SHAs.
- [ ] Enable branch protection and required status checks.
- [ ] Decide repository license and review third-party obligations.
- [ ] Review public-repository secret scanning and exposure settings.

## Phase 1 — protocol and client foundation
- [x] Create Flutter disconnected-state client shell.
- [x] Define platform/tunnel adapter boundaries and MSN-GUARD architecture lessons.
- [x] Approve six-protocol product strategy: WireGuard, VLESS + REALITY, Hysteria 2, TUIC, AmneziaWG and Shadowsocks 2022.
- [x] Enable automatic protocol selection in product policy (runtime remains disabled).
- [x] Define protocol catalog metadata and smart-selection safety contract.
- [ ] Add native Android runner and real WireGuard tunnel adapter.
- [ ] Validate sing-box versions, platform support, license and packaging for Hysteria 2, TUIC and Shadowsocks 2022.
- [ ] Integrate and test VLESS + REALITY using a reviewed Xray-compatible stack.
- [ ] Integrate and test a compatible AmneziaWG client/server adapter.
- [ ] Implement selector in shadow mode with deterministic scoring, hysteresis and manual override.
- [ ] Demonstrate connect, disconnect, reconnect, protocol failover and kill-switch on a real node.
- [ ] Add DNS/IPv4/IPv6 leak and network-transition tests.
- [ ] Add independent iOS, Windows and macOS tunnel adapters and platform CI.

## Phase 2 — control plane
- [x] Define API contract and security boundaries.
- [x] Add password hashing and short-lived access-token primitives.
- [x] Add persistent account, session and subscription storage.
- [x] Add session revocation, account suspension and subscription expiry checks.
- [x] Add account-owned device enrollment, device limits and transactional IPv4 allocation.
- [x] Add authenticated login, current-account, logout and device routes behind runtime signing-secret configuration.
- [x] Add persistent SQLite login throttling.
- [ ] Add verified registration, admin bootstrap, MFA/recovery and signing-key rotation.
- [ ] Add audit events, operational RBAC and support/admin management.
- [ ] Add one-time device enrollment challenge and short-lived configuration issuance.
- [ ] Add server catalog, health checks, protocol capability advertisement and live peer reconciliation.
- [ ] Add selection-policy API, opt-in health telemetry and privacy/retention controls.

## Phase 3 — server fleet
- [x] Add guarded Ansible WireGuard host-preparation template.
- [ ] Select hosting provider, regions, budget and expected concurrency.
- [ ] Provision hardened Linux nodes after explicit deployment review.
- [ ] Add server-side services for each approved protocol, firewall policy, IPv6 policy and DNS.
- [ ] Add live peer reconciliation and prove revocation terminates traffic for each protocol.
- [ ] Add monitoring, alerting, backups, patching, key rotation and restore drills.
- [ ] Complete load, failover and abuse-resilience testing across protocols.

## Phase 4 — limited pilot
- [ ] Internal test cohort with real end-to-end connection evidence.
- [ ] Enable automatic selection in shadow mode, then a small opt-in cohort after acceptance.
- [ ] Independent security review and penetration test of every enabled protocol path.
- [ ] Signed Android/desktop packages and controlled update channels.
- [ ] Billing, renewals, cancellation/refunds and support operations.
- [ ] Privacy/legal review and data-retention controls.
- [ ] Release only after every advertised platform/protocol pair passes its acceptance gates.

## Release gates

No production claim until a real client tunnel has passed end-to-end tests on every advertised platform and protocol. A catalog entry is not proof of availability. Automatic selection must not activate until candidate filtering, kill-switch continuity, hysteresis, fallback and leak tests pass. No subscription sale until entitlement enforcement, abuse controls, billing, refund/cancellation, support, privacy and applicable legal requirements are reviewed.

See [the current audit](AUDIT_2026-11-13.md), [architecture](ARCHITECTURE.md), [protocol strategy](PROTOCOLS.md) and [API contract](API_CONTRACT.md).
