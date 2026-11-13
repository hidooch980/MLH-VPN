import os
import time
from datetime import datetime, timedelta, timezone

import pytest

from app.account_store import AccountStore, AccountStoreError


def test_account_password_and_session_lifecycle(tmp_path):
    store = AccountStore(tmp_path / "accounts.sqlite3")
    account = store.create_account("Pilot@example.com", "a sufficiently long password")
    assert account.email == "pilot@example.com"
    assert store.authenticate("PILOT@example.com", "a sufficiently long password") == account
    assert store.authenticate("pilot@example.com", "incorrect password") is None

    now = int(time.time())
    store.create_session(account.user_id, "session-0001", now + 500)
    assert store.session_is_active(account.user_id, "session-0001", now + 100)
    assert not store.session_is_active(account.user_id, "session-0001", now + 500)
    assert store.revoke_session(account.user_id, "session-0001")
    assert not store.session_is_active(account.user_id, "session-0001", now + 100)


def test_duplicate_email_and_privileged_self_registration_are_rejected(tmp_path):
    store = AccountStore(tmp_path / "accounts.sqlite3")
    store.create_account("pilot@example.com", "a sufficiently long password")
    with pytest.raises(AccountStoreError):
        store.create_account("PILOT@example.com", "another sufficiently long password")
    with pytest.raises(AccountStoreError):
        store.create_account("admin@example.com", "a sufficiently long password", role="admin")


def test_subscription_entitlements_are_persisted_and_validated(tmp_path):
    store = AccountStore(tmp_path / "accounts.sqlite3")
    account = store.create_account("pilot@example.com", "a sufficiently long password")
    expiry = (datetime.now(timezone.utc) + timedelta(days=30)).isoformat()
    sub = store.set_subscription(account.user_id, "pilot-monthly", "trial", 3, expiry)
    assert store.get_subscription(account.user_id) == sub
    assert store.has_active_subscription(account.user_id)
    with pytest.raises(AccountStoreError):
        store.set_subscription(account.user_id, "bad", "active", 0)
    with pytest.raises(AccountStoreError):
        store.set_subscription(account.user_id, "bad", "active", 1, "2030-01-01T00:00:00")
    with pytest.raises(AccountStoreError):
        store.set_subscription(account.user_id, "no-expiry", "active", 1)


def test_expired_or_cancelled_subscription_is_not_entitled(tmp_path):
    store = AccountStore(tmp_path / "accounts.sqlite3")
    account = store.create_account("pilot@example.com", "a sufficiently long password")
    expired = (datetime.now(timezone.utc) - timedelta(days=1)).isoformat()
    store.set_subscription(account.user_id, "old", "active", 1, expired)
    assert not store.has_active_subscription(account.user_id)
    store.set_subscription(account.user_id, "cancelled", "cancelled", 1)
    assert not store.has_active_subscription(account.user_id)


def test_revoke_all_sessions_and_suspension(tmp_path):
    store = AccountStore(tmp_path / "accounts.sqlite3")
    account = store.create_account("pilot@example.com", "a sufficiently long password")
    now = int(time.time())
    store.create_session(account.user_id, "session-0001", now + 500)
    store.create_session(account.user_id, "session-0002", now + 500)
    assert store.revoke_all_sessions(account.user_id) == 2
    assert not store.session_is_active(account.user_id, "session-0001", now)

    store.create_session(account.user_id, "session-0003", now + 500)
    suspended = store.set_account_status(account.user_id, "suspended")
    assert suspended.status == "suspended"
    assert store.authenticate("pilot@example.com", "a sufficiently long password") is None
    assert not store.session_is_active(account.user_id, "session-0003", now)


def test_account_database_file_is_private(tmp_path):
    database = tmp_path / "private" / "accounts.sqlite3"
    AccountStore(database)
    assert os.stat(database).st_mode & 0o777 == 0o600


def test_session_rejects_expired_and_malformed_ids(tmp_path):
    store = AccountStore(tmp_path / "accounts.sqlite3")
    account = store.create_account("pilot@example.com", "a sufficiently long password")
    with pytest.raises(AccountStoreError):
        store.create_session(account.user_id, "short", int(time.time()) + 300)
    with pytest.raises(AccountStoreError):
        store.create_session(account.user_id, "session-0001", int(time.time()) - 1)
