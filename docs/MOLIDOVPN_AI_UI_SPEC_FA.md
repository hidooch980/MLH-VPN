# MolidoVPN AI — UI/UX Design Specification (Persian RTL)

**Status:** design baseline for implementation on `feature/molidovpn-ai-redesign`  
**Reference:** user-provided MolidoVPN home-screen screenshot and the six-screen concept board generated for this task.  
**Platform:** native Android / Jetpack Compose, first release ARM64.

## 1. Visual language

- Preserve the reference screen's midnight-navy canvas, restrained teal/cyan outlines, rounded glass-like panels, large power control and premium compact typography.
- Use a consistent spacing scale (4/8/12/16/20/24 dp), 18–24 dp card radii and minimum 48 dp touch targets.
- Use cyan/mint only for active or positive states; amber for caution; red for errors; muted blue-grey for unavailable data.
- Avoid persistent decorative animations. Respect Android animator/reduced-motion settings and system font scaling.
- All labels and accessibility descriptions must come from Android string resources. Layout direction follows the selected locale (Persian/Arabic RTL; English/Turkish LTR).

## 2. Primary navigation

Use five destinations, with a compact bottom bar and a secondary "More" hub:
1. Home
2. Servers
3. AI
4. Activity / diagnostics
5. More

The More hub groups Protocol catalogue, Imports & subscriptions, Security & privacy, Language & appearance, Support, Updates and About. A screen must not be shown as operational until it is backed by a real app action or data source.

## 3. Screen specifications

### Home — connection cockpit
- Header: MolidoVPN AI brand, connection state and quick appearance/language access.
- Primary connection card: large connect/disconnect action; distinct disconnected, requesting permission, connecting, connected and error states; elapsed session time only while connected.
- Active profile: provider/profile name, protocol label only when parsed from the selected profile, approximate exit region only when measured, and IP only when freshly returned by the existing connection test.
- Live metrics: ping, upload/download rates and session bytes. Stale or unavailable readings show "—" / "not measured", never fabricated zeroes or sample chart data.
- Smart recommendation card: selected profile, quality grade, measured score and concise reason. Manual choice always remains available.
- Quick actions: server catalogue, import existing profile, diagnostics.

### Servers — catalogue and selection
- Search and filters: all, favourites, recently tested, measured/reachable.
- Each row: actual profile/server label, source/group, latest measured latency and freshness, quality grade, selected state.
- Actions: select profile using the existing profile GUID action; run existing bulk/current latency test; favourite only if persisted by existing app state.
- Empty states explain that the user must import a provider profile; never invent countries or server endpoints.

### AI — local intelligence
- Explain that ranking is performed locally and does not require a cloud AI server.
- Display only measured inputs available from the current quality engine: latency, jitter, failed probes, throughput when measured, quarantine and score.
- Show decision reason from the deterministic selector; respect manual override and never silently change profile on a marginal score difference.
- Smart selection toggle is not to be shown until its preference and lifecycle behavior are implemented and tested.

### Protocol catalogue
- Show the six requested protocol families: VLESS, VMess, Trojan, Shadowsocks, Hysteria2 and WireGuard.
- Each card must separate source/parser evidence from verified runtime status. A protocol is not "verified" until physical-device traffic, DNS, IPv4/IPv6, reconnect and handover acceptance is recorded.
- Selecting a protocol means filtering/choosing an imported profile of that type, not generating a server or changing a profile's protocol in place.
- SOCKS and HTTP are labelled proxy/profile types, not presented as extra VPN tunnel protocols. Tor, obfs4 and sing-box remain experimental/unavailable until adapter evidence exists.

### Imports & subscriptions
- Entry points: QR, clipboard, file, subscription URL and supported configuration formats already implemented upstream.
- Preserve existing subscription refresh, deletion and profile-edit behavior. Do not create a new subscription backend or claim billing support.
- Clearly show parse failures, refresh time and unsupported configuration format without exposing URL tokens in logs.

### Activity / diagnostics
- Session duration, transferred bytes, last measured quality and test progress from existing sources.
- Historical daily/monthly charts are withheld until durable local history storage exists.
- DNS/IP leak checks are "not yet tested" unless a dedicated test result is available; do not infer safety from a connected tunnel.

### Security & privacy
- Explain Android Always-on VPN and Block connections without VPN via the actual Android VPN settings intent.
- Show local privacy policy: no upload of browsing destinations, DNS queries, payloads or raw profile strings for AI learning.
- Do not present a kill-switch toggle as app-controlled if Android owns the setting.
- Show test statuses only when a real test has run.

### Language, appearance, support, updates
- Locale changes use resource-backed translations and correct RTL/LTR direction.
- Appearance controls must change a real theme preference before they are exposed.
- Support provides troubleshooting guidance and a safe diagnostic export with secrets redacted.
- Updates use the existing signed/restricted updater path; never imply a new version is installed before Android Package Installer confirmation.

## 4. State and data truthfulness

Every dynamic field has one of: measured + timestamp, derived from measured inputs, user-configured, or unavailable. Unknown values are not replaced by demo values. Connected means the Android service reports running; it does not by itself prove DNS leak protection, stable quality, anonymity or successful internet reachability.

## 5. Accessibility and localization acceptance

- TalkBack labels for every icon-only action.
- 48 dp minimum targets, contrast checked for normal and large text.
- Font scale 1.0, 1.3 and 1.5; RTL and LTR screenshots.
- No clipped text on small Android screens; scrollable secondary pages.
- Reduced-motion behavior and distinct status text/icons (not color alone).

## 6. Implementation gates

1. Preserve the pinned upstream tunnel, parser, routing, permission and profile lifecycle.
2. Implement visual components against existing `MainUiState`, server rows, protocol registry and real `MainAction` values.
3. Add UI/state tests; run Android unit tests and emulator smoke tests in CI.
4. Validate on physical ARM64 device with user-owned valid profiles before marking protocols, security checks or network metrics verified.
5. Keep production signing/upgrade continuity as a separate release gate; do not replace the existing signing identity to unblock a preview build.
