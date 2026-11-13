from typing import Literal

from pydantic import BaseModel, ConfigDict


ProtocolId = Literal[
    "wireguard",
    "vless-reality",
    "hysteria2",
    "tuic",
    "amneziawg",
    "shadowsocks-2022",
]


class ProtocolDescriptor(BaseModel):
    """Public capability metadata; never includes credentials or endpoints."""

    model_config = ConfigDict(frozen=True)

    id: ProtocolId
    name: str
    role: Literal["primary", "fallback", "specialized"]
    transport: Literal["udp", "tcp", "quic", "mixed"]
    engine: str
    status: Literal["planned", "available", "degraded", "disabled"]
    enabled: bool
    traffic_plane_ready: bool = False


PROTOCOL_CATALOG: tuple[ProtocolDescriptor, ...] = (
    ProtocolDescriptor(
        id="wireguard",
        name="WireGuard",
        role="primary",
        transport="udp",
        engine="native-wireguard",
        status="planned",
        enabled=False,
    ),
    ProtocolDescriptor(
        id="vless-reality",
        name="VLESS + REALITY",
        role="fallback",
        transport="tcp",
        engine="xray-compatible",
        status="planned",
        enabled=False,
    ),
    ProtocolDescriptor(
        id="hysteria2",
        name="Hysteria 2",
        role="fallback",
        transport="quic",
        engine="sing-box",
        status="planned",
        enabled=False,
    ),
    ProtocolDescriptor(
        id="tuic",
        name="TUIC",
        role="fallback",
        transport="quic",
        engine="sing-box",
        status="planned",
        enabled=False,
    ),
    ProtocolDescriptor(
        id="amneziawg",
        name="AmneziaWG",
        role="specialized",
        transport="udp",
        engine="native-amneziawg",
        status="planned",
        enabled=False,
    ),
    ProtocolDescriptor(
        id="shadowsocks-2022",
        name="Shadowsocks 2022",
        role="specialized",
        transport="tcp",
        engine="sing-box",
        status="planned",
        enabled=False,
    ),
)


SMART_SELECTION_POLICY: dict[str, object] = {
    "mode": "automatic",
    "enabled": True,
    "runtime_ready": False,
    "selection_inputs": [
        "platform_capabilities",
        "server_protocol_capabilities",
        "recent_probe_success",
        "handshake_latency_ms",
        "packet_loss_percent",
        "measured_throughput_mbps",
        "transport_reachability",
    ],
    "safety_rules": [
        "never_select_unavailable_or_unverified_protocols",
        "never_bypass_user_or_operator_policy",
        "do_not_switch_on_single_transient_probe_failure",
        "preserve_kill_switch_during_reconnect",
        "do_not_fallback_to_cleartext",
    ],
}


def public_protocol_catalog() -> list[dict[str, object]]:
    """Return safe protocol metadata without private provisioning material."""
    return [item.model_dump(mode="json") for item in PROTOCOL_CATALOG]


def public_smart_selection_policy() -> dict[str, object]:
    """Return a copy so callers cannot mutate the global policy."""
    return {
        **SMART_SELECTION_POLICY,
        "selection_inputs": list(SMART_SELECTION_POLICY["selection_inputs"]),
        "safety_rules": list(SMART_SELECTION_POLICY["safety_rules"]),
    }
