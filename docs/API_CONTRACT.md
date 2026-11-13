# API Contract v0.1

Status: draft scaffold contract. Endpoints are not production-ready.

## System

### GET /health
Returns service health and deployment stage. This endpoint must not expose secrets, environment variables, hostnames, or internal diagnostics.

## Protocol catalog

### GET /api/v1/protocols
Returns protocol metadata only. An item with `enabled: false` is not provisioned and must not be offered as a usable VPN connection.

Example:

```json
{
  "items": [
    {"id": "wireguard", "name": "WireGuard", "status": "planned", "enabled": false},
    {"id": "sing-box", "name": "sing-box", "status": "planned", "enabled": false},
    {"id": "vless-reality", "name": "VLESS + REALITY", "status": "planned", "enabled": false},
    {"id": "hysteria2", "name": "Hysteria 2", "status": "planned", "enabled": false}
  ]
}
```

## Planned authenticated API surface

These routes are design targets only; do not assume they exist until implemented and tested.

| Method | Route | Purpose |
|---|---|---|
| POST | /api/v1/auth/register | Create an account with verified contact |
| POST | /api/v1/auth/session | Create a short-lived authenticated session |
| DELETE | /api/v1/auth/session | Revoke current session |
| GET | /api/v1/me | Read current account and entitlement summary |
| GET | /api/v1/devices | List enrolled devices |
| POST | /api/v1/devices/enroll | Enroll a device using a one-time challenge |
| DELETE | /api/v1/devices/{device_id} | Revoke a device |
| GET | /api/v1/servers | List eligible, healthy server capabilities |
| POST | /api/v1/configurations | Issue a short-lived client configuration |
| GET | /api/v1/subscription | Read subscription entitlement |

## Security requirements before implementation

- Use TLS, secure session cookies or carefully scoped bearer tokens, CSRF protection where cookies are used, rate limits and abuse controls.
- Require verified account ownership before issuing a device configuration.
- Never return server private keys or other users' device data.
- Make configuration issuance idempotent where possible and auditable.
- Enforce subscription/device limits on the server, not in the client.
- Support immediate session, device and configuration revocation.
- Use generic authentication errors to reduce account enumeration.
- Add schema validation, pagination, request IDs and structured redacted logs.
- Document data retention, deletion and lawful requests before pilot launch.
