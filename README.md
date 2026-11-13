# Molla VPN

Molla VPN is an independent, cross-platform VPN product for personal and commercial subscriptions.

## Product scope

- Clients: Android, iOS, Windows, macOS
- Control plane: authentication, devices, subscriptions, server inventory, health and policy APIs
- Data plane: dedicated VPN servers; Cloudflare is used for DNS, API protection and control-plane delivery, not as a generic raw VPN traffic relay
- Approved protocol strategy: WireGuard, VLESS + REALITY, Hysteria 2, TUIC, AmneziaWG and Shadowsocks 2022
- Smart protocol selection: enabled as the intended product policy; runtime activation is gated on real, verified protocol adapters and healthy server nodes
- sing-box is an integration/orchestration engine for selected protocols, not a seventh protocol

## Project status

**Development foundation — not production-ready and not yet a usable VPN service.** The development foundation has been merged into `main`, but the repository does not yet contain a live VPN node, native tunnel adapters, customer configuration issuance, billing, or verified end-to-end VPN traffic. All six protocol catalog entries remain planned and disabled. Automatic selection is configured in policy but not operational. The client connection control remains unavailable.

## Repository structure

- `apps/client/`: Flutter client shell; native VPN adapters are not implemented
- `backend/`: FastAPI scaffold, authentication primitives, persistent account/session/subscription store, account-scoped device registry and public protocol policy catalog
- `infra/ansible/`: guarded WireGuard server provisioning template; not executed against a production node
- `docs/`: architecture, reference analysis, API contract, protocol strategy, audit, roadmap and deployment gates
- `.github/workflows/`: backend, client, infrastructure, dependency/static security and CodeQL CI
- `.github/dependabot.yml`: automated dependency update proposals

## CI and security

CI checks source/tests and infrastructure syntax only. Security automation includes Python dependency auditing, Bandit static checks and CodeQL analysis. A green workflow does not establish that a VPN tunnel is live or production-secure. Review every dependency update and security finding before merge.

## Security

Never commit credentials, private keys, subscription tokens, real production endpoints intended to remain private, or production user data. Use deployment secrets and a managed secret store. Client private keys must be generated and retained on client devices; only public keys may be sent to the control plane. Smart selection must preserve the OS kill switch and never downgrade encryption to force connectivity.

## Licensing

The project license has not been selected. Before integrating sing-box, Xray-compatible components, AmneziaWG or other third-party engines, review their exact licenses, notices, distribution obligations and platform packaging terms. Do not import code from the AGPL-3.0 MSN-GUARD reference project unless the project explicitly adopts the applicable license obligations after legal review. All rights reserved unless a license is added explicitly.

See [the current audit](docs/AUDIT_2026-11-13.md), [architecture](docs/ARCHITECTURE.md), [protocol strategy](docs/PROTOCOLS.md), [API contract](docs/API_CONTRACT.md), and [roadmap](docs/ROADMAP.md).
