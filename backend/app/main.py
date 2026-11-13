from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.protocols import public_protocol_catalog

app = FastAPI(
    title="Molla VPN Control Plane API",
    version="0.1.1",
    description="Initial control-plane scaffold. No VPN traffic is routed by this API.",
)

# Local development origins only. Replace with explicit production app/admin origins.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:8080"],
    allow_credentials=False,
    allow_methods=["GET"],
    allow_headers=["*"],
)


@app.get("/health", tags=["system"])
def health() -> dict[str, str]:
    return {"status": "ok", "service": "molla-vpn-control-plane", "stage": "bootstrap"}


@app.get("/api/v1/protocols", tags=["catalog"])
def list_protocols() -> dict[str, object]:
    """Return public protocol metadata; this does not provision VPN access."""
    return {
        "catalog_version": "1",
        "items": public_protocol_catalog(),
    }
