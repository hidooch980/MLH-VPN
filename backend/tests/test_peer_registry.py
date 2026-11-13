import base64

import pytest

from app.peer_registry import PeerRegistry, PeerRegistryError


def key(seed: int) -> str:
    return base64.b64encode(bytes([seed]) * 32).decode("ascii")


def test_register_and_list_active_peer(tmp_path):
    registry = PeerRegistry(tmp_path / "peers.sqlite3")
    peer = registry.register("android-1", key(1), "10.66.0.2")

    assert peer.device_id == "android-1"
    assert peer.public_key == key(1)
    assert peer.assigned_ip == "10.66.0.2"
    assert peer.status == "active"
    assert peer.revoked_at is None
    assert registry.list_active() == [peer]


def test_registry_persists_peers_across_instances(tmp_path):
    database = tmp_path / "peers.sqlite3"
    PeerRegistry(database).register("ios-1", key(2), "10.66.0.3")

    restored = PeerRegistry(database)
    assert restored.get("ios-1").assigned_ip == "10.66.0.3"


def test_revoke_peer_is_idempotent_and_removes_from_active_list(tmp_path):
    registry = PeerRegistry(tmp_path / "peers.sqlite3")
    registry.register("desktop-1", key(3), "10.66.0.4")

    first = registry.revoke("desktop-1")
    second = registry.revoke("desktop-1")

    assert first.status == "revoked"
    assert first.revoked_at == second.revoked_at
    assert registry.list_active() == []


@pytest.mark.parametrize("bad_key", ["", "not-base64", "YQ==", " " + key(4)])
def test_rejects_invalid_public_keys(tmp_path, bad_key):
    registry = PeerRegistry(tmp_path / "peers.sqlite3")
    with pytest.raises(PeerRegistryError):
        registry.register("device", bad_key, "10.66.0.2")


def test_rejects_duplicate_device_key_and_address(tmp_path):
    registry = PeerRegistry(tmp_path / "peers.sqlite3")
    registry.register("device-1", key(5), "10.66.0.2")

    with pytest.raises(PeerRegistryError):
        registry.register("device-1", key(6), "10.66.0.3")
    with pytest.raises(PeerRegistryError):
        registry.register("device-2", key(5), "10.66.0.3")
    with pytest.raises(PeerRegistryError):
        registry.register("device-3", key(7), "10.66.0.2")


@pytest.mark.parametrize("address", ["10.66.1.2", "10.66.0.0", "10.66.0.255", "10.66.0.1", "2001:db8::1"])
def test_rejects_out_of_subnet_and_reserved_addresses(tmp_path, address):
    registry = PeerRegistry(tmp_path / "peers.sqlite3")
    with pytest.raises(PeerRegistryError):
        registry.register("device", key(8), address)


def test_unknown_device_cannot_be_revoked(tmp_path):
    registry = PeerRegistry(tmp_path / "peers.sqlite3")
    with pytest.raises(PeerRegistryError):
        registry.revoke("missing")
