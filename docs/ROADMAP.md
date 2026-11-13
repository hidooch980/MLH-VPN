# Roadmap

## Phase 0 — repository foundation
- [x] Confirm repository is empty and accessible.
- [x] Add product README and isolated development branch.
- [x] Record architecture boundaries and initial protocol choices.
- [ ] Add CI lint/build checks after application framework is selected.
- [ ] Decide repository visibility and license.

## Phase 1 — technical proof of concept
- [ ] Flutter shell builds for Android, iOS, Windows and macOS.
- [ ] Evaluate WireGuard integration on each OS using supported native APIs.
- [ ] Evaluate sing-box integration and packaging/licensing constraints.
- [ ] Demonstrate connect, disconnect, reconnect and kill-switch behavior.
- [ ] Automated DNS/IPv4/IPv6 leak and network-transition tests.

## Phase 2 — control plane
- [ ] Define API contract and threat model.
- [ ] Implement authentication, device enrollment and revocation.
- [ ] Implement server catalog and health checks.
- [ ] Implement subscription entitlements and admin RBAC.
- [ ] Add audit logging and privacy/retention controls.

## Phase 3 — server fleet
- [ ] Select regions and providers.
- [ ] Provision hardened Linux nodes using infrastructure-as-code.
- [ ] Deploy WireGuard and selected proxy transports.
- [ ] Add monitoring, alerting, backups, patching and key rotation.
- [ ] Load, failover and abuse-resilience testing.

## Phase 4 — limited pilot
- [ ] Internal test cohort.
- [ ] Security review and independent penetration test.
- [ ] Store packaging, signing and update channels.
- [ ] Billing integration and support operations.
- [ ] Release only after acceptance criteria pass.

## Release gates
No production claim until a real client tunnel has passed end-to-end tests on each advertised platform. No subscription sale until entitlement enforcement, refunds/cancellation, support and applicable legal requirements are reviewed.
