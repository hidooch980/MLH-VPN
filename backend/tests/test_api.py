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


def test_protocols_are_explicitly_planned_and_disabled():
    response = client.get("/api/v1/protocols")
    assert response.status_code == 200
    items = response.json()["items"]
    assert [item["id"] for item in items] == [
        "wireguard",
        "sing-box",
        "vless-reality",
        "hysteria2",
    ]
    assert all(item["enabled"] is False for item in items)
    assert all(item["status"] == "planned" for item in items)
