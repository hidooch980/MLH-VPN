import pytest

from app.account_store import AccountStore, AccountStoreError


def test_account_password_and_session_lifecycle(tmp_path):
    store = AccountStore(tmp_path / "accounts.sqlite3")
    account = store.create_account("Pilot@example.com", "a sufficiently long password")
    assert account.email == "pilot@example.com"
    assert store.authenticate("PILOT@example.com", "a sufficiently long password") == account
    assert store.authenticate("pilot@example.com", "incorrect password") is None

    store.create_session(account.user_id, "session-1", 2000)
    assert store.session_is_active(account.user_id, "session-1", 1500)
    assert not store.session_is_active(account.user_id, "session-1", 2000)
    assert store.revoke_session(account.user_id, "session-1")
    assert not store.session_is_active(account.user_id, "session-1", 1500)


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
    sub = store.set_subscription(account.user_id, "pilot-monthly", "trial", 3)
    assert store.get_subscription(account.user_id) == sub
    with pytest.raises(AccountStoreError):
        store.set_subscription(account.user_id, "bad", "active", 0)


def test_revoke_all_sessions(tmp_path):
    store = AccountStore(tmp_path / "accounts.sqlite3")
    account = store.create_account("pilot@example.com", "a sufficiently long password")
    store.create_session(account.user_id, "s1", 3000)
    store.create_session(account.user_id, "s2", 3000)
    assert store.revoke_all_sessions(account.user_id) == 2
    assert not store.session_is_active(account.user_id, "s1", 1000)
