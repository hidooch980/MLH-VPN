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

## Phase 1 — technical proof of concept
- [x] Create Flutter disconnected-state client shell.
- [x] Define platform/tunnel adapter boundaries and MSN-GUARD architecture lessons.
- [ ] Add native Android runner and real WireGuard tunnel adapter.
- [ ] Evaluate sing-box integration and packaging/licensing.
- [ ] Demonstrate connect, disconnect, reconnect and kill-switch on a real node.
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
- [ ] Add server catalog, health checks and live peer reconciliation.

## Phase 3 — server fleet
- [x] Add guarded Ansible WireGuard host-preparation template.
- [ ] Select hosting provider, regions, budget and expected concurrency.
- [ ] Provision hardened Linux nodes after explicit deployment review.
- [ ] Add live peer reconciliation, firewall policy, IPv6 policy and DNS.
- [ ] Add monitoring, alerting, backups, patching, key rotation and restore drills.
- [ ] Complete load, failover and abuse-resilience testing.

## Phase 4 — limited pilot
- [ ] Internal test cohort with real end-to-end connection evidence.
- [ ] Independent security review and penetration test.
- [ ] Signed Android/desktop packages and controlled update channels.
- [ ] Billing, renewals, cancellation/refunds and support operations.
- [ ] Privacy/legal review and data-retention controls.
- [ ] Release only after every advertised platform passes its acceptance gates.

## Release gates

No production claim until a real client tunnel has passed end-to-end tests on every advertised platform. No subscription sale until entitlement enforcement, abuse controls, billing, refund/cancellation, support, privacy and applicable legal requirements are reviewed.

See [the repository audit](AUDIT_2026-11-13.md) and [API contract](API_CONTRACT.md).
