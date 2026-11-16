# نقشه راه مادر MolidoVPN AI — نسخه اجرایی

> وضعیت سند: مبنای رسمی ادامه توسعه در شاخه `feature/molidovpn-ai-redesign`.
> اصل حاکم: هیچ قابلیت، موتور، پروتکل، تست یا انتشار بدون مدرک قابل بازبینی «تکمیل‌شده» نیست. تیک‌های این سند فقط پس از شواهد واقعی تغییر می‌کنند.

## هدف محصول

ساخت کلاینت Android با رابط Premium و چندزبانه، تونل‌های واقعی و قابل‌آزمایش، موتورهای قابل تعویض، کشف و ارزیابی امن منابع سرور، انتخاب هوشمند محلی، حفاظت حریم خصوصی، بروزرسانی امن و فرایند انتشار قابل بازتولید. طراحی local-first است؛ کنترل‌پلین/سرورهای مدیریت فقط در صورت نیاز واقعی و با تصمیم زیرساختی جداگانه اضافه می‌شوند.

## اسناد اجرایی همراه

- [دفتر اجرای پروژه](MOLIDOVPN_AI_EXECUTION_LOG.md)
- [ممیزی اولیه هسته‌ها و موتورهای Android](MOLIDOVPN_AI_ENGINE_INVENTORY.fa.md)

## وضعیت پایه و محدودیت‌های شناخته‌شده

- مخزن: `hidooch980/MLH-VPN`.
- شاخه توسعه: `feature/molidovpn-ai-redesign`. شاخه `main` بدون تأیید صریح تغییر یا merge نمی‌شود.
- رابط Android از upstream مبتنی بر v2rayNG و overlay اختصاصی ساخته می‌شود؛ CI مستقل Android APK دارد.
- مسیر ساخت از `apps/android/build.sh` استفاده می‌کند، upstream را روی revision قفل‌شده checkout می‌کند، هسته‌های upstream را می‌سازد و تست‌های Gradle/Android را اجرا می‌کند.
- Xray-compatible stack در مسیر فعلی upstream است؛ وجود قابلیت import یا نام پروتکل در UI به‌تنهایی اثبات اجرای واقعی آن نیست.
- WireGuard، sing-box، Hysteria2، AmneziaWG و Psiphon باید جداگانه از نظر منبع، مجوز، سازگاری، بسته‌بندی و اتصال واقعی ارزیابی شوند؛ تا آن زمان «پیشنهادی/آزمایشی» هستند.
- اجرای تونل واقعی، IP خروجی، موقعیت، نشت DNS/IP و پایداری فقط با APK روی دستگاه و endpoint/config معتبر اثبات می‌شوند.
- کلید امضای نسخه‌های ارتقایی باید با کلید قبلی سازگار بماند. کلید خصوصی هرگز در Git یا گفتگو قرار نمی‌گیرد.

## قواعد اجرای پروژه

1. ابتدا ممیزی read-only، ثبت commit پایه، وضعیت CI و وابستگی‌ها.
2. توسعه فقط در شاخه فوق و با commitهای کوچک و قابل برگشت.
3. برای هر قابلیت: معیار پذیرش، پیاده‌سازی، تست واحد، تست یکپارچه، تست بسته‌بندی و مستندات.
4. پس از هر تغییر Android، CI همان commit بررسی می‌شود؛ خطاها قبل از کار بعدی مرتبط اصلاح می‌شوند.
5. تست ناموفق، در حال اجرا یا غیرقابل مشاهده هرگز موفق گزارش نمی‌شود.
6. هیچ endpoint، IP، کشور، سرور یا نتیجه سرعتی جعل نمی‌شود.
7. منابع عمومی/رایگان فقط با بررسی منبع، مجوز، اعتبارسنجی config و هشدار ریسک استفاده می‌شوند؛ منابع ناشناس به‌صورت خودکار اجرا نمی‌شوند.
8. داده‌های browsing، مقصدها، DNS queryها، credentialها و private config در log یا telemetry ثبت نمی‌شوند.
9. هیچ سرور ابری، هزینه، حساب، دامنه یا سرویس بیرونی بدون تصمیم/مجوز کاربر ایجاد نمی‌شود.
10. انتشار Stable فقط بعد از عبور همه gateهای مربوطه و حفظ امکان upgrade امن انجام می‌شود.

## فاز 0 — ممیزی و تثبیت پایه

### کارها
- [ ] ثبت commit پایه و وضعیت دقیق شاخه، فایل‌های تغییرکرده و CIهای فعال.
- [ ] ممیزی کامل معماری Android، overlay، navigation، ViewModel، storage، service و tunnel lifecycle.
- [ ] تهیه inventory از تمام binary/native engines، ABIها، نسخه‌ها، checksumها و مجوزها.
- [ ] بررسی upstream lock، build scripts، dependency pinning و license notices.
- [ ] اجرای تست‌های موجود و دسته‌بندی خطاها به blocker/critical/major/minor.
- [ ] بررسی اسرار، logging، cleartext traffic، exported components و مجوزهای Android.
- [ ] تهیه فهرست قابلیت‌های واقعی، ناقص، نمایشی و غایب.

### معیار خروج
گزارش ممیزی با مسیر فایل، commit، نتیجه تست و ریسک؛ بدون ابهام درباره اینکه کدام هسته واقعاً در APK بسته‌بندی می‌شود.

## فاز 1 — پایه پایدار Android و تجربه کاربری

### کارها
- [ ] تثبیت build reproducibility و ساخت APKهای debug برای ABIهای پشتیبانی‌شده.
- [ ] تکمیل هویت MolidoVPN AI، تم Premium، Side Drawer، RTL/LTR و زبان‌های فارسی/انگلیسی/عربی/ترکی.
- [ ] تکمیل ده مقصد: داشبورد، کشورها/سرورها، مغز AI، پروتکل‌ها، اشتراک/کانفیگ، امنیت، گزارش‌ها، پشتیبانی/عیب‌یابی، زبان/ظاهر، تنظیمات/درباره/بروزرسانی.
- [ ] اتصال هر صفحه به state واقعی؛ حذف دکمه‌ها و داده‌های نمایشی فاقد عملکرد.
- [ ] مدیریت خطا، حالت خالی، بارگذاری، آفلاین، دسترسی‌پذیری و اندازه‌های مختلف صفحه.
- [ ] حفظ تنظیمات کاربر و مهاجرت امن schema.

### معیار خروج
تست navigation و state، screenshot/smoke در emulator، تست RTL/LTR و نبود crash یا کنترل بی‌عملکرد در مسیرهای اصلی.

## فاز 2 — موتورهای تونل و رجیستری پروتکل

### اولویت موتور
1. Xray-compatible engine موجود در زنجیره upstream: ممیزی و تثبیت، بدون فرض پشتیبانی فراتر از آنچه آزموده شده.
2. sing-box: ارزیابی رسمی، مجوز، ABI، wrapper و تداخل سرویس؛ ابتدا آزمایشی.
3. WireGuard: ارزیابی backend/native tunnel مستقل و Android VpnService/Always-on؛ تست اتصال end-to-end.
4. Hysteria2: ترجیحاً از طریق sing-box در صورت پشتیبانی نسخه pin‌شده و آزمون‌شده.
5. AmneziaWG: آزمایشی و فقط پس از بررسی سازگاری کلاینت/سرور و مجوز.
6. Psiphon: فقط پس از بررسی منبع رسمی، شرایط استفاده، معماری و مدل اعتماد؛ خارج از مسیر Stable تا تأیید.
7. shadowsocks-rust: فقط اگر benchmark مزیت قابل اندازه‌گیری نسبت به موتور موجود نشان دهد.

### پروتکل‌های محصول
VLESS، VMess، Trojan، Shadowsocks، Hysteria2 و WireGuard در فهرست هدف هستند. هر مورد فقط وقتی در UI به‌عنوان قابل استفاده نشان داده می‌شود که parser، engine adapter، config validation، lifecycle و تست اتصال واقعی آن آماده باشد.

### کارها
- [ ] ایجاد Engine API واحد: capabilities، validateConfig، prepare، connect، disconnect، health، stats و cleanup.
- [ ] جداسازی adapter هر engine و جلوگیری از اجرای همزمان ناخواسته چند تونل.
- [ ] تشخیص پروتکل از config با parser امن؛ انتخاب دستی فقط برای engine سازگار.
- [ ] بررسی امضا/checksum فایل‌های native و pin کردن نسخه‌ها.
- [ ] آزمون ABIهای هدف و کاهش حجم APK با split per ABI.
- [ ] مدیریت crash، timeout، restart، resource cleanup و نسخه engine.
- [ ] بررسی GPL/LGPL/Apache/MIT و الزامات notice/source offer برای تمام وابستگی‌ها.

### معیار خروج
برای هر engine: build، license، ABI، import config، connect/disconnect، reconnect، network switch، leak test و گزارش benchmark موفق. موتور بدون این مدارک فقط Experimental است.

## فاز 3 — کشف، واردسازی و اعتبارسنجی منابع سرور

### منابع مجاز
- اشتراک یا کانفیگی که خود کاربر وارد می‌کند.
- کاتالوگ رسمی/مورد اعتماد و امضاشده.
- فهرست جامعه فقط با برچسب منبع غیرمعتبر، محدودیت نرخ، اعتبارسنجی و تأیید کاربر.

### کارها
- [ ] Search/Discovery provider interface با منابع مستقل و قابل خاموش‌کردن.
- [ ] دریافت امن HTTPS، محدودیت اندازه و زمان، redirect policy و جلوگیری از SSRF در هر سرویس واسط.
- [ ] parse امن URIها، dedup بر اساس شناسه/هش بدون افشای credential.
- [ ] امتیاز اعتماد منبع، تاریخ آخرین بررسی، وضعیت انقضا و گزارش خطا.
- [ ] اعتبارسنجی syntax و capability کانفیگ بدون اتصال خودکار به endpoint ناشناس.
- [ ] قرنطینه configهای خراب، تکراری، منقضی یا مشکوک.
- [ ] refresh زمان‌بندی‌شده با backoff، jitter و احترام به تنظیمات کاربر.
- [ ] نمایش منبع، آخرین بروزرسانی و علت رد شدن هر config.

### معیار خروج
fixture tests، malformed-input/fuzz tests، timeout/redirect tests، dedup tests و تأیید اینکه secretها در log/analytics نیستند.

## فاز 4 — موتور سنجش کیفیت و انتخاب هوشمند

### کارها
- [ ] سنجش handshake/connect time، latency، jitter، packet loss و موفقیت اتصال.
- [ ] throughput test اختیاری با مصرف داده محدود و رضایت کاربر.
- [ ] کیفیت‌سنجی پس از برقراری تونل؛ IP خروجی فقط از probe معتبر داخل مسیر تونل.
- [ ] امتیازدهی قابل توضیح با وزن‌های شفاف و داده‌های تازه.
- [ ] انتخاب AUTO، FAST، STABLE، COMPATIBLE و انتخاب دستی.
- [ ] hysteresis و حداقل اختلاف کیفیت برای جلوگیری از تعویض پی‌درپی.
- [ ] sticky session، cooldown، circuit breaker و quarantine برای endpointهای خراب.
- [ ] یادگیری محلی محدود و قابل reset؛ بدون ارسال browsing/DNS history.
- [ ] سازگاری انتخاب با باتری، نوع شبکه، محدودیت داده و ترجیح کشور.
- [ ] fallback کنترل‌شده و جلوگیری از retry storm.

### معیار خروج
تست deterministic برای ranking، stale metrics، tie-break، manual override، quarantine، hysteresis، قطع endpoint و عدم انتخاب سرور غیرقابل‌دسترسی.

## فاز 5 — امنیت و حریم خصوصی

- [ ] بررسی Android VpnService permission، foreground service و lifecycle.
- [ ] Always-on VPN و Block connections without VPN/Kill Switch؛ مستندسازی محدودیت‌های OS.
- [ ] DNS leak، IPv4/IPv6 leak و split-tunnel policy.
- [ ] جلوگیری از cleartext و اعتبارسنجی TLS/گواهی‌ها.
- [ ] ذخیره امن credential با Android Keystore و حذف امن هنگام revoke.
- [ ] عدم ثبت URI کامل کانفیگ، token، private key، DNS query یا مقصدهای کاربر.
- [ ] telemetry خاموش به‌صورت پیش‌فرض و opt-in واقعی با توضیح داده‌ها.
- [ ] threat model، dependency/SBOM scan، secret scan و بررسی مجوزها.
- [ ] مدیریت clipboard، QR و share بدون نمایش یا ذخیره ناخواسته اسرار.
- [ ] بررسی exported activities/services/receivers و deep links.

### معیار خروج
تست‌های امنیتی و leak در دستگاه واقعی، بازبینی threat model، بدون secret exposure و با مستندات حریم خصوصی.

## فاز 6 — IP و موقعیت خروجی و داشبورد

- [ ] تشخیص IP عمومی فقط پس از اثبات اتصال تونل.
- [ ] استفاده از provider قابل اعتماد و چندمنبعی برای مکان‌یابی تقریبی.
- [ ] نمایش IP و موقعیت کنار نشانگر نورانی اتصال.
- [ ] تفکیک وضعیت «متصل»، «در حال بررسی»، «نامشخص» و «خطا».
- [ ] جلوگیری از نمایش IP شبکه محلی به‌عنوان IP خروجی.
- [ ] عدم ذخیره IP یا location مگر با نیاز روشن و رضایت کاربر.

### معیار خروج
مقایسه با IP check مستقل قبل/بعد تونل، قطع provider، IPv6، تغییر endpoint و حالت disconnected.

## فاز 7 — پایداری و سازگاری

- [ ] آزمون Wi-Fi ↔ mobile data، airplane mode، roaming و تغییر DNS.
- [ ] آزمون sleep/wake، background restriction، battery saver و reboot.
- [ ] آزمون تماس/برنامه‌های دیگر و رقابت VPNها.
- [ ] آزمون soak طولانی، memory leak، ANR، crash و مصرف باتری.
- [ ] تست دستگاه ARM64 واقعی و حداقل نسخه Android اعلام‌شده.
- [ ] ماتریس دستگاه/ABI/نسخه OS و مستندسازی موارد ناسازگار.
- [ ] بازیابی خودکار پس از crash و جلوگیری از حلقه reconnect.

### معیار خروج
هیچ blocker/critical باز، بدون crash/ANR در سناریوهای پذیرش، reconnect پایدار و گزارش آزمون دستگاه واقعی.

## فاز 8 — اشتراک‌ها، کانفیگ و کنترل‌پلین (در صورت نیاز محصول)

- [ ] تفکیک کلاینت مستقل از API و سرورهای مدیریت.
- [ ] ثبت اشتراک، انقضا، محدودیت دستگاه و revoke فقط در صورت نیاز به حساب/اشتراک.
- [ ] صدور config کوتاه‌عمر و device-bound؛ عدم ارسال کلید خصوصی سرور به کلاینت.
- [ ] API authentication، rate limit، audit و admin RBAC.
- [ ] مدیریت کاتالوگ سرور، health و maintenance.
- [ ] انتخاب ارائه‌دهنده و هزینه زیرساخت فقط با تأیید کاربر.
- [ ] تست بار، backup/restore، patching، alerting و incident response.

### معیار خروج
end-to-end account/device/config/revoke، کنترل دسترسی و تست بار؛ بدون خلط control-plane با data-plane.

## فاز 9 — امضا، بروزرسانی و بازیابی

- [ ] تأیید applicationId، versionCode/versionName و ABI outputs.
- [ ] حفظ signing certificate fingerprint نسخه‌های موجود.
- [ ] بازیابی کلید قبلی از مسیر امن؛ کلید خصوصی هرگز وارد repo/chat نمی‌شود.
- [ ] امضای release و verify امضا با apksigner.
- [ ] manifest بروزرسانی امضاشده، HTTPS، hash و anti-rollback.
- [ ] دانلود resume-safe، فضای کافی، checksum و مدیریت خطا.
- [ ] نصب از مسیر رسمی Android Package Installer با تأیید کاربر.
- [ ] rollback/نسخه پشتیبان داده و تست upgrade از نسخه قبلی.
- [ ] انتشار changelog و اطلاع‌رسانی تغییرات.

### معیار خروج
تست install، upgrade با حفظ داده، رد APK دستکاری‌شده، رد downgrade غیرمجاز و بازیابی نسخه.

## فاز 10 — تضمین کیفیت و ممیزی انتشار

- [ ] Unit tests برای policy، parser، scoring، persistence و state.
- [ ] Integration tests برای هر engine adapter و tunnel lifecycle.
- [ ] Instrumentation/emulator smoke برای نصب، اجرا و navigation.
- [ ] Device tests برای اتصال واقعی، leak و تغییر شبکه.
- [ ] Fuzz tests برای parser و ورودی‌های اشتراک.
- [ ] Static analysis، lint، dependency audit، license audit و CodeQL.
- [ ] ساخت release reproducible و SBOM/checksum.
- [ ] تست regression تمام باگ‌های رفع‌شده.
- [ ] ثبت شواهد تست، artifact، commit و نسخه.

### معیار خروج
تمام gateهای CI و تست دستگاه برای قابلیت‌های اعلام‌شده موفق؛ هیچ خطای blocker/critical و هیچ هشدار امنیتی بدون تصمیم ثبت‌شده.

## فاز 11 — انتشار Stable و نگهداری

- [ ] Release candidate محدود و بدون فروش/تبلیغ قابلیت تأییدنشده.
- [ ] جمع‌آوری بازخورد اختیاری و کمینه‌سازی داده.
- [ ] رفع باگ و انتشار patch با همان کلید امضا.
- [ ] کانال stable/beta مجزا و rollback کنترل‌شده.
- [ ] برنامه پشتیبانی، گزارش رخداد، وصله امنیتی و چرخه بروزرسانی dependency.
- [ ] انتشار عمومی فقط پس از تأیید نهایی و بررسی حقوقی/حریم خصوصی متناسب با بازار هدف.

## Definition of Done

یک قابلیت فقط زمانی Done است که:
1. کد واقعی در شاخه ثبت شده باشد.
2. تست مرتبط خودکار و موفق داشته باشد.
3. تست integration یا دستگاه متناسب با ریسک موفق باشد.
4. خطاها و محدودیت‌ها مستند شده باشند.
5. secret، privacy و license review انجام شده باشد.
6. CI مربوط به همان commit موفق باشد.
7. artifact و checksum در دسترس باشد، اگر قابلیت نیاز به بسته‌بندی دارد.

## ترتیب اجرای فعلی

1. فاز 0: ممیزی کامل و تثبیت پایه.
2. فاز 1: build و UI.
3. فاز 2: تثبیت موتورهای واقعی و adapter contract.
4. فاز 3 و 4: کشف سرور و انتخاب هوشمند.
5. فاز 5 و 6: امنیت و صحت IP/موقعیت.
6. فاز 7: تست دستگاه و پایداری.
7. فاز 8 فقط اگر معماری محصول به control-plane نیاز داشته باشد.
8. فاز 9 و 10: امضا، QA و release gate.
9. فاز 11: انتشار Stable.

## دفتر اجرای روزانه

برای هر مرحله یک گزارش ثبت شود: تاریخ، commit، فایل‌های تغییرکرده، تست اجراشده، نتیجه، خطاهای باز، artifact و تصمیم بعدی. تا زمانی که مدرک تست ثبت نشده، وضعیت «در حال انجام» باقی می‌ماند.
