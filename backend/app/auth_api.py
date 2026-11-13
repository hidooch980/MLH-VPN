"""Authenticated control-plane routes.

Registration and privileged account provisioning are intentionally out of scope:
accounts must be created by a controlled administrative process until email
verification, abuse controls and admin bootstrap are implemented.
"""
from __future__ import annotations

import hashlib
import secrets
import sqlite3
import time
from pathlib import Path
from typing import Callable

from fastapi import APIRouter, Depends, HTTPException, Request, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from pydantic import BaseModel, ConfigDict, Field

from app.account_store import AccountStore, AccountStoreError
from app.auth import AccessTokenCodec, AuthenticationError
from app.device_registry import DeviceRegistry, DeviceRegistryError


class LoginBody(BaseModel):
    model_config = ConfigDict(extra="forbid")
    email: str = Field(min_length=3, max_length=254)
    password: str = Field(min_length=1, max_length=1024)


class EnrollDeviceBody(BaseModel):
    model_config = ConfigDict(extra="forbid")
    device_name: str = Field(min_length=1, max_length=80)
    platform: str
    public_key: str = Field(min_length=44, max_length=44)


class PersistentLoginLimiter:
    """SQLite-backed fixed-window limiter; survives API process restarts."""

    def __init__(self, database_path: str | Path, *, limit: int = 8,
                 window_seconds: int = 300, block_seconds: int = 900):
        self.database_path = str(database_path)
        self.limit = limit
        self.window_seconds = window_seconds
        self.block_seconds = block_seconds
        Path(self.database_path).parent.mkdir(parents=True, exist_ok=True)
        with self._connect() as db:
            db.execute("""
                CREATE TABLE IF NOT EXISTS login_limits (
                    key_hash TEXT PRIMARY KEY,
                    window_started INTEGER NOT NULL,
                    failures INTEGER NOT NULL,
                    blocked_until INTEGER NOT NULL DEFAULT 0
                )
            """)

    def _connect(self) -> sqlite3.Connection:
        db = sqlite3.connect(self.database_path, timeout=10, isolation_level=None)
        db.execute("PRAGMA busy_timeout = 10000")
        return db

    @staticmethod
    def key(email: str, remote: str) -> str:
        normalized = email.strip().lower()[:254]
        material = f"{normalized}\0{remote[:128]}".encode("utf-8", "replace")
        return hashlib.sha256(material).hexdigest()

    def is_blocked(self, key_hash: str, *, now: int | None = None) -> bool:
        current = int(time.time()) if now is None else now
        with self._connect() as db:
            row = db.execute(
                "SELECT blocked_until FROM login_limits WHERE key_hash = ?",
                (key_hash,),
            ).fetchone()
        return bool(row and row[0] > current)

    def record_failure(self, key_hash: str, *, now: int | None = None) -> None:
        current = int(time.time()) if now is None else now
        db = self._connect()
        try:
            db.execute("BEGIN IMMEDIATE")
            row = db.execute(
                "SELECT window_started,failures,blocked_until FROM login_limits WHERE key_hash = ?",
                (key_hash,),
            ).fetchone()
            if row is None or current - row[0] >= self.window_seconds or current < row[0]:
                started, failures, blocked = current, 1, 0
            elif row[2] > current:
                started, failures, blocked = row
            else:
                started, failures = row[0], row[1] + 1
                blocked = current + self.block_seconds if failures >= self.limit else 0
            db.execute("""
                INSERT INTO login_limits(key_hash,window_started,failures,blocked_until)
                VALUES(?,?,?,?)
                ON CONFLICT(key_hash) DO UPDATE SET
                    window_started=excluded.window_started,
                    failures=excluded.failures,blocked_until=excluded.blocked_until
            """, (key_hash, started, failures, blocked))
            db.execute("COMMIT")
        except Exception:
            if db.in_transaction:
                db.execute("ROLLBACK")
            raise
        finally:
            db.close()

    def clear(self, key_hash: str) -> None:
        with self._connect() as db:
            db.execute("DELETE FROM login_limits WHERE key_hash = ?", (key_hash,))


def create_auth_router(
    accounts: AccountStore,
    devices: DeviceRegistry,
    codec: AccessTokenCodec,
    *,
    limiter: PersistentLoginLimiter | None = None,
) -> APIRouter:
    router = APIRouter(prefix="/api/v1", tags=["account"])
    bearer = HTTPBearer(auto_error=False)
    login_limiter = limiter or PersistentLoginLimiter(accounts.database_path)

    def current_account(
        credentials: HTTPAuthorizationCredentials | None = Depends(bearer),
    ):
        if credentials is None or credentials.scheme.lower() != "bearer":
            raise HTTPException(status_code=status.HTTP_401_UNAUTHORIZED,
                                detail="Authentication required.",
                                headers={"WWW-Authenticate": "Bearer"})
        try:
            claims = codec.verify(credentials.credentials)
        except AuthenticationError:
            raise HTTPException(status_code=401, detail="Invalid or expired session.",
                                headers={"WWW-Authenticate": "Bearer"})
        account = accounts.get_account(claims.subject)
        if (account is None or account.status != "active"
                or not accounts.session_is_active(account.user_id, claims.token_id, int(time.time()))):
            raise HTTPException(status_code=401, detail="Invalid or expired session.",
                                headers={"WWW-Authenticate": "Bearer"})
        return (account, claims)

    @router.post("/auth/login")
    def login(body: LoginBody, request: Request):
        remote = request.client.host if request.client else "unknown"
        key_hash = login_limiter.key(body.email, remote)
        if login_limiter.is_blocked(key_hash):
            raise HTTPException(status_code=429, detail="Too many login attempts. Try again later.",
                                headers={"Retry-After": str(login_limiter.block_seconds)})
        account = accounts.authenticate(body.email, body.password)
        if account is None:
            login_limiter.record_failure(key_hash)
            raise HTTPException(status_code=401, detail="Invalid email or password.")
        login_limiter.clear(key_hash)
        token = codec.issue(account.user_id)
        claims = codec.verify(token)
        try:
            accounts.create_session(account.user_id, claims.token_id, claims.expires_at)
        except AccountStoreError:
            raise HTTPException(status_code=401, detail="Unable to establish session.")
        return {
            "access_token": token,
            "token_type": "Bearer",
            "expires_in": claims.expires_at - claims.issued_at,
        }

    @router.get("/auth/me")
    def me(identity=Depends(current_account)):
        account, _claims = identity
        subscription = accounts.get_subscription(account.user_id)
        entitled = accounts.has_active_subscription(account.user_id)
        return {
            "user_id": account.user_id,
            "email": account.email,
            "role": account.role,
            "status": account.status,
            "subscription": {
                "plan_code": subscription.plan_code,
                "status": subscription.status,
                "expires_at": subscription.expires_at,
                "max_devices": subscription.max_devices,
                "active": entitled,
            } if subscription else None,
        }

    @router.post("/auth/logout", status_code=204)
    def logout(identity=Depends(current_account)):
        account, claims = identity
        accounts.revoke_session(account.user_id, claims.token_id)
        return None

    @router.get("/devices")
    def list_devices(identity=Depends(current_account)):
        account, _claims = identity
        return {"items": [
            {
                "device_id": d.device_id,
                "device_name": d.device_name,
                "platform": d.platform,
                "public_key": d.public_key,
                "assigned_ip": d.assigned_ip,
                "status": d.status,
                "created_at": d.created_at,
                "revoked_at": d.revoked_at,
            }
            for d in devices.list_for_user(account.user_id)
        ]}

    @router.post("/devices", status_code=201)
    def enroll_device(body: EnrollDeviceBody, identity=Depends(current_account)):
        account, _claims = identity
        try:
            device = devices.enroll(
                account.user_id, body.device_name, body.platform, body.public_key
            )
        except DeviceRegistryError as exc:
            message = str(exc)
            code = 409 if "already enrolled" in message or "limit" in message else 403
            raise HTTPException(status_code=code, detail=message)
        return {
            "device_id": device.device_id,
            "device_name": device.device_name,
            "platform": device.platform,
            "public_key": device.public_key,
            "assigned_ip": device.assigned_ip,
            "status": device.status,
            "created_at": device.created_at,
        }

    @router.delete("/devices/{device_id}", status_code=204)
    def revoke_device(device_id: str, identity=Depends(current_account)):
        account, _claims = identity
        try:
            devices.revoke(account.user_id, device_id)
        except DeviceRegistryError:
            raise HTTPException(status_code=404, detail="Device not found.")
        # This revokes registry state only. No live WireGuard node is reconciled yet.
        return None

    return router
