from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_reports_control_plane_without_claiming_vpn_is_live():
    response = client.get("/health")
    assert response.status_code == 200
    payload = response.json()
    assert payload == {
        "status": "ok",
        "service": "molla-vpn-control-plane",
        "stage": "pilot-control-plane",
        "authenticated_api_enabled": False,
    }


def test_protocol_catalog_is_explicitly_planned_and_disabled():
    response = client.get("/api/v1/protocols")
    assert response.status_code == 200
    payload = response.json()
    assert payload["catalog_version"] == "2"
    items = payload["items"]
    assert [item["id"] for item in items] == [
        "wireguard",
        "vless-reality",
        "hysteria2",
        "tuic",
        "amneziawg",
        "shadowsocks-2022",
    ]
    assert {item["id"]: item["role"] for item in items} == {
        "wireguard": "primary",
        "vless-reality": "fallback",
        "hysteria2": "fallback",
        "tuic": "fallback",
        "amneziawg": "specialized",
        "shadowsocks-2022": "specialized",
    }
    selection = payload["connection_selection"]
    assert selection["mode"] == "automatic"
    assert selection["enabled"] is True
    assert selection["runtime_ready"] is False
    assert "preserve_kill_switch_during_reconnect" in selection["safety_rules"]
    assert all(item["enabled"] is False for item in items)
    assert all(item["status"] == "planned" for item in items)
    assert all(item["traffic_plane_ready"] is False for item in items)
    assert all("private_key" not in item for item in items)
    assert all("endpoint" not in item for item in items)


def test_protocol_catalog_is_read_only():
    response = client.post("/api/v1/protocols")
    assert response.status_code == 405


def test_auth_routes_are_not_mounted_without_runtime_signing_secret():
    assert client.post("/api/v1/auth/login", json={
        "email": "pilot@example.com",
        "password": "not-a-real-password",
    }).status_code == 404
