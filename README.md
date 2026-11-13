# Molla VPN

Molla VPN is an independent, cross-platform VPN product for personal and commercial subscriptions.

## Standalone Android client

The Android implementation now has a pinned v2rayNG 2.3.15 build overlay with the
upstream protocol/import/VPN implementation, branded Molla VPN. It requires **no
Molla backend or newly provisioned server**: import an existing VPN provider's
configuration or subscription. A remote VPN/proxy endpoint is still necessary.

See [Android build/source](apps/android/README.md), [Persian installation guide](apps/android/INSTALL.fa.md)
and the **Standalone Android APK** workflow for installable development builds.
The generated Android derivative and overlay are GPL-3.0; upstream notices are retained.
Actual connectivity and leak testing require an Android device and valid endpoints.

## Original hosted-service scope

- Clients: Android, iOS, Windows, macOS
- Control plane: authentication, devices, subscriptions, server inventory, health and policy APIs
- Data plane: dedicated VPN servers; Cloudflare is used for DNS, API protection and control-plane delivery, not as a generic raw VPN traffic relay
- Initial protocol targets: WireGuard, sing-box-managed protocols, VLESS + REALITY, and Hysteria 2

## Project status

**Hosted service: development foundation, not production-ready.** The development foundation has been merged into `main`, but the repository does not yet contain a live VPN node, a native tunnel adapter, customer configuration issuance, billing, or verified end-to-end VPN traffic. Protocols remain disabled and the client connection control remains unavailable.

## Repository structure

- `apps/android/`: standalone native Android build overlay, pinned upstream sources, licensing and install guide
- `apps/client/`: legacy Flutter client shell; separate from the native Android build
- `backend/`: FastAPI scaffold, authentication primitives, persistent account/session/subscription store and account-scoped device registry
- `infra/ansible/`: guarded WireGuard server provisioning template; not executed against a production node
- `docs/`: architecture, reference analysis, API contract, audit, roadmap and deployment gates
- `.github/workflows/`: backend, client, infrastructure, dependency/static security and CodeQL CI
- `.github/dependabot.yml`: automated dependency update proposals

## CI and security

CI checks source/tests and infrastructure syntax only. Security automation includes Python dependency auditing, Bandit static checks and CodeQL analysis. A green workflow does not establish that a VPN tunnel is live or production-secure. Review every dependency update and security finding before merge.

## Security

Never commit credentials, private keys, subscription tokens, real production endpoints intended to remain private, or production user data. Use deployment secrets and a managed secret store. Client private keys must be generated and retained on client devices; only public keys may be sent to the control plane.

## Licensing

The standalone Android derivative and overlay use GPL-3.0 (see `apps/android/LICENSE`). A license for the unrelated original project components has not been selected. Do not import code from the AGPL-3.0 MSN-GUARD reference project unless the project explicitly adopts the applicable license obligations after legal review. All rights reserved unless a license is added explicitly.

See [the current audit](docs/AUDIT_2026-11-13.md), [architecture](docs/ARCHITECTURE.md), [API contract](docs/API_CONTRACT.md), and [roadmap](docs/ROADMAP.md).
