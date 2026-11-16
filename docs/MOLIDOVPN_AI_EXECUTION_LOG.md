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
