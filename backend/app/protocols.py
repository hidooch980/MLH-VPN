from typing import Literal

from pydantic import BaseModel, ConfigDict


ProtocolId = Literal["wireguard", "sing-box", "vless-reality", "hysteria2"]


class ProtocolDescriptor(BaseModel):
    """Public protocol metadata; never contains credentials or server endpoints."""

    model_config = ConfigDict(frozen=True)

    id: ProtocolId
    name: str
    status: Literal["planned", "available", "degraded", "disabled"]
    enabled: bool
    traffic_plane_ready: bool = False


PROTOCOL_CATALOG: tuple[ProtocolDescriptor, ...] = (
    ProtocolDescriptor(
        id="wireguard",
        name="WireGuard",
        status="planned",
        enabled=False,
    ),
    ProtocolDescriptor(
        id="sing-box",
        name="sing-box",
        status="planned",
        enabled=False,
    ),
    ProtocolDescriptor(
        id="vless-reality",
        name="VLESS + REALITY",
        status="planned",
        enabled=False,
    ),
    ProtocolDescriptor(
        id="hysteria2",
        name="Hysteria 2",
        status="planned",
        enabled=False,
    ),
)


def public_protocol_catalog() -> list[dict[str, object]]:
    """Return JSON-safe public metadata with no private provisioning material."""
    return [item.model_dump(mode="json") for item in PROTOCOL_CATALOG]
