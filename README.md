# Molla VPN

Molla VPN is an independent, cross-platform VPN product for personal and commercial subscriptions.

## Product scope

- Clients: Android, iOS, Windows, macOS
- Control plane: authentication, devices, subscriptions, server inventory, health and policy APIs
- Data plane: dedicated VPN servers; Cloudflare is used for DNS, API protection and control-plane delivery, not as a generic raw VPN traffic relay
- Initial protocol targets: WireGuard, sing-box-managed protocols, VLESS + REALITY, and Hysteria 2

## Project status

**Bootstrap stage — not production-ready.** No VPN server, client tunnel, subscription billing, or Cloudflare resource has been deployed yet. Do not use this repository as evidence of a live VPN service.

## Repository structure

- `apps/client/`: Flutter client shell (no real tunnel adapter yet)
- `backend/`: FastAPI control-plane scaffold
- `docs/`: architecture, protocol evaluation, API contract, roadmap and decisions
- `.github/workflows/`: backend and client CI

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md), [docs/API_CONTRACT.md](docs/API_CONTRACT.md), and [docs/ROADMAP.md](docs/ROADMAP.md).

## Security

Never commit credentials, private keys, subscription tokens, server addresses that are intended to remain private, or production user data. Use GitHub Actions secrets and a managed secret store.

## License

Not selected yet. All rights reserved unless a license is added explicitly.
