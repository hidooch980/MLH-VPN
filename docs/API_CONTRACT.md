# API Contract v0.2

Status: authenticated pilot-control-plane routes implemented behind explicit runtime signing-key configuration. Not a production VPN service.

## System

### GET /health
Returns service health, stage and whether authenticated routes are enabled. It does not expose secrets, environment variables or hostnames.

### GET /api/v1/protocols
Returns protocol metadata only. Every protocol remains disabled until its data plane is deployed and verified.

## Runtime configuration

Authenticated routes are mounted only when `MOLLAVPN_TOKEN_SECRET` is configured with at least 32 UTF-8 bytes. Never commit this secret. The SQLite database path can be set with `MOLLAVPN_DB_PATH` (default `data/molla-vpn.sqlite3`). The pilot IPv4 pool and reserved server address can be set with `MOLLAVPN_DEVICE_NETWORK` and `MOLLAVPN_SERVER_ADDRESS`.

If the signing secret is absent or too short, account/device routes are not mounted. This is fail-closed. The current implementation uses short-lived HMAC-SHA256 bearer tokens and SQLite-backed revocable sessions. Use TLS at the deployment edge.

## Implemented account routes

All routes below require the authenticated API to be enabled.

| Method | Route | Purpose |
|---|---|---|
| POST | /api/v1/auth/login | Authenticate an existing, administrator-provisioned account |
| GET | /api/v1/auth/me | Read current account and subscription entitlement |
| POST | /api/v1/auth/logout | Revoke current bearer-token session |
| GET | /api/v1/devices | List only the current account's devices |
| POST | /api/v1/devices | Enroll a device using its client-generated WireGuard public key |
| DELETE | /api/v1/devices/{device_id} | Revoke a device belonging to the current account |

Login rate limiting is persisted in SQLite: eight failed attempts within five minutes trigger a 15-minute block for the email/IP key. The limit is deliberately applied without trusting forwarded IP headers.

## Deliberately not implemented

- Public registration and email verification
- Administrator bootstrap and privileged account-management API
- MFA, recovery, audit-event pipeline and signing-key rotation
- Payment/billing integration
- Server inventory and health API
- Client configuration/profile issuance
- Live WireGuard peer reconciliation or tunnel termination on revocation
- Native VPN adapters and production traffic routing

Device enrollment stores a public key and allocates a private-network IPv4 address. It does not issue a VPN configuration or activate a peer on a server. Revocation currently changes registry state only.

## Security requirements before production

- Use TLS, secure deployment secrets, key rotation and a managed database/backup strategy.
- Add verified account ownership, admin MFA/bootstrap, recovery and audited support operations.
- Apply rate limits to all sensitive operations and add abuse monitoring.
- Reconcile device/peer state with live nodes and prove revocation stops traffic.
- Never return server private keys or other users' device data.
- Enforce subscription/device limits server-side.
- Add pagination, request IDs and structured redacted logs.
- Complete data-retention, deletion, privacy and legal review.
