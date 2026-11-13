import base64
from datetime import datetime, timedelta, timezone

from fastapi import FastAPI
from fastapi.testclient import TestClient

from app.account_store import AccountStore
from app.auth import AccessTokenCodec
from app.auth_api import PersistentLoginLimiter, create_auth_router
from app.device_registry import DeviceRegistry


def build_client(tmp_path):
    database = tmp_path / "molla.sqlite3"
    accounts = AccountStore(database)
    devices = DeviceRegistry(database)
    codec = AccessTokenCodec(b"molla-test-signing-secret-32-bytes-minimum")
    app = FastAPI()
    app.include_router(create_auth_router(accounts, devices, codec))
    return TestClient(app), accounts, devices


def create_entitled_user(accounts):
    user = accounts.create_account("pilot@example.com", "a sufficiently long password")
    expiry = (datetime.now(timezone.utc) + timedelta(days=7)).isoformat()
    accounts.set_subscription(user.user_id, "pilot-weekly", "trial", 2, expiry)
    return user


def test_login_me_logout_and_revoked_session(tmp_path):
    client, accounts, _devices = build_client(tmp_path)
    user = create_entitled_user(accounts)

    login = client.post("/api/v1/auth/login", json={
        "email": "PILOT@example.com",
        "password": "a sufficiently long password",
    })
    assert login.status_code == 200
    token = login.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    profile = client.get("/api/v1/auth/me", headers=headers)
    assert profile.status_code == 200
    assert profile.json()["user_id"] == user.user_id
    assert profile.json()["subscription"]["active"] is True

    logout = client.post("/api/v1/auth/logout", headers=headers)
    assert logout.status_code == 204
    assert client.get("/api/v1/auth/me", headers=headers).status_code == 401


def test_device_api_requires_auth_and_enforces_subscription(tmp_path):
    client, accounts, _devices = build_client(tmp_path)
    user = create_entitled_user(accounts)
    assert client.get("/api/v1/devices").status_code == 401

    token = client.post("/api/v1/auth/login", json={
        "email": user.email,
        "password": "a sufficiently long password",
    }).json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}
    public_key = base64.b64encode(b"\x01" * 32).decode("ascii")

    enrolled = client.post("/api/v1/devices", headers=headers, json={
        "device_name": "Android phone",
        "platform": "android",
        "public_key": public_key,
    })
    assert enrolled.status_code == 201
    device_id = enrolled.json()["device_id"]
    assert enrolled.json()["assigned_ip"] == "10.66.0.2"

    listed = client.get("/api/v1/devices", headers=headers)
    assert listed.status_code == 200
    assert len(listed.json()["items"]) == 1

    revoked = client.delete(f"/api/v1/devices/{device_id}", headers=headers)
    assert revoked.status_code == 204
    assert client.get("/api/v1/devices", headers=headers).json()["items"][0]["status"] == "revoked"


def test_suspended_account_cannot_use_existing_token(tmp_path):
    client, accounts, _devices = build_client(tmp_path)
    user = create_entitled_user(accounts)
    token = client.post("/api/v1/auth/login", json={
        "email": user.email,
        "password": "a sufficiently long password",
    }).json()["access_token"]
    accounts.set_account_status(user.user_id, "suspended")
    assert client.get("/api/v1/auth/me", headers={
        "Authorization": f"Bearer {token}"
    }).status_code == 401


def test_login_limiter_persists_block_and_clears_after_success(tmp_path):
    limiter = PersistentLoginLimiter(tmp_path / "limits.sqlite3")
    key = limiter.key("pilot@example.com", "192.0.2.10")
    for now in range(1000, 1008):
        limiter.record_failure(key, now=now)
    assert limiter.is_blocked(key, now=1008)

    # A new limiter instance sees the same persisted block.
    restarted = PersistentLoginLimiter(tmp_path / "limits.sqlite3")
    assert restarted.is_blocked(key, now=1008)
    restarted.clear(key)
    assert not limiter.is_blocked(key, now=1008)


def test_login_limiter_window_resets(tmp_path):
    limiter = PersistentLoginLimiter(tmp_path / "limits.sqlite3")
    key = limiter.key("pilot@example.com", "192.0.2.10")
    for now in range(1000, 1008):
        limiter.record_failure(key, now=now)
    limiter.record_failure(key, now=1301)
    assert not limiter.is_blocked(key, now=1301)
