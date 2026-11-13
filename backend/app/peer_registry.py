"""Persistent, credential-minimal registry for WireGuard client peers.

This module stores public keys only. It deliberately does not expose HTTP routes,
generate client private keys, or apply changes to a live WireGuard interface.
"""
from __future__ import annotations

import base64
import binascii
import ipaddress
import sqlite3
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path


class PeerRegistryError(ValueError):
    """Raised when a peer cannot be safely registered or changed."""


@dataclass(frozen=True)
class Peer:
    device_id: str
    public_key: str
    assigned_ip: str
    status: str
    created_at: str
    revoked_at: str | None


def _validate_public_key(public_key: str) -> str:
    if not isinstance(public_key, str) or not public_key or public_key.strip() != public_key:
        raise PeerRegistryError("A canonical WireGuard public key is required.")
    try:
        decoded = base64.b64decode(public_key, validate=True)
    except (binascii.Error, ValueError) as exc:
        raise PeerRegistryError("Public key must be valid base64.") from exc
    if len(decoded) != 32 or base64.b64encode(decoded).decode("ascii") != public_key:
        raise PeerRegistryError("WireGuard public key must encode exactly 32 bytes.")
    return public_key


class PeerRegistry:
    """SQLite registry for device peers; no private key material is accepted."""

    def __init__(self, database_path: str | Path, network: str = "10.66.0.0/24"):
        self.database_path = str(database_path)
        self.network = ipaddress.ip_network(network, strict=True)
        if self.network.version != 4 or self.network.num_addresses < 4:
            raise PeerRegistryError("The pilot registry requires an IPv4 subnet of at least four addresses.")
        self._initialize()

    def _connect(self) -> sqlite3.Connection:
        connection = sqlite3.connect(self.database_path, timeout=10)
        connection.row_factory = sqlite3.Row
        connection.execute("PRAGMA foreign_keys = ON")
        return connection

    def _initialize(self) -> None:
        Path(self.database_path).parent.mkdir(parents=True, exist_ok=True)
        with self._connect() as connection:
            connection.execute(
                """
                CREATE TABLE IF NOT EXISTS wireguard_peers (
                    device_id TEXT PRIMARY KEY,
                    public_key TEXT NOT NULL UNIQUE,
                    assigned_ip TEXT NOT NULL UNIQUE,
                    status TEXT NOT NULL CHECK (status IN ('active', 'revoked')),
                    created_at TEXT NOT NULL,
                    revoked_at TEXT
                )
                """
            )

    def register(self, device_id: str, public_key: str, assigned_ip: str) -> Peer:
        if not isinstance(device_id, str) or not device_id or len(device_id) > 128:
            raise PeerRegistryError("Device ID must contain 1 to 128 characters.")
        key = _validate_public_key(public_key)
        try:
            address = ipaddress.ip_address(assigned_ip)
        except ValueError as exc:
            raise PeerRegistryError("A valid IPv4 peer address is required.") from exc
        if address.version != 4 or address not in self.network:
            raise PeerRegistryError("Peer address must belong to the configured VPN subnet.")
        if address in (self.network.network_address, self.network.broadcast_address):
            raise PeerRegistryError("Network and broadcast addresses cannot be assigned to peers.")
        if address == next(self.network.hosts()):
            raise PeerRegistryError("The VPN server address is reserved.")

        now = datetime.now(timezone.utc).isoformat()
        try:
            with self._connect() as connection:
                connection.execute(
                    """
                    INSERT INTO wireguard_peers
                    (device_id, public_key, assigned_ip, status, created_at, revoked_at)
                    VALUES (?, ?, ?, 'active', ?, NULL)
                    """,
                    (device_id, key, str(address), now),
                )
                row = connection.execute(
                    "SELECT * FROM wireguard_peers WHERE device_id = ?", (device_id,)
                ).fetchone()
        except sqlite3.IntegrityError as exc:
            raise PeerRegistryError("Device, public key, or assigned IP is already registered.") from exc
        return self._to_peer(row)

    def revoke(self, device_id: str) -> Peer:
        now = datetime.now(timezone.utc).isoformat()
        with self._connect() as connection:
            cursor = connection.execute(
                """
                UPDATE wireguard_peers
                SET status = 'revoked', revoked_at = COALESCE(revoked_at, ?)
                WHERE device_id = ?
                """,
                (now, device_id),
            )
            if cursor.rowcount == 0:
                raise PeerRegistryError("Device peer was not found.")
            row = connection.execute(
                "SELECT * FROM wireguard_peers WHERE device_id = ?", (device_id,)
            ).fetchone()
        return self._to_peer(row)

    def get(self, device_id: str) -> Peer | None:
        with self._connect() as connection:
            row = connection.execute(
                "SELECT * FROM wireguard_peers WHERE device_id = ?", (device_id,)
            ).fetchone()
        return self._to_peer(row) if row else None

    def list_active(self) -> list[Peer]:
        with self._connect() as connection:
            rows = connection.execute(
                "SELECT * FROM wireguard_peers WHERE status = 'active' ORDER BY created_at"
            ).fetchall()
        return [self._to_peer(row) for row in rows]

    @staticmethod
    def _to_peer(row: sqlite3.Row) -> Peer:
        return Peer(
            device_id=row["device_id"],
            public_key=row["public_key"],
            assigned_ip=row["assigned_ip"],
            status=row["status"],
            created_at=row["created_at"],
            revoked_at=row["revoked_at"],
        )
