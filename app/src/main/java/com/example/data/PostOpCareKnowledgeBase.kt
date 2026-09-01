package com.example.data

/**
 * پایگاه‌دانش «مراقبت‌های پس از درمان» و «هشدارهای بالینی بیمار».
 *
 * تفاوت این فایل با [PrescriptionKnowledgeBase]: آن‌جا **دارو** تجویز می‌شود،
 * این‌جا **رفتار بیمار** پس از هر جلسه توصیه می‌شود. پیش از این
 * `TherapyKnowledgeBase.generateDischargeInstructions` فقط یک فهرست ثابت از روی
 * عنوان درمان می‌داد و سه چیز را نادیده می‌گرفت:
 *   ۱) اینکه بیمار در کدام **جلسه** است (جلسهٔ جراحی با جلسهٔ کشیدن بخیه یکی نیست)،
 *   ۲) **بیماری‌های زمینه‌ای** بیمار،
 *   ۳) **داروهای مصرفی** بیمار.
 * این فایل هر سه را وارد می‌کند.
 *
 * منابع (تاریخ مراجعه: ۲۰۲۶):
 *  - SDCEP — Management of Dental Patients Taking Anticoagulants or Antiplatelet
 *    Drugs (2nd ed.): آستانهٔ INR<4 برای جراحی، عدم قطع خودسرانهٔ دارو، تدابیر
 *    موضعی بندآوری خون، و برنامه‌ریزی درمان در ساعات ابتدایی روز.
 *  - SDCEP — Oral Health Management of Patients at Risk of MRONJ (extant 2024):
 *    طبقه‌بندی ریسک بیس‌فسفونات/دنوسوماب و اولویت پیشگیری بر جراحی.
 *  - AAOMS / NHS Tayside / VDH — دستورالعمل‌های پس از کشیدن دندان: تشکیل و حفظ
 *    لختهٔ خون، ممنوعیت مکش و شست‌وشو در ۲۴ ساعت اول، دهان‌شویهٔ آب‌نمک از روز
 *    دوم، و علائم هشدار درای‌ساکت در روز سوم تا ششم.
 *  - AAE / American Association of Endodontists — مراقبت پس از درمان ریشه:
 *    پرهیز از جویدن تا ترمیم دائمی، شیوع «فلر‌آپ» حدود ۵٪، و ضرورت روکش.
 *  - ADA — Antibiotic Prophylaxis و AHA 2021 (آندوکاردیت عفونی).
 *  - AAPD — Reference Manual of Pediatric Dentistry (مراقبت‌های کودکان).
 *
 * هشدار: خروجی این پایگاه‌دانش «پیش‌نویس» است و پیش از ارسال باید به تأیید
 * پزشک برسد؛ جای قضاوت بالینی را نمی‌گیرد.
 */

/** فاز جلسه — تعیین می‌کند کدام دسته توصیه نمایش داده شود. */
enum class SessionPhase {
    /** جلسهٔ اصلی/جراحی (بیشترین محدودیت). */
    OPERATIVE,
    /** جلسهٔ میانی (مثلاً بین جلسات عصب‌کشی، با ترمیم موقت). */
    INTERIM,
    /** جلسهٔ پایانی/کشیدن بخیه/تحویل کار (سبک‌ترین توصیه‌ها). */
    FINAL
}

/** یک هشدار بالینی برخاسته از سابقهٔ پزشکی یا داروی مصرفی بیمار. */
data class ClinicalCareWarning(
    val title: String,
    val detail: String,
    val severity: FlagSeverity,
    /** منبع علمی پشتیبان، برای نمایش به پزشک. */
    val source: String
)

/** بستهٔ کامل توصیه‌های یک جلسه. */
data class PostOpCarePlan(
    val procedureLabel: String,
    val phase: SessionPhase,
    /** توصیه‌های عمومی همان درمان و همان فاز (خطاب به بیمار). */
    val instructions: List<String>,
    /** علائمی که بیمار باید فوراً گزارش کند. */
    val redFlags: List<String>,
    /** هشدارهای بالینی مخصوص این بیمار (خطاب به پزشک/پرونده). */
    val warnings: List<ClinicalCareWarning>,
    val sources: List<String>
) {
    fun isEmpty(): Boolean = instructions.isEmpty() && warnings.isEmpty()
}

private data class CareProfile(
    val key: String,
    val label: String,
    val matchKeywords: List<String>,
    val operative: List<String>,
    val interim: List<String> = emptyList(),
    val final: List<String> = emptyList(),
    val redFlags: List<String>,
    val sources: List<String>
)

object PostOpCareKnowledgeBase {

    // کلیدواژه‌هایی که نشان می‌دهند جلسه، جلسهٔ پایانی است.
    private val finalStageKeywords = listOf(
        "کشیدن بخیه", "برداشتن بخیه", "تحویل", "سمانتاسیون", "چسباندن", "پالیش",
        "کنترل", "فالوآپ", "پیگیری", "معاینه مجدد", "ویزیت مجدد", "نهایی",
        "delivery", "cementation", "follow", "recall", "suture removal"
    )

    // کلیدواژه‌هایی که نشان می‌دهند جلسه، جلسهٔ میانی است (کار ناتمام + ترمیم موقت).
    private val interimStageKeywords = listOf(
        "موقت", "بین جلسه", "جلسه دوم", "جلسه ۲", "پانسمان", "دارو گذاری",
        "داروگذاری", "temporary", "interim", "dressing"
    )

    fun phaseOf(stageText: String): SessionPhase {
        val t = stageText.lowercase().trim()
        if (t.isBlank()) return SessionPhase.OPERATIVE
        if (finalStageKeywords.any { t.contains(it.lowercase()) }) return SessionPhase.FINAL
        if (interimStageKeywords.any { t.contains(it.lowercase()) }) return SessionPhase.INTERIM
        return SessionPhase.OPERATIVE
    }

    private val profiles: List<CareProfile> = listOf(
        CareProfile(
            key = "extraction",
            label = "کشیدن دندان / جراحی دهان",
            matchKeywords = listOf(
                "کشیدن", "خارج کردن", "جراحی", "نهفته", "عقل", "اکستراکشن",
                "extraction", "surgical", "third molar"
            ),
            operative = listOf(
                "گاز استریل را ۳۰ تا ۶۰ دقیقه محکم گاز بگیرید؛ در صورت ادامهٔ خونریزی، گاز تازه بگذارید و ۳۰ دقیقهٔ دیگر فشار دهید.",
                "تا ۲۴ ساعت دهان را نشویید، تف نکنید، از نی استفاده نکنید و سیگار نکشید — این کارها لختهٔ خون را جابه‌جا می‌کند و باعث «درای‌ساکت» می‌شود.",
                "تا رفع کامل بی‌حسی (۲ تا ۴ ساعت) چیزی نخورید و مراقب باشید لب، گونه یا زبان را گاز نگیرید.",
                "روز اول فقط غذای نرم و خنک بخورید؛ از نوشیدنی و غذای داغ پرهیز کنید.",
                "۲۴ تا ۴۸ ساعت اول، هر ۲۰ دقیقه کیسهٔ یخ روی صورت (سمت عمل) بگذارید تا تورم کمتر شود.",
                "هنگام خواب سر را بالاتر از سطح بدن نگه دارید و روز اول از فعالیت سنگین و ورزش پرهیز کنید.",
                "از روز دوم، هر ۴ ساعت با آب‌نمک ولرم (یک قاشق چای‌خوری نمک در یک لیوان آب) دهان را آرام بشویید — بدون غرغرهٔ شدید.",
                "مسواک زدن را از همان روز ادامه دهید ولی تا ۳ تا ۵ روز محل جراحی را مسواک نزنید.",
                "سیگار دست‌کم ۳ روز (ترجیحاً بیشتر) ممنوع است؛ خطر درای‌ساکت را چند برابر می‌کند."
            ),
            final = listOf(
                "بخیه‌ها کشیده شد؛ از امروز می‌توانید محل را با مسواک نرم تمیز کنید.",
                "ادامهٔ شست‌وشو با آب‌نمک ولرم تا یک هفته توصیه می‌شود.",
                "در صورت باقی ماندن لبهٔ تیز استخوان یا زخم دیرالتیام، اطلاع دهید."
            ),
            redFlags = listOf(
                "درد شدیدی که روز سوم تا ششم بدتر شود و با مسکن آرام نشود (نشانهٔ درای‌ساکت).",
                "خونریزی فعالی که با ۳۰ دقیقه فشار مستقیم بند نیاید.",
                "تب، تورم پیش‌رونده، یا اشکال در بلع و باز کردن دهان.",
                "بی‌حسی ماندگار لب یا زبان بیش از ۲۴ ساعت."
            ),
            sources = listOf(
                "AAOMS / NHS Tayside — Advice following minor oral surgery or extraction",
                "Virginia Dept. of Health — Post-operative instructions (clot protection)"
            )
        ),
        CareProfile(
            key = "endodontic",
            label = "درمان ریشه (عصب‌کشی)",
            matchKeywords = listOf(
                "عصب", "ریشه", "اندو", "روت کانال", "پالپکتومی",
                "endodontic", "root canal", "rct"
            ),
            operative = listOf(
                "تا رفع کامل بی‌حسی چیزی نخورید تا لب و زبان را گاز نگیرید.",
                "تا ۳۰ دقیقه پس از درمان چیزی نخورید و ننوشید تا ترمیم موقت سفت شود.",
                "تا زمان گذاشتن ترمیم/روکش دائمی، با دندان درمان‌شده چیز سفت، چسبنده یا ترد نجوید؛ ترمیم موقت شکننده است.",
                "حساسیت و درد خفیف هنگام گاز گرفتن تا چند روز طبیعی است و به‌تدریج کم می‌شود.",
                "اگر احساس می‌کنید دندان درمان‌شده «زودتر از بقیه» به هم می‌خورد، حتماً اطلاع دهید تا بایت تنظیم شود؛ بی‌توجهی به آن التیام را به تأخیر می‌اندازد.",
                "مسواک و نخ دندان را طبق روال ادامه دهید."
            ),
            interim = listOf(
                "درمان ریشه هنوز کامل نشده و داخل دندان داروی موقت است؛ حتماً در موعد مقرر برای ادامهٔ کار مراجعه کنید.",
                "تا جلسهٔ بعد با آن سمت نجوید؛ شکستن ترمیم موقت باعث آلودگی مجدد کانال می‌شود.",
                "اگر ترمیم موقت افتاد، در اسرع وقت تماس بگیرید."
            ),
            final = listOf(
                "درمان ریشه کامل شد؛ برای جلوگیری از شکستن دندان، ترمیم دائمی یا روکش باید حداکثر تا یک ماه انجام شود.",
                "تا قرار گرفتن روکش، از جویدن مواد سفت با این دندان خودداری کنید.",
                "یک سال بعد برای رادیوگرافی کنترل و اطمینان از التیام مراجعه کنید."
            ),
            redFlags = listOf(
                "درد شدید، تورم یا ضربان‌دار شدن ناحیه (احتمال فلر‌آپ — حدود ۵٪ موارد).",
                "افتادن ترمیم موقت یا شکستن دندان.",
                "تب یا تورم صورت."
            ),
            sources = listOf(
                "American Association of Endodontists — Post-operative instructions & flare-up incidence (~5%)",
                "Endodontic post-op guidance — permanent restoration within 3–4 weeks"
            )
        ),
        CareProfile(
            key = "implant",
            label = "کاشت ایمپلنت",
            matchKeywords = listOf("ایمپلنت", "کاشت", "implant", "پیوند استخوان", "سینوس لیفت"),
            operative = listOf(
                "تا ۲۴ ساعت دهان را نشویید و تف نکنید؛ لختهٔ خون نباید جابه‌جا شود.",
                "۴۸ ساعت اول کمپرس یخ متناوب روی صورت بگذارید.",
                "غذای نرم و خنک بخورید و تا چند روز از سمت عمل‌شده نجوید.",
                "سیگار جدی‌ترین عامل شکست ایمپلنت است؛ دست‌کم دو هفته کامل ترک شود.",
                "از روز دوم، دهان‌شویهٔ کلرهگزیدین طبق دستور و شست‌وشوی ملایم با آب‌نمک.",
                "اگر پروتز متحرک دارید، طبق دستور پزشک تا چند هفته آن را کمتر استفاده کنید (به‌ویژه شب‌ها).",
                "از ورزش سنگین تا یک هفته پرهیز کنید."
            ),
            final = listOf(
                "پروتز روی ایمپلنت تحویل شد؛ بهداشت اطراف ایمپلنت (نخ دندان مخصوص/براش بین‌دندانی) حیاتی است.",
                "جلسات کنترل دوره‌ای را جدی بگیرید؛ پری‌ایمپلانتیت در مراحل اولیه بدون درد است."
            ),
            redFlags = listOf(
                "لق شدن ایمپلنت یا پروتز.",
                "تورم پیش‌رونده، ترشح چرکی یا تب.",
                "بی‌حسی ماندگار لب پایین."
            ),
            sources = listOf(
                "ITI Treatment Guide — post-surgical protocols",
                "SDCEP / AAOMS — implant surgery aftercare"
            )
        ),
        CareProfile(
            key = "perio",
            label = "درمان لثه / جرم‌گیری",
            matchKeywords = listOf(
                "جرم", "لثه", "پریو", "فلپ", "کورتاژ", "بروساژ",
                "scaling", "perio", "srp"
            ),
            operative = listOf(
                "حساسیت دندان‌ها به سرما تا چند روز طبیعی است و کم می‌شود.",
                "خونریزی خفیف لثه در روزهای اول طبیعی است؛ مسواک زدن را قطع نکنید چون قطع آن التهاب را بیشتر می‌کند.",
                "از مسواک نرم و تکنیک صحیح استفاده کنید و نخ دندان را روزانه ادامه دهید.",
                "در صورت تجویز، دهان‌شویهٔ کلرهگزیدین را بعد از مسواک استفاده کنید (نه هم‌زمان با خمیردندان).",
                "تا ۲۴ ساعت از غذاهای بسیار داغ، تند و اسیدی پرهیز کنید."
            ),
            final = listOf(
                "برای حفظ نتیجه، جرم‌گیری دوره‌ای طبق برنامهٔ پزشک ضروری است.",
                "بازبینی تکنیک مسواک زدن در هر جلسهٔ کنترل انجام می‌شود."
            ),
            redFlags = listOf(
                "خونریزی خودبه‌خودی و ادامه‌دار لثه.",
                "آبسه یا تورم دردناک لثه.",
                "لق شدن پیش‌رونده دندان‌ها."
            ),
            sources = listOf("EFP / AAP — periodontal therapy aftercare")
        ),
        CareProfile(
            key = "restorative",
            label = "ترمیم / پرکردن",
            matchKeywords = listOf(
                "ترمیم", "پرکردن", "کامپوزیت", "آمالگام", "بیلدآپ", "اینله", "انله",
                "filling", "composite", "restoration"
            ),
            operative = listOf(
                "اگر بی‌حسی دارید تا رفع کامل آن چیزی نخورید.",
                "ترمیم کامپوزیت بلافاصله سفت می‌شود و محدودیت زمانی برای غذا خوردن ندارد؛ ترمیم آمالگام تا ۲۴ ساعت نباید تحت فشار جویدن سنگین قرار گیرد.",
                "حساسیت گذرا به سرما و گرما تا چند هفته می‌تواند طبیعی باشد.",
                "اگر بایت بلند است یا دندان زودتر تماس می‌گیرد، حتماً برای تنظیم مراجعه کنید."
            ),
            redFlags = listOf(
                "درد خودبه‌خودی، شبانه یا ضربان‌دار (احتمال درگیری پالپ).",
                "شکستن یا افتادن ترمیم.",
                "ماندگار شدن حساسیت بیش از چند هفته."
            ),
            sources = listOf("ADA — restorative post-treatment guidance")
        ),
        CareProfile(
            key = "crown",
            label = "روکش و پروتز ثابت",
            matchKeywords = listOf("روکش", "بریج", "لمینت", "ونیر", "crown", "bridge", "veneer"),
            operative = listOf(
                "با روکش موقت از جویدن مواد چسبنده (آدامس، سوهان‌عسل) و سفت خودداری کنید.",
                "هنگام کشیدن نخ دندان کنار روکش موقت، نخ را از پهلو بیرون بکشید نه به سمت بالا، تا روکش کنده نشود.",
                "حساسیت گذرا به سرما و گرما تا زمان چسباندن روکش دائمی طبیعی است.",
                "اگر روکش موقت افتاد آن را نگه دارید و سریع تماس بگیرید؛ حرکت دندان‌ها باعث نچسبیدن روکش دائمی می‌شود."
            ),
            final = listOf(
                "روکش دائمی چسبانده شد؛ تا یک ساعت چیزی نخورید و تا ۲۴ ساعت از مواد بسیار سفت پرهیز کنید.",
                "بهداشت لبهٔ روکش و نخ دندان روزانه، عامل اصلی طول عمر آن است.",
                "در صورت احساس بلندی بایت، برای تنظیم مراجعه کنید."
            ),
            redFlags = listOf(
                "درد هنگام گاز گرفتن پس از چسباندن دائمی.",
                "بوی بد یا التهاب لثهٔ اطراف روکش.",
                "لق شدن یا افتادن روکش دائمی."
            ),
            sources = listOf("ADA / ITI — fixed prosthodontics aftercare")
        ),
        CareProfile(
            key = "pediatric",
            label = "درمان کودکان",
            matchKeywords = listOf(
                "اطفال", "کودک", "پالپوتومی", "شیری", "فلوراید", "فیشور",
                "ssc", "pediatric", "space maintainer", "فضانگهدار"
            ),
            operative = listOf(
                "تا ۲ ساعت پس از بی‌حسی مراقب باشید کودک لب، گونه یا زبان خود را گاز نگیرد — این شایع‌ترین آسیب پس از درمان کودکان است.",
                "تا رفع بی‌حسی فقط نوشیدنی خنک بدهید.",
                "پس از فلورایدتراپی، تا ۳۰ دقیقه چیزی نخورد و ننوشد.",
                "غذای نرم برای باقی روز؛ از تنقلات چسبنده و شیرین پرهیز شود.",
                "مسواک زدن کودک تا سن ۸ سالگی باید زیر نظر و با کمک والدین باشد."
            ),
            final = listOf(
                "در صورت نصب فضانگهدار، از خوردن آدامس و مواد چسبنده پرهیز شود و جلسات کنترل رویش دندان دائمی رعایت شود."
            ),
            redFlags = listOf(
                "تورم صورت، تب یا بی‌قراری شدید کودک.",
                "افتادن یا شکستن روکش/فضانگهدار.",
                "زخم بزرگ ناشی از گاز گرفتن لب."
            ),
            sources = listOf("AAPD — Reference Manual of Pediatric Dentistry")
        ),
        CareProfile(
            key = "ortho",
            label = "ارتودنسی",
            matchKeywords = listOf("ارتودنسی", "براکت", "الاینر", "سیم", "orthodontic", "aligner", "retainer"),
            operative = listOf(
                "درد و فشار تا ۳ تا ۵ روز پس از هر فعال‌سازی طبیعی است؛ غذای نرم کمک می‌کند.",
                "از گاز زدن مستقیم غذاهای سفت (سیب، هویج، نان سنگک) خودداری کنید و آن‌ها را قطعه‌قطعه کنید.",
                "آدامس، سوهان‌عسل و مواد چسبنده باعث کنده شدن براکت می‌شود.",
                "در صورت زخم شدن گونه، از موم ارتودنسی روی براکت استفاده کنید.",
                "الاینر باید روزانه ۲۰ تا ۲۲ ساعت در دهان باشد؛ فقط هنگام غذا خوردن خارج شود.",
                "بهداشت دهان در ارتودنسی حیاتی است؛ بی‌توجهی به آن لکهٔ سفید دائمی روی مینا باقی می‌گذارد."
            ),
            final = listOf(
                "پس از برداشتن دستگاه، استفادهٔ منظم از پلاک نگهدارنده تنها عامل جلوگیری از برگشت دندان‌هاست.",
                "پلاک را با آب سرد و مسواک تمیز کنید (آب داغ آن را تغییر شکل می‌دهد)."
            ),
            redFlags = listOf(
                "سیم آزادشده‌ای که بافت را زخم می‌کند.",
                "درد شدید و ادامه‌دار بیش از یک هفته.",
                "لق شدن غیرعادی دندان."
            ),
            sources = listOf("AAO — orthodontic patient care instructions")
        ),
        CareProfile(
            key = "whitening",
            label = "سفید کردن (بلیچینگ)",
            matchKeywords = listOf("بلیچینگ", "سفید", "whitening", "bleaching"),
            operative = listOf(
                "تا ۴۸ ساعت از مواد رنگ‌زا (چای، قهوه، سس، آب‌میوهٔ رنگی، سیگار) کاملاً پرهیز کنید.",
                "حساسیت گذرا به سرما تا چند روز شایع است و خودبه‌خود برطرف می‌شود.",
                "در صورت حساسیت زیاد، از خمیردندان مخصوص دندان حساس استفاده کنید."
            ),
            redFlags = listOf("حساسیت شدید و ادامه‌دار بیش از یک هفته.", "سوختگی یا سفیدشدگی لثه."),
            sources = listOf("ADA — tooth whitening safety guidance")
        ),
        CareProfile(
            key = "denture",
            label = "پروتز متحرک",
            matchKeywords = listOf("پروتز متحرک", "دنچر", "پارسیل", "denture"),
            operative = listOf(
                "روزهای اول احساس حجم و افزایش بزاق طبیعی است و بدن به‌تدریج عادت می‌کند.",
                "شب‌ها پروتز را خارج کرده و در آب نگه دارید تا لثه استراحت کند.",
                "پروتز را با مسواک و آب (نه خمیردندان ساینده) و بیرون از سینک پرآب تمیز کنید.",
                "در صورت ایجاد زخم، پروتز را نکشید بلکه برای تنظیم مراجعه کنید — چند ساعت قبل از مراجعه آن را بگذارید تا محل زخم مشخص شود."
            ),
            redFlags = listOf("زخم دیرالتیام بیش از دو هفته.", "لقی شدید یا شکستن پروتز."),
            sources = listOf("ADA — removable prosthodontics home care")
        )
    )

    // ================================================================================
    // هشدارهای بالینی بر اساس بیماری‌ها و داروهای فرم پذیرش
    // ================================================================================

    private data class WarningRule(
        val triggers: List<String>,
        val title: String,
        val detail: String,
        val severity: FlagSeverity,
        val source: String,
        /** اگر مقدار داشته باشد، این هشدار فقط برای درمان‌های خونین/جراحی نمایش داده می‌شود. */
        val surgicalOnly: Boolean = false
    )

    private val warningRules: List<WarningRule> = listOf(
        WarningRule(
            triggers = listOf("وارفارین", "ضدانعقاد", "رقیق‌کننده خون", "رقیق کننده خون", "warfarin", "کوماردین"),
            title = "مصرف وارفارین — کنترل INR پیش از جراحی",
            detail = "طبق SDCEP، برای اعمال خونین باید INR حداکثر ۲۴ ساعت قبل (در بیمار پایدار تا ۷۲ ساعت) اندازه‌گیری شود. اگر INR کمتر از ۴ باشد درمان بدون قطع دارو انجام می‌شود. دارو هرگز نباید خودسرانه قطع شود؛ خطر ترومبوآمبولی از خطر خونریزی بیشتر است. درمان را اول وقت روز و محدود برنامه‌ریزی کنید و از تدابیر موضعی (بخیه، اسفنج ژلاتینی، فشار با گاز) استفاده کنید.",
            severity = FlagSeverity.CONTRAINDICATION,
            source = "SDCEP — Management of Dental Patients Taking Anticoagulants (2nd ed.)",
            surgicalOnly = true
        ),
        WarningRule(
            triggers = listOf("ریواروکسابان", "آپیکسابان", "دابیگاتران", "ادوکسابان", "doac"),
            title = "مصرف ضدانعقاد خوراکی مستقیم (DOAC)",
            detail = "برای اعمال با ریسک خونریزی پایین، دارو قطع نمی‌شود. برای اعمال پرخطر، طبق SDCEP دوز صبح روز درمان حذف (آپیکسابان، دابیگاتران) یا به تعویق انداخته می‌شود (ریواروکسابان، ادوکسابان) — حتماً با پزشک معالج هماهنگ شود.",
            severity = FlagSeverity.CONTRAINDICATION,
            source = "SDCEP — Anticoagulants & Antiplatelet Drugs",
            surgicalOnly = true
        ),
        WarningRule(
            triggers = listOf("آسپرین", "پلاویکس", "کلوپیدوگرل", "ضدپلاکت", "aspirin", "clopidogrel"),
            title = "داروی ضدپلاکت — بدون قطع دارو",
            detail = "طبق SDCEP، درمان دندانپزشکی در بیماران تحت درمان تک‌دارویی یا دودارویی ضدپلاکت بدون قطع دارو انجام می‌شود؛ کنترل خونریزی با تدابیر موضعی. به بیمار تأکید شود تا ۲۴ ساعت دهان را نشوید و از مکش پرهیز کند.",
            severity = FlagSeverity.CAUTION,
            source = "SDCEP / Eur Heart J review of antiplatelet therapy in dentistry",
            surgicalOnly = true
        ),
        WarningRule(
            triggers = listOf("بیس فسفونات", "بیس‌فسفونات", "آلندرونات", "زولدرونیک", "دنوسوماب", "پرولیا", "فوسامکس", "استئونکروز"),
            title = "ریسک استئونکروز فک (MRONJ)",
            detail = "طبق SDCEP، بیمار نباید از مصرف دارو یا از درمان دندانپزشکی منع شود، ولی اولویت با پیشگیری و پرهیز از جراحی استخوانی است. پیش از کشیدن دندان یا ایمپلنت، رضایت آگاهانهٔ کتبی با ذکر ریسک MRONJ گرفته شود و با پزشک تجویزکننده مشورت شود. بیمارانی که دنوسوماب را در ۹ ماه گذشته مصرف کرده‌اند نیز پرخطر محسوب می‌شوند.",
            severity = FlagSeverity.CONTRAINDICATION,
            source = "SDCEP — Oral Health Management of Patients at Risk of MRONJ (extant 2024)",
            surgicalOnly = true
        ),
        WarningRule(
            triggers = listOf("دیابت", "قند خون", "انسولین", "متفورمین", "دیابتی"),
            title = "دیابت — التیام کندتر و ریسک عفونت",
            detail = "قرار ملاقات ترجیحاً صبح و پس از صرف وعدهٔ غذایی و داروی معمول بیمار تنظیم شود تا از افت قند جلوگیری شود. کنترل ضعیف قند، التیام زخم را کند و ریسک عفونت و درای‌ساکت را زیاد می‌کند. در صورت تجویز کورتون، قند خون پایش شود.",
            severity = FlagSeverity.CAUTION,
            source = "ADA — Dental management of patients with diabetes"
        ),
        WarningRule(
            triggers = listOf("فشار خون", "فشارخون", "هایپرتنشن", "hypertension"),
            title = "فشار خون بالا",
            detail = "پیش از عمل فشار خون کنترل شود. بی‌حسی حاوی اپی‌نفرین در فشار خون کنترل‌نشده با احتیاط و در حداقل مقدار استفاده شود. NSAID طولانی‌مدت می‌تواند اثر داروهای فشار خون را کم کند.",
            severity = FlagSeverity.CAUTION,
            source = "AHA / ADA — cardiovascular considerations in dental care"
        ),
        WarningRule(
            triggers = listOf("دریچه مصنوعی", "اندوکاردیت", "بیماری مادرزادی قلب", "پیوند قلب"),
            title = "نیاز به پروفیلاکسی آندوکاردیت",
            detail = "بیمار در گروه پرخطر است؛ طبق AHA 2021 پیش از اعمال همراه با دستکاری لثه یا مخاط، آموکسی‌سیلین ۲ گرم یک ساعت قبل تجویز شود. کلیندامایسین دیگر توصیه نمی‌شود.",
            severity = FlagSeverity.CONTRAINDICATION,
            source = "AHA 2021 Scientific Statement — Prevention of Viridans Group Streptococcal IE"
        ),
        WarningRule(
            triggers = listOf("پیس میکر", "pace maker", "pacemaker", "ضربان ساز", "ضربان‌ساز", "دفیبریلاتور"),
            title = "پیس‌میکر / ICD",
            detail = "از دستگاه‌های الکتروسرجری، جرم‌گیر اولتراسونیک و مگنت‌های قوی در مجاورت دستگاه با احتیاط استفاده شود؛ در صورت امکان از ابزار دستی استفاده گردد.",
            severity = FlagSeverity.CAUTION,
            source = "ADA — electromagnetic interference with cardiac devices"
        ),
        WarningRule(
            triggers = listOf("شیمی درمانی", "شیمی‌درمانی", "پرتودرمانی", "رادیوتراپی", "سرطان", "chemotherapy"),
            title = "شیمی‌درمانی / پرتودرمانی",
            detail = "ریسک موکوزیت، عفونت فرصت‌طلب و استئورادیونکروز (در پرتودرمانی سر و گردن) بالاست. اعمال خونین باید با هماهنگی انکولوژیست و با توجه به شمارش نوتروفیل و پلاکت زمان‌بندی شود؛ ترجیحاً پیش از شروع درمان، دهان از نظر دندانی آماده شود.",
            severity = FlagSeverity.CONTRAINDICATION,
            source = "MASCC/ISOO & NCI — oral complications of cancer therapy",
            surgicalOnly = true
        ),
        WarningRule(
            triggers = listOf("بارداری", "حاملگی", "باردار"),
            title = "بارداری",
            detail = "ایمن‌ترین بازه برای درمان‌های انتخابی، سه‌ماههٔ دوم است. در سه‌ماههٔ سوم بیمار نباید مدت طولانی به پشت بخوابد (سندرم فشار ورید اجوف). رادیوگرافی فقط در صورت ضرورت و با پیش‌بند سربی. NSAID به‌ویژه در سه‌ماههٔ سوم و داکسی‌سایکلین در تمام دوران ممنوع است.",
            severity = FlagSeverity.CONTRAINDICATION,
            source = "ACOG / ADA — Oral health care during pregnancy"
        ),
        WarningRule(
            triggers = listOf("آسم", "تنفسی", "asthma", "copd"),
            title = "آسم / بیماری تنفسی",
            detail = "اسپری استنشاقی بیمار باید در دسترس باشد. از NSAID در بیماران مبتلا به آسم حساس به آسپرین پرهیز شود. استرس و درد می‌تواند حملهٔ آسم را تحریک کند.",
            severity = FlagSeverity.CAUTION,
            source = "ADA — management of patients with respiratory disease"
        ),
        WarningRule(
            triggers = listOf("صرع", "تشنج", "epilepsy", "seizure"),
            title = "صرع / سابقهٔ تشنج",
            detail = "جلسات کوتاه و کم‌استرس برنامه‌ریزی شود و بیمار داروی ضدتشنج خود را طبق روال مصرف کرده باشد. رشد لثه ناشی از فنی‌توئین نیازمند مراقبت پریودنتال ویژه است.",
            severity = FlagSeverity.CAUTION,
            source = "ADA — seizure disorders and dental care"
        ),
        WarningRule(
            triggers = listOf("کلیوی", "دیالیز", "کلیه", "renal"),
            title = "نارسایی کلیوی / دیالیز",
            detail = "درمان در روز غیردیالیز (معمولاً فردای دیالیز) انجام شود چون بیمار در روز دیالیز هپارینه است. دوز داروهای دفع‌کلیوی بر اساس GFR تنظیم و از NSAID پرهیز شود.",
            severity = FlagSeverity.CONTRAINDICATION,
            source = "KDIGO / ADA — dental care in chronic kidney disease"
        ),
        WarningRule(
            triggers = listOf("کبدی", "هپاتیت", "سیروز", "الکلیسم"),
            title = "بیماری کبدی",
            detail = "اختلال انعقادی و کاهش متابولیسم داروها محتمل است؛ پیش از جراحی PT/INR بررسی شود. حداکثر دوز استامینوفن کاهش یابد و از داروهای کبدی‌سمی پرهیز شود.",
            severity = FlagSeverity.CONTRAINDICATION,
            source = "AASLD / ADA — dental management in liver disease"
        ),
        WarningRule(
            triggers = listOf("هموفیلی", "اختلال انعقاد", "کم‌خونی", "کم خونی", "آنمی", "پلاکت"),
            title = "اختلال خونی / انعقادی",
            detail = "پیش از هر عمل خونین، هماهنگی با هماتولوژیست و بررسی شمارش پلاکت و فاکتورهای انعقادی الزامی است. تدابیر موضعی بندآوری خون و در صورت لزوم ترانگزامیک اسید موضعی به کار رود.",
            severity = FlagSeverity.CONTRAINDICATION,
            source = "WFH — Guidelines for the management of hemophilia (dental care)",
            surgicalOnly = true
        ),
        WarningRule(
            triggers = listOf("سیگار", "دخانیات", "قلیان", "smoking"),
            title = "مصرف دخانیات",
            detail = "سیگار ریسک درای‌ساکت را بیش از سه برابر می‌کند (حدود ۱۳٪ در سیگاری‌ها در برابر ۴٪) و مهم‌ترین عامل شکست ایمپلنت است. ترک دست‌کم ۳ روز پس از کشیدن دندان و ۲ هفته پس از ایمپلنت تأکید شود.",
            severity = FlagSeverity.CAUTION,
            source = "Systematic review — smoking and alveolar osteitis incidence"
        ),
        WarningRule(
            triggers = listOf("پروتز مفصل", "پروتز زانو", "پروتز لگن", "مفصل مصنوعی"),
            title = "پروتز مفصلی — بدون نیاز به پروفیلاکسی معمول",
            detail = "طبق بازنگری AAOS نوامبر ۲۰۲۴ هم‌راستا با ADA، پروفیلاکسی آنتی‌بیوتیکی معمول پیش از درمان دندانپزشکی توصیه نمی‌شود مگر با صلاحدید صریح جراح ارتوپد.",
            severity = FlagSeverity.INFO,
            source = "AAOS/ADA — Nov 2024 appropriate use review"
        ),
        WarningRule(
            triggers = listOf("پنی‌سیلین", "پنی سیلین", "آلرژی", "حساسیت", "آنافیلاکسی", "لاتکس"),
            title = "سابقهٔ آلرژی",
            detail = "نوع و شدت واکنش پیش از تجویز دارو یا استفاده از دستکش لاتکس بررسی و در پرونده ثبت شود. در آلرژی شدید/آنافیلاکسی به بتالاکتام‌ها، سفالکسین نیز به دلیل واکنش متقاطع کنار گذاشته شود.",
            severity = FlagSeverity.CONTRAINDICATION,
            source = "AHA 2021 / ADA — antibiotic allergy management"
        ),
        WarningRule(
            triggers = listOf("کورتون", "پردنیزولون", "دگزامتازون", "استروئید", "corticosteroid"),
            title = "مصرف طولانی‌مدت کورتیکواستروئید",
            detail = "احتمال سرکوب محور آدرنال و کاهش پاسخ به استرس جراحی وجود دارد؛ در اعمال وسیع با پزشک معالج دربارهٔ نیاز به دوز پوششی مشورت شود. التیام کندتر و ریسک عفونت بیشتر است.",
            severity = FlagSeverity.CAUTION,
            source = "Endocrine Society / ADA — perioperative steroid considerations"
        ),
        WarningRule(
            triggers = listOf("تیروئید", "هایپوتیروئید", "هایپرتیروئید", "thyroid"),
            title = "اختلال تیروئید",
            detail = "در پرکاری کنترل‌نشدهٔ تیروئید، اپی‌نفرین با احتیاط مصرف شود (خطر بحران تیروئیدی). در کم‌کاری شدید، حساسیت بیشتری به داروهای آرام‌بخش وجود دارد.",
            severity = FlagSeverity.CAUTION,
            source = "ADA — endocrine disorders in dental practice"
        ),
        WarningRule(
            triggers = listOf("hiv", "ایدز", "نقص ایمنی", "immunocompromised", "پیوند عضو"),
            title = "نقص ایمنی",
            detail = "ریسک عفونت و کاندیدیازیس دهانی بالاتر است. در صورت شمارش پایین نوتروفیل، اعمال خونین با هماهنگی پزشک معالج و پوشش آنتی‌بیوتیکی انجام شود.",
            severity = FlagSeverity.CAUTION,
            source = "CDC / ADA — dental care for immunocompromised patients"
        )
    )

    private val surgicalKeywords = listOf(
        "کشیدن", "جراحی", "ایمپلنت", "کاشت", "نهفته", "عقل", "پیوند", "فلپ",
        "extraction", "surgical", "implant", "graft"
    )

    private fun isSurgical(treatmentTitle: String, stageText: String): Boolean {
        val c = "$treatmentTitle $stageText".lowercase()
        return surgicalKeywords.any { c.contains(it.lowercase()) }
    }

    /**
     * ترتیب اولویت تطبیق — از خاص به عام.
     *
     * این ترتیب عمدی است و نباید به ترتیب تعریف [profiles] واگذار شود، چون
     * عنوان‌های ترکیبی به چند پروفایل می‌خورند و باید «محدودکننده‌ترین و
     * اختصاصی‌ترین» توصیه برنده شود. نمونه‌ها:
     *  - «جراحی ایمپلنت» هم «جراحی» دارد هم «ایمپلنت» → باید ایمپلنت باشد.
     *  - «جراحی فلپ لثه» هم «جراحی» دارد هم «لثه» → باید پریو باشد.
     *  - «عصب‌کشی و ترمیم» هم اندو است هم ترمیم → محدودیت اصلی مال اندوست.
     *  - «روکش استیل شیری» درمان کودک است، نه پروتز بزرگسال.
     */
    private val matchPriority = listOf(
        "pediatric",   // نشانگرهای صریح کودک (شیری/اطفال/کودک) بر همه مقدم است
        "implant",
        "ortho",
        "whitening",
        "denture",
        "endodontic",
        "crown",
        "perio",       // «جراحی لثه» باید پریو باشد نه کشیدن دندان
        "extraction",
        "restorative"  // عام‌ترین، آخرین گزینه
    )

    /** یافتن پروفایل مراقبتی متناسب با عنوان درمان و متن جلسه. */
    private fun matchProfile(treatmentTitle: String, stageText: String): CareProfile? {
        val combined = "${treatmentTitle.lowercase()} ${stageText.lowercase()}".trim()
        if (combined.isBlank()) return null
        for (key in matchPriority) {
            val p = profiles.firstOrNull { it.key == key } ?: continue
            if (p.matchKeywords.any { combined.contains(it.lowercase()) }) return p
        }
        return null
    }

    /**
     * هشدارهای بالینی مخصوص بیمار بر اساس بیماری‌های زمینه‌ای و داروهای مصرفی
     * ثبت‌شده در فرم پذیرش.
     */
    fun warningsForPatient(
        medicalHistory: String,
        medicalAlerts: String,
        treatmentTitle: String = "",
        stageText: String = ""
    ): List<ClinicalCareWarning> {
        val combined = "$medicalHistory $medicalAlerts".lowercase()
        if (combined.isBlank()) return emptyList()
        val surgical = isSurgical(treatmentTitle, stageText)
        val seen = mutableSetOf<String>()
        val out = mutableListOf<ClinicalCareWarning>()
        for (rule in warningRules) {
            if (rule.surgicalOnly && !surgical) continue
            if (rule.triggers.none { combined.contains(it.lowercase()) }) continue
            if (!seen.add(rule.title)) continue
            out.add(ClinicalCareWarning(rule.title, rule.detail, rule.severity, rule.source))
        }
        // شدیدترین هشدارها اول
        return out.sortedBy {
            when (it.severity) {
                FlagSeverity.CONTRAINDICATION -> 0
                FlagSeverity.CAUTION -> 1
                FlagSeverity.INFO -> 2
            }
        }
    }

    /**
     * ساخت برنامهٔ کامل مراقبت پس از درمان، بر اساس:
     * نوع درمان + فاز جلسه + سوابق پزشکی و داروهای بیمار.
     */
    fun buildPlan(
        treatmentTitle: String,
        stageText: String,
        medicalHistory: String,
        medicalAlerts: String
    ): PostOpCarePlan {
        val profile = matchProfile(treatmentTitle, stageText)
        val phase = phaseOf(stageText)
        val instructions = when {
            profile == null -> listOf(
                "بهداشت دهان را با مسواک نرم و نخ دندان ادامه دهید.",
                "داروهای تجویزشده را سر ساعت و کامل مصرف کنید.",
                "در صورت بروز درد، تورم یا هر علامت غیرمنتظره با مطب تماس بگیرید."
            )
            phase == SessionPhase.FINAL && profile.final.isNotEmpty() -> profile.final
            phase == SessionPhase.INTERIM && profile.interim.isNotEmpty() -> profile.interim
            else -> profile.operative
        }
        return PostOpCarePlan(
            procedureLabel = profile?.label ?: "توصیه‌های عمومی",
            phase = phase,
            instructions = instructions,
            redFlags = profile?.redFlags.orEmpty(),
            warnings = warningsForPatient(medicalHistory, medicalAlerts, treatmentTitle, stageText),
            sources = profile?.sources.orEmpty()
        )
    }

    /**
     * متن آمادهٔ ارسال به بیمار. فقط بخش‌های «خطاب به بیمار» را شامل می‌شود؛
     * هشدارهای بالینی عمداً وارد این متن نمی‌شوند چون مخاطبشان پزشک و پرونده است.
     */
    fun buildPatientMessage(
        plan: PostOpCarePlan,
        patientName: String,
        clinicName: String,
        treatmentTitle: String
    ): String = buildString {
        append("با سلام و احترام؛\n")
        append("مراجع عزیز $patientName\n\n")
        append("توصیه‌های مراقبت پس از «$treatmentTitle»")
        if (clinicName.isNotBlank()) append(" — $clinicName")
        append(":\n\n")
        plan.instructions.forEach { append("• $it\n") }
        if (plan.redFlags.isNotEmpty()) {
            append("\nدر صورت مشاهدهٔ موارد زیر فوراً با مطب تماس بگیرید:\n")
            plan.redFlags.forEach { append("‼ $it\n") }
        }
        append("\nبا آرزوی سلامتی شما.")
    }

    /** خلاصهٔ هشدارها برای درج در پروندهٔ بیمار (خطاب به پزشک). */
    fun buildClinicalNote(plan: PostOpCarePlan): String {
        if (plan.warnings.isEmpty()) return ""
        return buildString {
            append("⚠ هشدارهای بالینی بر اساس سوابق و داروهای بیمار:\n")
            plan.warnings.forEach { w ->
                val tag = when (w.severity) {
                    FlagSeverity.CONTRAINDICATION -> "منع/الزام"
                    FlagSeverity.CAUTION -> "احتیاط"
                    FlagSeverity.INFO -> "اطلاع"
                }
                append("• [$tag] ${w.title}: ${w.detail}\n   منبع: ${w.source}\n")
            }
            append("\nاین بررسی خودکار جایگزین قضاوت بالینی نیست.")
        }
    }
}
