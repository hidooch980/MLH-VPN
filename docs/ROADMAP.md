# Roadmap

## Phase 0 — repository foundation
- [x] Confirm repository is accessible and create an isolated development branch.
- [x] Add product README, architecture boundaries and initial protocol decisions.
- [x] Add backend, client and infrastructure CI workflows.
- [x] Add API contract, protocol evaluation and WireGuard pilot design.
- [x] Merge reviewed foundation into `main` after backend, client and Ansible CI passed.
- [x] Add Python dependency audit, Bandit static checks, CodeQL and Dependabot configuration.
- [ ] Enable branch protection and required status checks.
- [ ] Decide repository license and confirm third-party dependency obligations.
- [ ] Review public repository exposure and enable/verify secret-scanning settings.

## Phase 1 — technical proof of concept
- [x] Create Flutter client shell with honest disconnected state.
- [x] Define platform/tunnel adapter boundaries and MSN-GUARD architecture lessons.
- [ ] Add native Android runner and a real WireGuard tunnel adapter.
- [ ] Evaluate sing-box integration and packaging/licensing constraints.
- [ ] Demonstrate connect, disconnect, reconnect and kill-switch behavior on a real node.
- [ ] Add automated DNS/IPv4/IPv6 leak and network-transition tests.
- [ ] Add independent iOS, Windows and macOS tunnel adapters and platform CI.

## Phase 2 — control plane
- [x] Define API contract and initial security boundaries.
- [x] Add password hashing and short-lived access-token primitives.
- [x] Add persistent account, session and subscription storage primitives.
- [x] Add session revocation, account suspension and subscription expiry checks.
- [x] Add account-scoped device enrollment, subscription device-limit enforcement and transactional IPv4 allocation.
- [ ] Add verified registration, login/logout/current-user APIs and persistent rate limiting.
- [ ] Add admin bootstrap, MFA/recovery policy, signing-key rotation and audit events.
- [ ] Add authenticated device enrollment, configuration issuance and revocation APIs.
- [ ] Add server catalog, health checks, pagination and operational RBAC.

## Phase 3 — server fleet
- [x] Add an Ansible WireGuard host-preparation template with secret-safe key generation and unmanaged-config guard.
- [ ] Select hosting provider, regions, budget and expected concurrent users.
- [ ] Provision hardened Linux nodes after explicit deployment review.
- [ ] Add live peer reconciliation, firewall policy, IPv6 policy and DNS.
- [ ] Add monitoring, alerting, backups, patching, key rotation and restore drills.
- [ ] Complete load, failover and abuse-resilience testing.

## Phase 4 — limited pilot
- [ ] Internal test cohort with real end-to-end connection evidence.
- [ ] Security review and independent penetration test.
- [ ] Signed Android/desktop packages and controlled update channels.
- [ ] Billing, renewals, cancellation/refunds and support operations.
- [ ] Privacy/legal review and data-retention controls.
- [ ] Release only after every platform-specific acceptance gate passes.

## Release gates

No production claim until a real client tunnel has passed end-to-end tests on each advertised platform. No subscription sale until entitlement enforcement, abuse controls, billing, refund/cancellation, support, privacy and applicable legal requirements are reviewed.

See [the repository audit](AUDIT_2026-11-13.md) for current evidence and blockers.
