# Authentication primitives

- Password hashes use scrypt with a fresh random 16-byte salt and fixed work parameters.
- Access tokens use HMAC-SHA256 and validate issuer, audience, token type, issue time and expiry.
- Access tokens expire after 15 minutes by default; configurable lifetime is restricted to 1–60 minutes.
- Signing keys must contain at least 32 bytes.

## Not yet production authentication

These functions are not mounted as API routes and do not create accounts or sessions. Before enabling login or device endpoints, add persistent user/account records, administrator-controlled account creation, login throttling and lockout, token revocation/session tracking, MFA/recovery policy, audit logging, HTTPS enforcement and deliberate secret rotation. Keep the signing key in deployment secrets/environment, never source control.

This is a narrowly scoped access-token primitive, not a complete OAuth/OIDC implementation. Do not trust external issuers or reuse this key for other purposes.
