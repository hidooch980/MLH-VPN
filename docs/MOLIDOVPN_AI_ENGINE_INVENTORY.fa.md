# ممیزی اولیه هسته‌ها و موتورهای MolidoVPN AI

تاریخ ممیزی: 2026-11-16  
مرجع کد: شاخه `feature/molidovpn-ai-redesign`  
مبنای بسته‌بندی: Android preview workflow run `43916712315`، کد موتور انتخاب `876d4ecb550d58a8c1d58ee7e61ae0efd3ff6e83`.

## شواهد مستقیم زنجیره ساخت

- `apps/android/upstream.lock.json`، upstream را روی v2rayNG نسخه 2.3.15 و commit ثابت `57907cf354d9769200ed4717b4430d605abe0952` قفل می‌کند.
- کتابخانه Xray-compatible از AndroidLibXrayLite commit `2ffe5f794a32ba15b122e177b5acffc6f11c4959` و AAR با SHA-256 قفل‌شده دریافت می‌شود.
- HEV از submodule قفل‌شده `c5584feca8f6df70ee53eb2879e034b849a3d15a` از سورس کامپایل می‌شود.
- `apps/android/build.sh` قبل از Gradle، `prepare.py --download-core` و `compile-hevtun.sh` را اجرا می‌کند.
- `apps/android/verify_apks.py` وجود `libgojni.so`، `libhev-socks5-tunnel.so` و `libhevsockstun.so` را برای ABIهای خروجی بررسی می‌کند.
- APK آزمایشی ARM64 واقعاً شامل این کتابخانه‌هاست؛ در فهرست native libraryهای آن `libsingbox.so` یا کتابخانه‌ای با نام `wireguard-go` مشاهده نشد. این مشاهده به‌تنهایی ثابت نمی‌کند که هیچ مسیر دیگری برای پروتکل وجود ندارد؛ فقط ثابت می‌کند این دو نام binary در این APK نیستند.

## طبقه‌بندی فعلی

| جزء | شواهد موجود | وضعیت مجاز در محصول |
|---|---|---|
| Xray-compatible core | AAR قفل‌شده، `libgojni.so` در APK، build موفق | موجود در زنجیره؛ هر پروتکل/transport نیازمند تست مستقل |
| HEV tunnel | سورس قفل‌شده و دو کتابخانه native در APK | موجود در بسته؛ نقش دقیق هر مسیر باید در runtime trace و تست اتصال تثبیت شود |
| WireGuard | parser/import patch و تست قالب موجود؛ README آن را در قابلیت‌های upstream ذکر می‌کند | واردسازی/پشتیبانی مستندشده؛ اتصال واقعی و kill-switch هنوز تأیید نشده |
| Hysteria2 | formatter/test و مستندات upstream | مسیر config موجود؛ اتصال واقعی و سازگاری با نسخه pinned باید روی endpoint معتبر تست شود |
| sing-box | رجیستری آن را فقط با مدرک «کتابخانه مشاهده‌شده در APK ارسالی کاربر» ثبت کرده و در کاتالوگ قابل عرضه قرار نمی‌دهد | در APK preview فعلی به‌عنوان موتور native مستقل تأیید نشده؛ فقط Experimental/غیرفعال |
| AmneziaWG | در پیشنهاد هسته‌های آینده، نه در زنجیره build اثبات‌شده | برنامه آینده؛ هنوز اضافه نشده |
| Psiphon | فقط گزینه پیشنهادی در سند آینده | اضافه نشده |
| shadowsocks-rust | پیشنهاد مشروط به benchmark | اضافه نشده |

## نتیجه تست نسخه آزمایشی

- Workflow `43916712315`: success.
- Gradle unit tests: 170 passed، صفر failure، صفر skipped.
- Android emulator instrumentation: `OK (1 test)`.
- بسته‌بندی و بررسی هویت MolidoVPN AI و native libraryهای مورد انتظار موفق.
- این تست‌ها، اتصال به VPN server واقعی، نشت DNS/IP، عملکرد در تغییر شبکه، کیفیت سرعت یا مصرف باتری را اثبات نمی‌کنند.

## شکاف‌های مهم

1. پیاده‌سازی `SmartEngineOrchestrator` فعلاً coordinator مستقل است؛ هنوز به تصمیم‌گیری runtime و lifecycle واقعی تونل وصل نشده است.
2. باید interface یکپارچه Engine Adapter طراحی و به service واقعی متصل شود.
3. باید معلوم شود WireGuard در build دقیقاً از چه مسیر runtime اجرا می‌شود و با چه شرایطی با Xray/HEV هم‌زیستی دارد.
4. باید پشتیبانی Hysteria2 و تمام transportها با کانفیگ‌های معتبر و تست end-to-end تأیید شود.
5. sing-box نیازمند منبع رسمی، نسخه pin، checksum، مجوز، build ABI، adapter و تست مستقل است؛ صرف مشاهده کتابخانه در APK قدیمی/ارسالی کافی نیست.
6. kill switch، DNS/IPv4/IPv6 leak و network handover باید روی گوشی واقعی آزمایش شوند.
7. قبل از افزودن هسته جدید، حجم APK، مصرف RAM/CPU، زمان اتصال، throughput و battery benchmark با baseline مقایسه شود.

## تصمیم اجرایی

- ابتدا Xray-compatible core و HEV مسیر فعلی را ممیزی و تثبیت می‌کنیم.
- سپس قرارداد Engine Adapter و اتصال واقعی coordinator به مسیر انتخاب/اتصال را تکمیل می‌کنیم.
- sing-box را فقط پس از ارزیابی رسمی و benchmark به شاخه آزمایشی اضافه می‌کنیم.
- WireGuard و Hysteria2 را به‌صورت capabilityهای مستقل با test matrix تأیید می‌کنیم.
- هیچ موتور یا پروتکلی پیش از عبور از دروازه‌های release، Stable اعلام نمی‌شود.
