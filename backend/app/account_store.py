"""Persistent account, session and subscription-entitlement primitives.

Passwords are stored only as scrypt hashes. Sessions store signed-token IDs, never
bearer tokens. This repository is deliberately not an HTTP API.
"""
from __future__ import annotations

import os
import re
import secrets
import sqlite3
import time
from dataclasses import dataclass
from datetime import datetime, timezone
from pathlib import Path

from app.auth import hash_password, verify_password


class AccountStoreError(ValueError):
    """Raised when account or entitlement data fails validation."""


@dataclass(frozen=True)
class Account:
    user_id: str
    email: str
    role: str
    status: str


@dataclass(frozen=True)
class Subscription:
    user_id: str
    plan_code: str
    status: str
    expires_at: str | None
    max_devices: int


class AccountStore:
    """SQLite-backed account and session repository suitable for a controlled pilot."""

    def __init__(self, database_path: str | Path):
        self.database_path = str(database_path)
        path = Path(self.database_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        self._initialize()
        # Account metadata and password hashes must not be readable by other OS users.
        os.chmod(path, 0o600)

    def _connect(self) -> sqlite3.Connection:
        db = sqlite3.connect(self.database_path, timeout=10)
        db.row_factory = sqlite3.Row
        db.execute("PRAGMA foreign_keys = ON")
        db.execute("PRAGMA busy_timeout = 10000")
        return db

    def _initialize(self) -> None:
        with self._connect() as db:
            db.executescript("""
                CREATE TABLE IF NOT EXISTS accounts (
                    user_id TEXT PRIMARY KEY,
                    email TEXT NOT NULL UNIQUE COLLATE NOCASE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL CHECK(role IN ('user','admin','support')),
                    status TEXT NOT NULL CHECK(status IN ('active','suspended')),
                    created_at TEXT NOT NULL
                );
                CREATE TABLE IF NOT EXISTS sessions (
                    token_id TEXT PRIMARY KEY,
                    user_id TEXT NOT NULL REFERENCES accounts(user_id),
                    expires_at INTEGER NOT NULL,
                    revoked_at TEXT
                );
                CREATE INDEX IF NOT EXISTS sessions_user_id_idx ON sessions(user_id);
                CREATE TABLE IF NOT EXISTS subscriptions (
                    user_id TEXT PRIMARY KEY REFERENCES accounts(user_id),
                    plan_code TEXT NOT NULL,
                    status TEXT NOT NULL CHECK(status IN ('trial','active','expired','cancelled')),
                    expires_at TEXT,
                    max_devices INTEGER NOT NULL CHECK(max_devices BETWEEN 1 AND 100)
                );
            """)

    @staticmethod
    def _normalize_email(email: str) -> str:
        if not isinstance(email, str):
            raise AccountStoreError("A valid email address is required.")
        value = email.strip().lower()
        if len(value) > 254 or value.count("@") != 1:
            raise AccountStoreError("A valid email address is required.")
        local, domain = value.split("@")
        if (not local or not domain or "." not in domain
                or any(ch.isspace() for ch in value)):
            raise AccountStoreError("A valid email address is required.")
        return value

    @staticmethod
    def _parse_expiry(expires_at: str | None) -> datetime | None:
        if expires_at is None:
            return None
        if not isinstance(expires_at, str) or len(expires_at) > 64:
            raise AccountStoreError("Subscription expiry must be an ISO-8601 timestamp.")
        try:
            parsed = datetime.fromisoformat(expires_at.replace("Z", "+00:00"))
        except ValueError as exc:
            raise AccountStoreError("Subscription expiry must be an ISO-8601 timestamp.") from exc
        if parsed.tzinfo is None or parsed.utcoffset() is None:
            raise AccountStoreError("Subscription expiry must include a timezone.")
        return parsed.astimezone(timezone.utc)

    def create_account(self, email: str, password: str, *, role: str = "user") -> Account:
        normalized = self._normalize_email(email)
        if role not in ("user", "admin", "support"):
            raise AccountStoreError("Unsupported account role.")
        if role != "user":
            raise AccountStoreError("Privileged roles cannot be self-registered.")
        password_hash = hash_password(password)
        user_id = secrets.token_urlsafe(18)
        now = datetime.now(timezone.utc).isoformat()
        try:
            with self._connect() as db:
                db.execute(
                    "INSERT INTO accounts(user_id,email,password_hash,role,status,created_at) VALUES(?,?,?,?,?,?)",
                    (user_id, normalized, password_hash, role, "active", now),
                )
        except sqlite3.IntegrityError as exc:
            raise AccountStoreError("An account with this email already exists.") from exc
        return Account(user_id, normalized, role, "active")

    def authenticate(self, email: str, password: str) -> Account | None:
        try:
            normalized = self._normalize_email(email)
        except AccountStoreError:
            return None
        with self._connect() as db:
            row = db.execute("SELECT * FROM accounts WHERE email = ?", (normalized,)).fetchone()
        if row is None or row["status"] != "active":
            return None
        if not verify_password(password, row["password_hash"]):
            return None
        return Account(row["user_id"], row["email"], row["role"], row["status"])

    def get_account(self, user_id: str) -> Account | None:
        with self._connect() as db:
            row = db.execute(
                "SELECT user_id,email,role,status FROM accounts WHERE user_id = ?", (user_id,)
            ).fetchone()
        return Account(**dict(row)) if row else None

    def set_account_status(self, user_id: str, status: str) -> Account:
        if status not in ("active", "suspended"):
            raise AccountStoreError("Unsupported account status.")
        now = datetime.now(timezone.utc).isoformat()
        with self._connect() as db:
            cursor = db.execute(
                "UPDATE accounts SET status = ? WHERE user_id = ?", (status, user_id)
            )
            if cursor.rowcount != 1:
                raise AccountStoreError("Account not found.")
            if status == "suspended":
                db.execute(
                    "UPDATE sessions SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL",
                    (now, user_id),
                )
            row = db.execute(
                "SELECT user_id,email,role,status FROM accounts WHERE user_id = ?", (user_id,)
            ).fetchone()
        return Account(**dict(row))

    def create_session(self, user_id: str, token_id: str, expires_at: int) -> None:
        if (not isinstance(token_id, str) or not re.fullmatch(r"[A-Za-z0-9_-]{8,128}", token_id)
                or type(expires_at) is not int or expires_at <= int(time.time())):
            raise AccountStoreError("Invalid or expired session metadata.")
        with self._connect() as db:
            account = db.execute(
                "SELECT status FROM accounts WHERE user_id = ?", (user_id,)
            ).fetchone()
            if account is None or account["status"] != "active":
                raise AccountStoreError("Active account not found.")
            db.execute(
                "INSERT INTO sessions(token_id,user_id,expires_at,revoked_at) VALUES(?,?,?,NULL)",
                (token_id, user_id, expires_at),
            )

    def session_is_active(self, user_id: str, token_id: str, now: int) -> bool:
        with self._connect() as db:
            row = db.execute("""
                SELECT s.token_id FROM sessions s
                JOIN accounts a ON a.user_id = s.user_id
                WHERE s.user_id = ? AND s.token_id = ? AND s.expires_at > ?
                  AND s.revoked_at IS NULL AND a.status = 'active'
            """, (user_id, token_id, now)).fetchone()
        return row is not None

    def revoke_session(self, user_id: str, token_id: str) -> bool:
        now = datetime.now(timezone.utc).isoformat()
        with self._connect() as db:
            cursor = db.execute(
                "UPDATE sessions SET revoked_at = ? WHERE user_id = ? AND token_id = ? AND revoked_at IS NULL",
                (now, user_id, token_id),
            )
        return cursor.rowcount == 1

    def revoke_all_sessions(self, user_id: str) -> int:
        now = datetime.now(timezone.utc).isoformat()
        with self._connect() as db:
            cursor = db.execute(
                "UPDATE sessions SET revoked_at = ? WHERE user_id = ? AND revoked_at IS NULL",
                (now, user_id),
            )
        return cursor.rowcount

    def set_subscription(self, user_id: str, plan_code: str, status: str,
                         max_devices: int, expires_at: str | None = None) -> Subscription:
        if not isinstance(plan_code, str) or not plan_code.strip() or len(plan_code) > 64:
            raise AccountStoreError("Invalid plan code.")
        if status not in ("trial", "active", "expired", "cancelled"):
            raise AccountStoreError("Invalid subscription status.")
        if type(max_devices) is not int or not 1 <= max_devices <= 100:
            raise AccountStoreError("Device limit must be between 1 and 100.")
        expiry = self._parse_expiry(expires_at)
        if status in ("trial", "active") and expiry is None:
            raise AccountStoreError("Trial and active subscriptions require an explicit expiry.")
        normalized_expiry = expiry.isoformat() if expiry else None
        with self._connect() as db:
            if db.execute("SELECT 1 FROM accounts WHERE user_id = ?", (user_id,)).fetchone() is None:
                raise AccountStoreError("Account not found.")
            db.execute("""
                INSERT INTO subscriptions(user_id,plan_code,status,expires_at,max_devices)
                VALUES(?,?,?,?,?)
                ON CONFLICT(user_id) DO UPDATE SET
                    plan_code=excluded.plan_code,status=excluded.status,
                    expires_at=excluded.expires_at,max_devices=excluded.max_devices
            """, (user_id, plan_code.strip(), status, normalized_expiry, max_devices))
        return Subscription(user_id, plan_code.strip(), status, normalized_expiry, max_devices)

    def get_subscription(self, user_id: str) -> Subscription | None:
        with self._connect() as db:
            row = db.execute("SELECT * FROM subscriptions WHERE user_id = ?", (user_id,)).fetchone()
        return Subscription(**dict(row)) if row else None

    def has_active_subscription(self, user_id: str, *, now: datetime | None = None) -> bool:
        subscription = self.get_subscription(user_id)
        if subscription is None or subscription.status not in ("trial", "active"):
            return False
        expiry = self._parse_expiry(subscription.expires_at)
        current = now or datetime.now(timezone.utc)
        if current.tzinfo is None or current.utcoffset() is None:
            raise AccountStoreError("Entitlement checks require a timezone-aware clock.")
        return expiry is not None and expiry > current.astimezone(timezone.utc)
