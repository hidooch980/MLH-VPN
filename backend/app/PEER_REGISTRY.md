# Peer registry

The SQLite peer registry is an internal domain component, not an exposed API.

- Stores WireGuard **public keys only** and a unique device/IP mapping.
- Validates 32-byte canonical base64 public keys.
- Prevents duplicate device IDs, public keys and assigned addresses.
- Restricts peer addresses to the configured IPv4 subnet and reserves the server/network/broadcast addresses.
- Supports idempotent revocation in the registry.

## Important security boundary

This component does not authenticate callers, generate client private keys, create downloadable tunnel profiles, or apply peers to a live WireGuard interface. Do not expose it through HTTP until account authentication, device authorization, rate limits, audit logging and an authenticated provisioning adapter are implemented. A registry status of `revoked` does not yet revoke a live kernel peer.
