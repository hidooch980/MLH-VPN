# Authentication and account API

## Implemented

- Password hashes use scrypt with a fresh random 16-byte salt and fixed work parameters.
- Access tokens use HMAC-SHA256 and validate issuer, audience, token type, issue time and expiry.
- Access tokens expire after 15 minutes by default; codec lifetime is restricted to 1–60 minutes.
- SQLite sessions persist token IDs, expiry and revocation state; bearer tokens themselves are not stored.
- Account suspension revokes sessions and invalidates any remaining session checks.
- Login failures are tracked in SQLite: eight failures in five minutes block the email/IP key for 15 minutes.
- Authenticated routes cover login, current account, logout and account-owned device list/enroll/revoke.
- Device enrollment checks active account/subscription and device limit in a transaction. Only client public keys are accepted.

## Enable in a deployment

Set `MOLLAVPN_TOKEN_SECRET` to a random secret containing at least 32 UTF-8 bytes and keep it in the deployment secret store. Do not commit it or print it in logs. Optionally set `MOLLAVPN_DB_PATH`, `MOLLAVPN_DEVICE_NETWORK` and `MOLLAVPN_SERVER_ADDRESS`. If the signing secret is missing or too short, authenticated routes are not mounted.

Place the API behind HTTPS/TLS. Restrict CORS origins to actual application origins before production. The default origins are localhost development origins only.

## Deliberate limitations

- There is no public registration endpoint. Accounts must be provisioned through a controlled administrative process.
- Admin bootstrap, role-management APIs, MFA, account recovery and email verification are not implemented.
- There is no token refresh or signing-key rotation workflow.
- No billing provider or automatic subscription webhook exists.
- Device revocation is currently database-only; it does not remove a peer from a running WireGuard interface.
- No VPN configuration/profile is issued by these endpoints.

This is a pilot control-plane implementation, not a complete OAuth/OIDC server and not production-ready authentication. Do not expose it commercially until independent security review, abuse controls, recovery, operational audit, secret rotation and live peer reconciliation are complete.
