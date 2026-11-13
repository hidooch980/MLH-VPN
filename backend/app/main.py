import os
from pathlib import Path

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.protocols import public_protocol_catalog, public_smart_selection_policy

app = FastAPI(
    title="Molla VPN Control Plane API",
    version="0.3.0",
    description="Control-plane API. VPN traffic is not routed by this service.",
)

# Local development origins only. Production origins must be explicitly configured.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:8080"],
    allow_credentials=False,
    allow_methods=["GET", "POST", "DELETE"],
    allow_headers=["Authorization", "Content-Type"],
)

_auth_api_enabled = False
_signing_secret = os.environ.get("MOLLAVPN_TOKEN_SECRET", "")
if len(_signing_secret.encode("utf-8")) >= 32:
    from app.account_store import AccountStore
    from app.auth import AccessTokenCodec
    from app.auth_api import create_auth_router
    from app.device_registry import DeviceRegistry

    database_path = os.environ.get(
        "MOLLAVPN_DB_PATH", str(Path("data") / "molla-vpn.sqlite3")
    )
    account_store = AccountStore(database_path)
    device_registry = DeviceRegistry(
        database_path,
        network=os.environ.get("MOLLAVPN_DEVICE_NETWORK", "10.66.0.0/24"),
        server_address=os.environ.get("MOLLAVPN_SERVER_ADDRESS", "10.66.0.1"),
    )
    token_codec = AccessTokenCodec(_signing_secret)
    app.include_router(create_auth_router(account_store, device_registry, token_codec))
    _auth_api_enabled = True


@app.get("/health", tags=["system"])
def health() -> dict[str, object]:
    return {
        "status": "ok",
        "service": "molla-vpn-control-plane",
        "stage": "pilot-control-plane",
        "authenticated_api_enabled": _auth_api_enabled,
    }


@app.get("/api/v1/protocols", tags=["catalog"])
def list_protocols() -> dict[str, object]:
    """Return public protocol metadata; this does not provision VPN access."""
    return {
        "catalog_version": "2",
        "items": public_protocol_catalog(),
        "connection_selection": public_smart_selection_policy(),
    }
