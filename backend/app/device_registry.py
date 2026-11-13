"""Account-owned device enrollment and WireGuard public-key registry.

This domain component enforces account status, active subscription and device limits.
It stores client public keys only; it never accepts or stores client private keys and
does not apply peers to a running WireGuard interface.
"""
from __future__ import annotations

import base64
import binascii
import ipaddress
import os
import secrets
import sqlite3
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path


class DeviceRegistryError(ValueError):
    """Raised when device enrollment or revocation is not permitted."""


@dataclass(frozen=True)
class Device:
    device_id: str
    user_id: str
    device_name: str
    platform: str
    public_key: str
    assigned_ip: str
    status: str
    created_at: str
    revoked_at: str | None


def _validate_public_key(public_key: str) -> str:
    if not isinstance(public_key, str) or not public_key or public_key.strip() != public_key:
        raise DeviceRegistryError("A canonical WireGuard public key is required.")
    try:
        raw = base64.b64decode(public_key, validate=True)
    except (binascii.Error, ValueError) as exc:
        raise DeviceRegistryError("Public key must be valid base64.") from exc
    if len(raw) != 32 or base64.b64encode(raw).decode("ascii") != public_key:
        raise DeviceRegistryError("WireGuard public key must encode exactly 32 bytes.")
    return public_key


class DeviceRegistry:
    """SQLite device registry sharing the account database and transaction boundary."""

    PLATFORMS = frozenset({"android", "ios", "windows", "macos"})

    def __init__(self, database_path: str | Path, network: str = "10.66.0.0/24",
                 server_address: str = "10.66.0.1"):
        self.database_path = str(database_path)
        self.network = ipaddress.ip_network(network, strict=True)
        self.server_address = ipaddress.ip_address(server_address)
        if (self.network.version != 4 or self.network.num_addresses < 4
                or self.server_address.version != 4 or self.server_address not in self.network
                or self.server_address in (self.network.network_address, self.network.broadcast_address)):
            raise DeviceRegistryError("Invalid IPv4 device pool or reserved server address.")
        path = Path(self.database_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        self._initialize()
        os.chmod(path, 0o600)

    def _connect(self) -> sqlite3.Connection:
        db = sqlite3.connect(self.database_path, timeout=10, isolation_level=None)
        db.row_factory = sqlite3.Row
        db.execute("PRAGMA foreign_keys = ON")
        db.execute("PRAGMA busy_timeout = 10000")
        return db

    def _initialize(self) -> None:
        with self._connect() as db:
            db.execute("""
                CREATE TABLE IF NOT EXISTS devices (
                    device_id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL REFERENCES accounts(user_id),
                    device_name TEXT NOT NULL,
                    platform TEXT NOT NULL CHECK(platform IN ('android','ios','windows','macos')),
                    public_key TEXT NOT NULL UNIQUE,
                    assigned_ip TEXT NOT NULL,
                    status TEXT NOT NULL CHECK(status IN ('active','revoked')),
                    created_at TEXT NOT NULL,
                    revoked_at TEXT
                )
            """)
            db.execute("CREATE INDEX IF NOT EXISTS devices_user_status_idx ON devices(user_id,status)")
            db.execute("CREATE UNIQUE INDEX IF NOT EXISTS devices_active_ip_unique ON devices(assigned_ip) WHERE status = 'active'")

    def _allocate_ip(self, db: sqlite3.Connection) -> str:
        used = {
            row["assigned_ip"] for row in db.execute(
                "SELECT assigned_ip FROM devices WHERE status = 'active'"
            ).fetchall()
        }
        for address in self.network.hosts():
            if address != self.server_address and str(address) not in used:
                return str(address)
        raise DeviceRegistryError("No VPN addresses are available in the configured pool.")

    @staticmethod
    def _subscription_is_active(row: sqlite3.Row | None, now: datetime) -> bool:
        if row is None or row["status"] not in ("trial", "active"):
            return False
        if row["expires_at"] is None:
            return True
        try:
            expiry = datetime.fromisoformat(row["expires_at"].replace("Z", "+00:00"))
        except (TypeError, ValueError):
            return False
        if expiry.tzinfo is None or expiry.utcoffset() is None:
            return False
        return expiry.astimezone(timezone.utc) > now

    def enroll(self, user_id: str, device_name: str, platform: str,
               public_key: str) -> Device:
        if not isinstance(device_name, str) or not device_name.strip() or len(device_name.strip()) > 80:
            raise DeviceRegistryError("Device name must contain 1 to 80 characters.")
        if platform not in self.PLATFORMS:
            raise DeviceRegistryError("Unsupported device platform.")
        key = _validate_public_key(public_key)
        device_id = secrets.token_urlsafe(18)
        now = datetime.now(timezone.utc)
        now_iso = now.isoformat()
        db = self._connect()
        try:
            db.execute("BEGIN IMMEDIATE")
            account = db.execute(
                "SELECT status FROM accounts WHERE user_id = ?", (user_id,)
            ).fetchone()
            if account is None or account["status"] != "active":
                raise DeviceRegistryError("Active account not found.")
            subscription = db.execute(
                "SELECT status,expires_at,max_devices FROM subscriptions WHERE user_id = ?",
                (user_id,),
            ).fetchone()
            if not self._subscription_is_active(subscription, now):
                raise DeviceRegistryError("An active subscription is required.")
            active_count = db.execute(
                "SELECT COUNT(*) AS count FROM devices WHERE user_id = ? AND status = 'active'",
                (user_id,),
            ).fetchone()["count"]
            if active_count >= subscription["max_devices"]:
                raise DeviceRegistryError("The subscription device limit has been reached.")
            assigned_ip = self._allocate_ip(db)
            db.execute("""
                INSERT INTO devices
                (device_id,user_id,device_name,platform,public_key,assigned_ip,status,created_at,revoked_at)
                VALUES(?,?,?,?,?,?,'active',?,NULL)
            """, (device_id,user_id,device_name.strip(),platform,key,assigned_ip,now_iso))
            row = db.execute("SELECT * FROM devices WHERE device_id = ?", (device_id,)).fetchone()
            db.execute("COMMIT")
            return self._to_device(row)
        except sqlite3.IntegrityError as exc:
            db.execute("ROLLBACK")
            raise DeviceRegistryError("This public key is already enrolled.") from exc
        except Exception:
            if db.in_transaction:
                db.execute("ROLLBACK")
            raise
        finally:
            db.close()

    def list_for_user(self, user_id: str) -> list[Device]:
        with self._connect() as db:
            rows = db.execute(
                "SELECT * FROM devices WHERE user_id = ? ORDER BY created_at,device_id",
                (user_id,),
            ).fetchall()
        return [self._to_device(row) for row in rows]

    def revoke(self, user_id: str, device_id: str) -> Device:
        now = datetime.now(timezone.utc).isoformat()
        with self._connect() as db:
            db.execute("BEGIN IMMEDIATE")
            cursor = db.execute("""
                UPDATE devices SET status='revoked',revoked_at=COALESCE(revoked_at,?)
                WHERE user_id=? AND device_id=?
            """, (now,user_id,device_id))
            if cursor.rowcount != 1:
                db.execute("ROLLBACK")
                raise DeviceRegistryError("Device was not found for this account.")
            row = db.execute("SELECT * FROM devices WHERE device_id=?", (device_id,)).fetchone()
            db.execute("COMMIT")
        return self._to_device(row)

    @staticmethod
    def _to_device(row: sqlite3.Row) -> Device:
        return Device(**dict(row))
