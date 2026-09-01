# یکپارچه‌سازی خروجی‌های تصویری (کارت‌ها، اسناد، PDF، کارت بانکی)

این سند توضیح می‌دهد چرا قبلاً هر کارت ظاهری متفاوت داشت و حالا قرارداد جدید چیست.
مرتبط با: `app/src/main/java/com/example/reports/`

## مشکل (خلاصهٔ ریشه‌ها)

1. **ماژول واحد وجود نداشت.** فایل `UnifiedImageGenerator.kt` بود، اما هیچ
   call-site ای آن را صدا نمی‌زد؛ ۱۱ تابع دیگر هرکدام Canvas/Paint/TextPaint و
   منطق چیدمان کپی‌شدهٔ خودشان را داشتند (`SharingUtils.kt` × ۷،
   `PatientCommUtils.kt` × ۳، `UnifiedImageGenerator` × ۱).
2. **تم نادیده گرفته می‌شد.** امضای توابع `themeSpec: ExportThemeSpec =
   ExportThemeSpec.Default` بود و `Default` یعنی تم `ThemeAmberClassic`
   (طلایی). دو call-site (`generateStandardPrescriptionCardImage` در
   `DentalScreens.kt` برای کارت نسخه، و `generateSimpleInfoCardImage` برای
   یادآوری نوبت) پارامتر را پاس نمی‌دادند ⇒ آن دو کارت همیشه طلایی.
3. **مقیاس فونت کاربر دور زده می‌شد.** `ExportThemeSpec.fromPalette(palette)`
   با پیش‌فرض `fontScaleIndex = 3` یعنی ۰.۸۵× (نه ۱×)؛ و ۷ از ۱۱ ژنراتور
   اصلاً `fontScaleMultiplier` را نمی‌خواندند.
4. **عرض هرکدام دلخواه بود:** ۱۰۰۰ / ۱۰۸۰ / ۱۲۰۰ / ۱۳۵۰(ثابت!) /
   `page.width * 2`. چون ارتفاع بیشتر کارت‌ها از محتوا می‌آمد، نسبت ابعاد بین
   خروجی‌ها فرق می‌کرد و در واتساپ/گالری نامتناسب دیده می‌شد.
5. **فونت:** هیچ `.ttf`/`.otf` در پروژه نبود و هیچ ژنراتوری `Typeface` ست
   نمی‌کرد ⇒ رندر کاملاً به فونت پیش‌فرض دستگاه وابسته بود. تنها استثنا
   `IranianBankHelper` بود که `Typeface.DEFAULT`/`MONOSPACE` می‌داد، پس کارت
   بانکی با بقیه فرق داشت.
6. رنگ‌های هارد‌کد در «اسناد روشن» (`#1E88E5`، `#E0F2FE`، `#F8FAFC`، …) باعث
   می‌شد عوض‌کردن تم تقریباً روی آن‌ها اثری نداشته باشد.
7. مسیر ذخیرهٔ فایل‌ها نامنظم بود (`cacheDir` ریشه، `cacheDir/reports`،
   `cacheDir/shared`) و بعضی ارتفاع‌ها ثابت بود (۱۳۵۰) ⇒ متن بریده یا فضای
   اضافه.

## قرارداد جدید

| مفهوم | تنها منبع حقیقت |
|---|---|
| ابعاد و حاشیه هر خروجی | `ExportDocKind` (`reports/ExportDocKind.kt`) |
| رندر بوم/متن/جدول/کادر/امضا | `UnifiedImageGenerator.generateUnifiedImage(...)` |
| رنگ‌ها | `ExportThemeSpec` (تم فعال + پلهٔ سایز فونت) و `ExportSemanticColors` (خطر/هشدار/موفقیت) |
| قلم (۴ چهره / ۳ لایه) | `AppFonts` (`assets/fonts/*.ttf`) — `Body` Vazirmatn · `Emphasized` Vazirmatn Bold · `Title` Estedad Bold · `Number` JetBrains Mono |
| ذخیرهٔ فایل | `ExportFiles` (همیشه `cache/shared`) |

### مسیر داده

```
DentalViewModel.currentExportTheme()   ← تم فعال + globalFontScaleIndex
        │  (همراه با ExportThemeSpec.install تا مسیرهای بدون ViewModel هم ببینند)
        ▼
generateXxxCardImage(...)  ── فقط یک UnifiedDocument توصیفی می‌سازد
        ▼
UnifiedImageGenerator.generateUnifiedImage(context, doc, theme)
        ▼
ExportFiles.writeBitmap → cache/shared/<prefix>_<ts>.png
```

### قواعدی که باید رعایت شود

* **پارامتر `themeSpec` هیچ پیش‌فرضی ندارد.** اگر جایی فراموش شود، بیلد
  می‌شکند (نه اینکه بی‌سروصدا طلایی رندر شود).
* هیچ تابع تصویرسازِ جدیدی نباید `Bitmap.createBitmap` / `Canvas` /
  `TextPaint` / `Paint().textSize` برای سند بسازد؛ یک `UnifiedDocument` با
  بلوک‌های `DocBlock.*` بسازید و به `generateUnifiedImage` بدهید.
  بلوک جدید لازم داشتید، **داخل رندرر** اضافه کنید نه در ژنراتور.
* عرض خروجی از `ExportDocKind` می‌آید. عرض جدید = یک enum member جدید با
  دلیل نوشته‌شده، نه یک عدد محلی.
* تمام اندازه‌ها در رندرر در `kind.scale` (× مقیاس فونت کاربر) ضرب می‌شوند،
  پس کارت ۱۰۸۰ و سند ۱۴۸۰ دقیقاً یک چگالی بصری دارند.
* ارتفاع همیشه از محتوا اندازه‌گیری می‌شود (دو پاس: چیدمان، بعد رندر).
* رنگ ثابت فقط در `ExportSemanticColors` مجاز است.
* قلم فقط از `AppFonts`؛ `Typeface.DEFAULT`/`MONOSPACE` و
  `FontFamily.Monospace` در UI ممنوع (استثنا: fallbackهای خودِ `AppFonts`).
* نقش متن را درست انتخاب کنید: `Role.Title` فقط برای عنوان/سرستون (فونت نمایشی)،
  تأکیدِ وسط پاراگراف `Role.Emphasized` است، و `Role.Number` باید با متنِ همان
  رشته صدا زده شود تا قاعدهٔ `AppFonts.isMonoSafeText` تصمیم بگیرد مونواسپیس
  باشد یا بدنه (متن فارسی هرگز با مونواسپیس رندر نمی‌شود).

### جدول عرضها (`ExportDocKind`)

مرجع طراحی 1080px است؛ `scale = widthPx / 1080` و همهٔ اندازه‌ها (فونت، حاشیه،
فاصله، شعاع، ضخامت خط) در همین ضریب × مقیاس فونت کاربر ضرب می‌شوند.

| خروجی | kind | عرض | چرا |
|---|---|---|---|
| کارت اطلاع‌رسانی (نوبت/یادآوری) | `InfoCard` | 1080 | بهینه برای پیش‌نمایش چت |
| صورت‌حساب / طرح درمان | `StatementCard` | 1080 | همان نسبت کارت چت |
| مراقبت پس از درمان | `PostOpCare` | 1240 | عرض A4 در 150DPI ⇒ نسبت کاغذ |
| فرم پذیرش و تاریخچه | `AdmissionForm` | 1240 | سند چاپی تک‌ستونه |
| رضایت‌نامه آگاهانه | `LegalConsent` | 1240 | سند چاپی تک‌ستونه |
| یادآوری/اخطار قسط | `InstallmentNotice` | 1240 | سند چاپی تک‌ستونه |
| نسخهٔ دارویی (جدول ۵ ستونه) | `Prescription` | 1480 | جدول پرتعداد؛ ستون‌ها نشکنند |
| فاکتور لابراتوار | `LabInvoice` | 1480 | جدول + مبالغ |
| صورتحساب طرف‌حساب | `VendorLedger` | 1480 | جدول ردیف‌های مالی |
| تصویر کارت بانکی | `BankCard` | 1000×630 | نسبت ISO/IEC 7810 ID-1 (۱٫۵۸۶) |

ارتفاع هیچ‌کدام ثابت نیست (از محتوا اندازه‌گیری می‌شود)؛ فقط `BankCard`
`fixedHeightPx = 630` دارد. این قواعد در `app/src/test/java/com/example/ExportLayoutTest.kt`
تست شده‌اند تا کسی دوباره عددی را هارد‌کند.

### تنها استثنا: کارت بانکی

`IranianBankHelper.generateCardImageBitmap` عمداً رندرر جداگانه دارد (یک کارت
بانکی است، نه سند A4/چت) اما از همان قراردادها پیروی می‌کند: ابعاد از
`ExportDocKind.BankCard` (۱۰۰۰×۶۳۰ ⇒ نسبت ISO/IEC 7810 ID-1)، همهٔ مختصات
با ضریب خودش مقیاس می‌شوند، قلم فقط `AppFonts` (شماره کارت/شبا با چهرهٔ Number)، و ذخیره از
طریق `ExportFiles`. CVV2 و تاریخ انقضا همچنان ماسک می‌شوند.

## فونت (وضعیت فعلی)

استاندارد سه‌لایهٔ برنامه نصب است (≈۵۰۰KB، همه OFL‑1.1، مجوزها در `LICENSES/`):
**Vazirmatn** (Regular + Bold) برای متن و تأکید، **Estedad Bold** برای عنوان‌ها و
سرستون‌ها، **JetBrains Mono Medium** برای ارقام و لاتینِ خالص. چون ارقام Vazirmatn
هم tabular است (عرض همهٔ ده رقم = ۱۱۵۰)، ستون‌های پول حتی بدون فونت مونواسپیس هم
تراز می‌مانند؛ و `AppFonts.isMonoSafeText` نگهبان این است که متن فارسی هرگز با
فونت لاتین رندر نشود. جزئیات، عدد و رقم‌ها و راهنمای تعویض هر لایه در
[docs/FONTS.md](FONTS.md). قواعد افزودن/تعویض خانواده و دلیلِ عدم‌نصب IranSansX (فونت پولی) در
[docs/FONTS.md](FONTS.md). اگر پوشه خالی باشد، همه‌چیز با فونت سیستم کار می‌کند
و بیلد نمی‌شکند؛ `app/src/test/java/com/example/BundledFontTest.kt` هم نگهبان
سلامت فایل‌های فونت است.
