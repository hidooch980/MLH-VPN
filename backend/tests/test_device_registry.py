import base64
from datetime import datetime, timedelta, timezone

import pytest

from app.account_store import AccountStore
from app.device_registry import DeviceRegistry, DeviceRegistryError


def key(seed: int) -> str:
    return base64.b64encode(bytes([seed]) * 32).decode("ascii")


def setup_account(tmp_path, *, limit=2, status="active", expiry=None):
    database = tmp_path / "molla.sqlite3"
    accounts = AccountStore(database)
    account = accounts.create_account("pilot@example.com", "a sufficiently long password")
    accounts.set_subscription(account.user_id, "pilot", status, limit, expiry)
    return database, account


def test_enrollment_allocates_unique_addresses_and_is_account_scoped(tmp_path):
    database, account = setup_account(tmp_path, limit=2)
    registry = DeviceRegistry(database)
    first = registry.enroll(account.user_id, "My phone", "android", key(1))
    second = registry.enroll(account.user_id, "Work laptop", "windows", key(2))
    assert first.assigned_ip == "10.66.0.2"
    assert second.assigned_ip == "10.66.0.3"
    assert len(registry.list_for_user(account.user_id)) == 2


def test_device_limit_is_enforced_in_transaction(tmp_path):
    database, account = setup_account(tmp_path, limit=1)
    registry = DeviceRegistry(database)
    registry.enroll(account.user_id, "Phone", "android", key(3))
    with pytest.raises(DeviceRegistryError, match="device limit"):
        registry.enroll(account.user_id, "Laptop", "windows", key(4))


def test_enrollment_requires_active_account_and_subscription(tmp_path):
    database, account = setup_account(tmp_path, status="cancelled")
    registry = DeviceRegistry(database)
    with pytest.raises(DeviceRegistryError, match="subscription"):
        registry.enroll(account.user_id, "Phone", "android", key(5))

    accounts = AccountStore(database)
    future = (datetime.now(timezone.utc) + timedelta(days=30)).isoformat()
    accounts.set_subscription(account.user_id, "pilot", "active", 2, future)
    accounts.set_account_status(account.user_id, "suspended")
    with pytest.raises(DeviceRegistryError, match="Active account"):
        registry.enroll(account.user_id, "Phone", "android", key(6))


def test_expired_subscription_is_rejected(tmp_path):
    expiry = (datetime.now(timezone.utc) - timedelta(days=1)).isoformat()
    database, account = setup_account(tmp_path, expiry=expiry)
    registry = DeviceRegistry(database)
    with pytest.raises(DeviceRegistryError, match="subscription"):
        registry.enroll(account.user_id, "Phone", "android", key(7))


def test_duplicate_key_and_invalid_platform_are_rejected(tmp_path):
    database, account = setup_account(tmp_path)
    registry = DeviceRegistry(database)
    registry.enroll(account.user_id, "Phone", "android", key(8))
    with pytest.raises(DeviceRegistryError):
        registry.enroll(account.user_id, "Second phone", "ios", key(8))
    with pytest.raises(DeviceRegistryError):
        registry.enroll(account.user_id, "Unknown", "linux", key(9))


def test_revocation_is_owner_scoped_and_releases_address(tmp_path):
    database, account = setup_account(tmp_path)
    registry = DeviceRegistry(database)
    first = registry.enroll(account.user_id, "Phone", "android", key(10))
    with pytest.raises(DeviceRegistryError):
        registry.revoke("not-the-owner", first.device_id)
    revoked = registry.revoke(account.user_id, first.device_id)
    assert revoked.status == "revoked"
    replacement = registry.enroll(account.user_id, "Tablet", "android", key(11))
    assert replacement.assigned_ip == first.assigned_ip


def test_rejects_invalid_public_key_and_exhausted_pool(tmp_path):
    database, account = setup_account(tmp_path, limit=10)
    registry = DeviceRegistry(database, network="10.66.0.0/30", server_address="10.66.0.1")
    registry.enroll(account.user_id, "Phone", "android", key(12))
    with pytest.raises(DeviceRegistryError, match="No VPN addresses"):
        registry.enroll(account.user_id, "Tablet", "android", key(13))
    with pytest.raises(DeviceRegistryError):
        registry.enroll(account.user_id, "Bad", "android", "not-a-key")
