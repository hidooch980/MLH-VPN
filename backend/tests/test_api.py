from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_reports_bootstrap_without_claiming_vpn_is_live():
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "service": "molla-vpn-control-plane",
        "stage": "bootstrap",
    }


def test_protocol_catalog_is_explicitly_planned_and_disabled():
    response = client.get("/api/v1/protocols")
    assert response.status_code == 200
    payload = response.json()
    assert payload["catalog_version"] == "1"
    items = payload["items"]
    assert [item["id"] for item in items] == [
        "wireguard",
        "sing-box",
        "vless-reality",
        "hysteria2",
    ]
    assert all(item["enabled"] is False for item in items)
    assert all(item["status"] == "planned" for item in items)
    assert all(item["traffic_plane_ready"] is False for item in items)
    assert all("private_key" not in item for item in items)
    assert all("endpoint" not in item for item in items)


def test_protocol_catalog_is_read_only():
    response = client.post("/api/v1/protocols")
    assert response.status_code == 405
