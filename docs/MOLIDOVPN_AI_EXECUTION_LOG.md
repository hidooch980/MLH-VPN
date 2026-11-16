# MolidoVPN AI — دفتر اجرای پروژه

## 2026-11-16 — شروع اجرای نقشه راه مادر

### مرجع
- شاخه: `feature/molidovpn-ai-redesign`
- آخرین HEAD پس از ثبت نقشه راه: `7178464abd55190caebd750bc84c1aeb0fe4f801`
- commit کد انتخاب هوشمند: `876d4ecb550d58a8c1d58ee7e61ae0efd3ff6e83`
- نقشه راه: [MOLIDOVPN_AI_MASTER_ROADMAP.fa.md](MOLIDOVPN_AI_MASTER_ROADMAP.fa.md)
- workflow: [Android Standalone APK #43916712315](https://github.com/hidooch980/MLH-VPN/actions/runs/43916712315)

### تغییرات ثبت‌شده
- [x] اضافه شدن `SmartEngineOrchestrator` برای انتخاب محلی و رتبه‌بندی پروفایل‌های قابل دسترس.
- [x] پشتیبانی policy از AUTO/FAST/STABLE/COMPATIBLE و انتخاب دستی محدود به پروفایل سازگار و قابل دسترس.
- [x] جلوگیری از تعویض کوچک و بی‌دلیل با حداقل اختلاف امتیاز.
- [x] اعمال سقف برای اثر یادگیری محلی و پاداش علاقه‌مندی/کشور.
- [x] تست‌های واحد برای پایداری انتخاب، تعویض با بهبود معنی‌دار، انتخاب دستی، قرنطینه و bounded learning.
- [x] ثبت نقشه راه مادر و پیوند آن از `docs/ROADMAP.md`.

### شواهد CI برای commit کد
- [x] Overlay unit checks: موفق.
- [x] دریافت upstream قفل‌شده: موفق.
- [x] نصب Java و Android SDK: موفق.
- [x] Build و تست‌های Gradle: موفق.
- [x] بررسی هویت MolidoVPN AI و native engines بسته‌بندی‌شده: موفق.
- [x] ساخت source bundle و checksum: موفق.
- [x] بارگذاری artifact: موفق.
- [x] تست Android emulator: موفق؛ نصب APK، نصب instrumentation test APK و اجرای تست startup؛ نتیجه `OK (1 test)`.
- [ ] تست تونل واقعی روی گوشی ARM64 با endpoint و کانفیگ معتبر: انجام نشده.
- [ ] تأیید نشت DNS/IPv4/IPv6 و تغییر شبکه روی دستگاه واقعی: انجام نشده.

### Artifact
- نام: `MLHVPN-Android-preview`
- Artifact ID: `14249773850`
- حجم: 255,107,862 bytes
- Unit tests: `170 tests, 0 failures, 0 skipped` (Gradle HTML report).
- Emulator instrumentation: `OK (1 test)`.
- APK ARM64 استخراج و checksum آن با `SHA256SUMS` داخل artifact تأیید شد.
- محتوا شامل APKهای ARM64، ARM32، x86، x86_64، Universal، Android test APK، source bundle و SHA256SUMS است.
- این APKها preview/debug هستند، نه release امضاشده برای انتشار عمومی.
- SHA-256 فایل ZIP دریافت‌شده: `6d438b3129e797d244d5c80a62b40a680e3e61a2893ce552ac7753dda266e43c`

### ریسک‌ها و کارهای باز
1. اجرای emulator smoke هنوز نتیجه نهایی ندارد.
2. باید کد جدید orchestrator به جریان واقعی انتخاب سرور/اتصال متصل شود؛ فعلاً یک coordinator مستقل و unit-tested است و خودش tunnel را تغییر نمی‌دهد.
3. فهرست پروتکل‌ها به‌تنهایی نشان‌دهنده پشتیبانی runtime نیست؛ برای هر engine adapter و تست واقعی لازم است.
4. باید ممیزی جامع وابستگی‌ها، ABIها، مجوزها، native binaries، UI، امنیت و lifecycle تکمیل شود.
5. release signing به کلید بازیابی امن و سازگار با نسخه قبلی نیاز دارد.

### گام بعدی
- گرفتن نتیجه نهایی emulator smoke و بررسی instrumentation artifact.
- سپس فاز 0: ممیزی معماری و native engine inventory؛ ثبت موارد واقعی و شکاف‌ها.
- بعد از آن اتصال orchestrator به مسیر واقعی server selection با تست regression.


### تکمیل اجرای CI
- Workflow run `43916712315` نهایی شد: `success`.
- Build job: `success`; Android smoke job: `success`.
- Gradle unit test report: 170 تست، صفر شکست، صفر skipped.
- Emulator instrumentation: `OK (1 test)`.
- Preview ARM64 APK در مسیر artifact موجود است؛ checksum داخلی artifact تأیید شد.
- این نتایج فقط روی commit کد `876d4ecb550d58a8c1d58ee7e61ae0efd3ff6e83` معتبرند؛ آخرین commit فعلی شاخه شامل مستندات نقشه راه و دفتر اجراست و هنوز workflow مجزا برای همان HEAD اجرا نشده است.

## 2026-11-16 — اتصال انتخاب‌گر هوشمند به جریان واقعی انتخاب سرور

### تغییر کد
- Commit: `82298004e58719dd100090982dd40bea27f162c0`
- فایل: `apps/android/overlay/app/src/main/java/com/v2ray/ang/ui/main/MainViewModel.kt`
- انتخاب حالت AUTO/FAST/STABLE/COMPATIBLE اکنون از `SmartEngineOrchestrator` استفاده می‌کند.
- رتبه‌بندی پس از پایان تست گروهی سرورها نیز از همان orchestrator عبور می‌کند؛ دو مسیر تصمیم‌گیری موازی حذف شدند.
- نام پروتکل profile به مدل پروتکل داخلی نگاشت می‌شود؛ Hysteria و Hysteria2 به یک capability واحد normalize می‌شوند.
- فقط پروفایل دارای latency مثبت و قابل ارزیابی، با پروتکل سازگار، وارد فهرست کاندیداها می‌شود. latency ناشناخته به‌عنوان اتصال موفق فرض نمی‌شود.
- علاقه‌مندی و کشور تأییدشده به‌عنوان سیگنال‌های محدود رتبه‌بندی محلی استفاده می‌شوند.
- هنگام اجرای تونل فعال، تست گروهی باعث تغییر سرور نمی‌شود؛ تغییر خودکار پس از تست فقط وقتی تونل خاموش است اعمال می‌شود.
- انتخاب سرور در این مسیر دیگر به‌اشتباه به‌عنوان «اتصال موفق» در حافظه ثبت نمی‌شود؛ ثبت موفقیت به callback واقعی اتصال واگذار شده است.

### اعتبارسنجی
- GitHub Actions run: [43920252812](https://github.com/hidooch980/MLH-VPN/actions/runs/43920252812)
- SHA مورد آزمایش: `82298004e58719dd100090982dd40bea27f162c0`
- آخرین مشاهده هنگام ثبت این یادداشت: build job در حال اجرای Gradle build/tests؛ نتیجه نهایی هنوز دریافت نشده است.
- این تغییر تا موفقیت CI و بازبینی تست‌ها «تکمیل‌شده» محسوب نمی‌شود.


## 2026-11-16 — Smart selection CI and finalization gate review
- Android source commit tested: `82298004e58719dd100090982dd40bea27f162c0`.
- Workflow run `43920252812`: build and Android emulator smoke jobs both completed successfully.
- Preview artifact `MLHVPN-Android-preview` ID `14254117139`; SHA-256 `48c6639ca0aafbf3140070a9ef688d1d29a9bf127414e74d2b96743a9e0f9d1d`.
- Source review found that smart orchestration selects a profile but does not own the actual tunnel lifecycle; candidate latency freshness is not established at the call site.
- A shared multi-engine adapter, per-engine runtime verification, full license/SBOM clearance, multilingual resources, physical-device leak/kill-switch tests, and production signing continuity remain open.
- Status: preview CI green; production release and physical VPN acceptance remain blocked. No merge to main.


## 2026-11-16 — Hardening pass: probe freshness and Xray lifecycle adapter

### Source changes on feature branch
- MainViewModel freshness gate: commit `b9d057a55c61654f0c75730315f182a7af99d22d`.
- Added monotonic `ProbeFreshness` with a five-minute TTL; persisted latency values loaded from cache are not considered fresh after process restart. Missing, stale and future-dated observations are rejected from smart candidate construction.
- Added unit tests for unknown, fresh, stale, future-dated and blank-ID probe observations: commit `e4d76eb54be892fe14be0236d683e1c8542fa046` and `711afcd0e4772194d0065926cafc872281dd3b42`.
- Added `EngineAdapter` lifecycle contract and `XrayCoreEngineAdapter`; routed CoreServiceManager start/stop/status through this adapter while leaving Xray-specific delay/traffic/process APIs on the existing CoreController: commits `7b9129b8262a369956a47b7ceaf8cac0342a1457` and `920824be81828dd52e57141abbb5d792024bf888`.
- Only the Xray-compatible core is registered as an implemented adapter. This does not enable WireGuard/Hysteria2 as independent engines and does not establish multi-engine switching.

### CI
- Latest Android workflow run: [43923994731](https://github.com/hidooch980/MLH-VPN/actions/runs/43923994731).
- Tested HEAD: `920824be81828dd52e57141abbb5d792024bf888`.
- Overlay Python checks passed; Android Gradle build/tests were still running when this note was written. No pass/fail claim is made until the workflow completes.

### Still blocked / requires device or owner-provided inputs
- Real ARM64 phone testing with a valid server profile: handshake, routed traffic, public egress IP, DNS, IPv4/IPv6 leak, Kill Switch/Always-on, force-stop, permission revoke, airplane mode and Wi-Fi/mobile handover.
- Full protocol-by-protocol runtime acceptance; only Xray lifecycle is behind the adapter contract at this stage.
- Full SBOM and per-dependency license/NOTICE clearance.
- Persian/English/Arabic/Turkish resource localization and accessibility acceptance.
- Production signing continuity and install-over-upgrade test using the existing signing identity.
- No merge to `main`; no production release.


## 2026-11-16 — Active-session selection guard
- Commit `d9973366fe251794654b0d481853c510af3f8c16`: changing AUTO/FAST/STABLE/COMPATIBLE while a tunnel is running now stores the preference for the next session and does not change the selected profile underneath the active tunnel. This prevents the UI's selected profile from diverging from the endpoint actually carrying traffic.
- Android workflow run: [43924969206](https://github.com/hidooch980/MLH-VPN/actions/runs/43924969206), HEAD `d9973366fe251794654b0d481853c510af3f8c16`; queued/pending at log time. No build result claimed.
- The preceding workflow [43923994731](https://github.com/hidooch980/MLH-VPN/actions/runs/43923994731) was still running its Gradle build; it is not evidence of a completed build for the later HEAD.


## 2026-11-16 — Adapter error contract and protocol UI evidence correction
- Commit `928234b0ee926a4398df6f7a71eb2fc376726a7a`: Xray adapter now normalizes START/STOP/STATUS failures into `EngineAdapterException` with stable engine ID and operation. Exception message excludes config, endpoint and credential contents; original exception remains available as cause for controlled diagnostics.
- Commit `71bb7bf0c5f048b0c20673ac8856e77e686348a3`: added unit test proving failure metadata is stable and exception message does not expose secret-like cause text.
- Commit `75a23ad0d92f36903a8ec4eccd31e8626f419191`: protocol dashboard now labels an item as verified only when `deviceVerified == true`; source documentation alone is shown as needing review until physical-device acceptance exists.
- Latest CI run: [43925761547](https://github.com/hidooch980/MLH-VPN/actions/runs/43925761547), HEAD `71bb7bf0c5f048b0c20673ac8856e77e686348a3`; queued when recorded. No Build/test result claimed yet.
- Prior run [43924969206](https://github.com/hidooch980/MLH-VPN/actions/runs/43924969206) targets an earlier SHA and remains in progress; it cannot validate these later commits.
- Still not verified: physical-device tunnel operation for each protocol; IPv4/IPv6 and DNS leak behavior; Kill Switch; Wi-Fi/mobile handover; signing-key recovery and upgrade continuity.


## 2026-11-16 — Dashboard data integrity
- Removed the hard-coded synthetic quality trend line from the dashboard; it was not backed by a stored time series.
- Added explicit localized disclosure (English, Persian, Arabic, Turkish) that trend history is not yet collected and the displayed score represents only the latest assessment.
- Commits: resource strings `17e9b0cd49e72c79a0e7f980d34cb6ef3c07fda6`, `ab0e703b30174618c26e3a2dfc5af80b71569e34`, `8f4b1add788639a19ed438b7f2e2abf17fe9228e`, `15cc51cac6dce098221a6ed6076201e4d9766c66`; UI `7ea1fb2b1ef8fa7fc554aaee34232edc442431f5`.
- This is a correctness fix, not a claim that historical analytics are implemented.


## 2026-11-16 — Recovery retry, privacy-safe quality history, and release gates

### Implemented on the feature branch
- `fd51b112c5861becb9cc7bfd6572677edab0bc3b`: network handover recovery now performs up to three bounded attempts with 0.5s / 1.5s / 4s backoff, including restart when the Xray core is no longer running. Final failure is surfaced as a generic reconnect message. This is recovery for the currently wired Xray-compatible adapter only; it is not per-engine recovery for engines that are not integrated.
- The same change removes logging of generated tunnel config and avoids forwarding native exception messages to UI/logs, reducing risk of exposing credentials or endpoints.
- `35e798a87947b0da5913d6b6915aff127f144789`, `6b76264028136c4f55edf8c8ca0b915d3764cf11`, `1872c85bf8b47bef705f66ee9ee1e5067d00d0d2`: added a bounded local quality-history store and codec tests. It records aggregate timestamp, score, latency, jitter, failure percentage and measured throughput only; it deliberately omits server IDs, IPs, endpoints, configuration and DNS/activity data.
- `30018ea23a00f1249a5fee644853f6b0c1367942`, `b63a915079119512e47a17fc1cd516959baf9f17`: history is loaded into UI state and appended after real quality assessments.
- `a1da38e59a1579fe29b989a014a863ab575e5ed8`, `0b5f204dfb2597b10c2a663fb15920cf4028cf4b`, `48073a38eb887521fd61e6960facf890adfdf5cd`, `a0b88b687a308e09884f5304e11c1fc390200a8b`, `269435bef9c8b978ab080f3c08a5b4f33c83b217`: dashboard history view and English/Persian/Arabic/Turkish labels added.

### CI
- Workflow [43926904351](https://github.com/hidooch980/MLH-VPN/actions/runs/43926904351), HEAD `269435bef9c8b978ab080f3c08a5b4f33c83b217`, was in progress when this entry was written. No pass/fail claim is made yet.
- Earlier CI attempts were cancelled by subsequent pushes; only the latest run for the final code HEAD is relevant.

### Explicit blockers not solvable from repository code alone
- WireGuard and Hysteria2 still lack an independently proven engine adapter/runtime acceptance path. Source documentation/config formatting is not proof of end-to-end connectivity. Do not mark either protocol stable without pinned runtime, ABI/license review, valid endpoint tests and real-device traffic verification.
- IPv4/IPv6 egress, DNS leak, Kill Switch/Always-on and Wi-Fi/mobile handover still require a physical ARM64 phone and a valid test profile entered locally on that phone. No such device/config is attached to this development environment.
- The previous signing identity can only be restored by decrypting the existing encrypted backup. The signed-release workflow expects GitHub Actions secret `MLHVPN_BACKUP_PRIVATE_KEY_PEM_B64`; the private recovery key must be supplied by the owner through GitHub Secrets, never in chat or repository. Do not generate a replacement key because it would break upgrade continuity.
- The signed-release workflow includes certificate fingerprint verification and emulator install-over-upgrade smoke from v1.1.0 to v1.2.0. It has not been dispatched here; publishing requires the explicit `RELEASE` confirmation input and the recovery secret.
- No physical-device or production signing test is claimed. No merge to `main`.
