# Community subscriptions and compatibility

MLH VPN adds these public sources on its first main-screen initialization:

| Name | Public subscription | Source project |
| --- | --- | --- |
| SolVPN | `https://raw.githubusercontent.com/SoliSpirit/SolVPN/main/Subscribes/sub1.txt` | https://github.com/SoliSpirit/SolVPN |
| Radikal | `https://raw.githubusercontent.com/0xRadikal/Free-v2ray-Configs/main/verified/configs_base64.txt` | https://github.com/0xRadikal/Free-v2ray-Configs |

Both URLs were fetched on 2026-11-13. This verifies availability of the feeds, not the safety, uptime or reachability of every endpoint. Providers independently change these shared public lists. MLH does not operate those nodes, guarantee their privacy, or include stolen/private paid subscriptions.

Each new built-in group enables the existing WorkManager subscription updater at 60-minute intervals. The first update is eligible immediately when network access is available. Android battery/background restrictions may delay it. Use the existing subscription screen to refresh now, change the interval (minimum 15 minutes), disable updates or delete a source. Existing user subscriptions and preferences are preserved. A one-time marker prevents removed sources from coming back on restart; no server is selected or connected automatically. A failed source fetch does not manufacture a successful connection.

## V2Box

Standard VLESS/VMess/Trojan/Shadowsocks/SOCKS/HTTP/WireGuard/Hysteria2 links and subscription lists are usable when their parameters are supported by the pinned Xray core. V2Box is another client, not a protocol or server provider. This is not a claim of complete V2Box feature parity: SSH, Ping Tunnel, DNSTT, and proprietary V2Box backup/deep-link formats are not implemented by this overlay.

## WARP / 1.1.1.1

The existing WireGuard importer accepts a user's valid WARP-style `.conf` or `wireguard://` profile, including endpoint, addresses, DNS, private/public keys and reserved bytes. A synthetic regression test checks these fields; it is not a live Cloudflare test. Import from the main + menu. WARP account enrollment and MASQUE integration are not implemented. There are no shared WARP private keys in this application. For new enrollment, use the WARP setup menu linking to Cloudflare's official Android instructions. The official WARP app is separate and cannot run as a second simultaneous Android VPN alongside MLH.

Cloudflare's [mode documentation](https://developers.cloudflare.com/warp-client/warp-modes/) distinguishes DNS-only 1.1.1.1 from a WARP traffic tunnel. Selecting 1.1.1.1 DNS in MLH does not by itself enable WARP.

## Free account providers and additional engines

See [the Persian integration status and import guide](FREE-SERVICES.fa.md). Proton Free standard WireGuard exports can use the existing importer, including leading comments and bracketed IPv6 endpoints. Psiphon, Lantern and OpenVPN/VPN Gate are not embedded engines in this release.
