from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

app = FastAPI(
    title="Molla VPN Control Plane API",
    version="0.1.0",
    description="Initial control-plane scaffold. No VPN traffic is routed by this API.",
)

# Restrict origins before production deployment. This permissive setting is for local
# development only and must be replaced with explicit app/admin origins.
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:3000", "http://localhost:8080"],
    allow_credentials=False,
    allow_methods=["GET"],
    allow_headers=["*"],
)

PROTOCOLS = [
    {"id": "wireguard", "name": "WireGuard", "status": "planned", "enabled": False},
    {"id": "sing-box", "name": "sing-box", "status": "planned", "enabled": False},
    {"id": "vless-reality", "name": "VLESS + REALITY", "status": "planned", "enabled": False},
    {"id": "hysteria2", "name": "Hysteria 2", "status": "planned", "enabled": False},
]


@app.get("/health", tags=["system"])
def health() -> dict[str, str]:
    return {"status": "ok", "service": "molla-vpn-control-plane", "stage": "bootstrap"}


@app.get("/api/v1/protocols", tags=["catalog"])
def list_protocols() -> dict[str, list[dict[str, object]]]:
    """Return planned protocol metadata; no protocol is enabled in this scaffold."""
    return {"items": PROTOCOLS}
