package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.reports.AppFonts

// Set of Material typography styles to start with
val Typography =
  Typography(
    bodyLarge =
      TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp,
      )
    /* Other default text styles to override
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
    */
  )

/**
 * تایپوگرافی واقعی برنامه: همان TTF های استاندارد پروژه که کارت‌ها، اسناد،
 * PDF و تصویر کارت بانکی با آن‌ها رندر می‌شوند، روی UI هم اعمال می‌شوند.
 * اگر فونتی در `assets/fonts` نباشد، همان [Typography] پیش‌فرض برگردانده
 * می‌شود (رفتار قبلی بدون تغییر).
 *
 * این، نیمهٔ «اپلیکیشن» از یکپارچه‌سازی است: تا پیش از این UI با فونت سیستم
 * و خروجی‌ها با فونت پیش‌فرض دستگاه رندر می‌شدند و ظاهرشان هیچ‌وقت با هم
 * یکی نبود.
 */
@Composable
fun appTypography(): Typography {
  val context = LocalContext.current
  val base = Typography
  if (!AppFonts.hasCustomFont(context)) return base
  val text = AppFonts.textFamily()
  val title = AppFonts.titleFamily()
  return base.copy(
    displayLarge = base.displayLarge.copy(fontFamily = title, fontWeight = FontWeight.Bold),
    displayMedium = base.displayMedium.copy(fontFamily = title),
    displaySmall = base.displaySmall.copy(fontFamily = title),
    headlineLarge = base.headlineLarge.copy(fontFamily = title, fontWeight = FontWeight.Bold),
    headlineMedium = base.headlineMedium.copy(fontFamily = title),
    headlineSmall = base.headlineSmall.copy(fontFamily = title),
    titleLarge = base.titleLarge.copy(fontFamily = title),
    titleMedium = base.titleMedium.copy(fontFamily = title),
    titleSmall = base.titleSmall.copy(fontFamily = title),
    bodyLarge = base.bodyLarge.copy(fontFamily = text),
    bodyMedium = base.bodyMedium.copy(fontFamily = text),
    bodySmall = base.bodySmall.copy(fontFamily = text),
    labelLarge = base.labelLarge.copy(fontFamily = title),
    labelMedium = base.labelMedium.copy(fontFamily = text),
    labelSmall = base.labelSmall.copy(fontFamily = text)
  )
}

/**
 * تایپوگرافیِ اختصاصیِ تمِ «زمردِ سلطنتی» — سایز و وزنِ هر سطحِ متن جداگانه و
 * سنجیده تنظیم شده تا این تم حس‌وحالِ رسمی‌تر و خواناتری داشته باشد (عنوان‌های
 * درشت‌تر و بولد، بدنهٔ کمی بزرگ‌تر برای خوانایی، لیبل‌های فشرده‌تر). این تابع
 * فقط وقتی صدا زده می‌شود که تمِ ROYAL_EMERALD انتخاب شده باشد؛ سایرِ تم‌ها
 * دست‌نخورده می‌مانند. اگر فونتِ اختصاصیِ برنامه موجود نباشد، همان تنظیمِ سایز/وزن
 * روی فونتِ پیش‌فرض اعمال می‌شود.
 */
@Composable
fun royalEmeraldTypography(): Typography {
  val context = LocalContext.current
  val hasFont = AppFonts.hasCustomFont(context)
  val title = if (hasFont) AppFonts.titleFamily() else FontFamily.Default
  val text = if (hasFont) AppFonts.textFamily() else FontFamily.Default
  val base = Typography
  return base.copy(
    displayLarge  = base.displayLarge.copy(fontFamily = title, fontWeight = FontWeight.Black,    fontSize = 40.sp, lineHeight = 48.sp),
    displayMedium = base.displayMedium.copy(fontFamily = title, fontWeight = FontWeight.Bold,     fontSize = 32.sp, lineHeight = 40.sp),
    displaySmall  = base.displaySmall.copy(fontFamily = title, fontWeight = FontWeight.Bold,     fontSize = 26.sp, lineHeight = 34.sp),
    headlineLarge = base.headlineLarge.copy(fontFamily = title, fontWeight = FontWeight.Bold,     fontSize = 24.sp, lineHeight = 32.sp),
    headlineMedium= base.headlineMedium.copy(fontFamily = title, fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 28.sp),
    headlineSmall = base.headlineSmall.copy(fontFamily = title, fontWeight = FontWeight.SemiBold, fontSize = 19.sp, lineHeight = 26.sp),
    titleLarge    = base.titleLarge.copy(fontFamily = title, fontWeight = FontWeight.Bold,     fontSize = 18.sp, lineHeight = 24.sp),
    titleMedium   = base.titleMedium.copy(fontFamily = title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleSmall    = base.titleSmall.copy(fontFamily = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge     = base.bodyLarge.copy(fontFamily = text,  fontWeight = FontWeight.Normal,   fontSize = 16.sp, lineHeight = 25.sp, letterSpacing = 0.25.sp),
    bodyMedium    = base.bodyMedium.copy(fontFamily = text,  fontWeight = FontWeight.Normal,   fontSize = 14.sp, lineHeight = 21.sp, letterSpacing = 0.2.sp),
    bodySmall     = base.bodySmall.copy(fontFamily = text,  fontWeight = FontWeight.Normal,   fontSize = 12.5.sp, lineHeight = 18.sp),
    labelLarge    = base.labelLarge.copy(fontFamily = title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.3.sp),
    labelMedium   = base.labelMedium.copy(fontFamily = text,  fontWeight = FontWeight.Medium,   fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelSmall    = base.labelSmall.copy(fontFamily = text,  fontWeight = FontWeight.Medium,   fontSize = 11.sp, lineHeight = 15.sp, letterSpacing = 0.4.sp)
  )
}
