package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.BankAccount
import com.example.reports.AppFonts
import com.example.util.IranianBankHelper
import com.example.util.IranianBankInfo

/**
 * متن با «افکت برجسته» (Emboss) مثل حکاکیِ روی کارت واقعی: یک سایهٔ تیره کمی
 * پایین-راست و یک های‌لایتِ روشن کمی بالا-چپِ متن اصلی رسم می‌شود، پس حروف
 * انگار از سطح کارت بیرون زده‌اند.
 */
@Composable
private fun EmbossedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    fontSize: androidx.compose.ui.unit.TextUnit = 16.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    fontFamily: FontFamily? = null,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    textAlign: TextAlign = TextAlign.Unspecified,
    maxLines: Int = 1
) {
    Box(modifier = modifier) {
        val base = TextStyle(
            fontSize = fontSize,
            fontWeight = fontWeight,
            fontFamily = fontFamily,
            letterSpacing = letterSpacing,
            textAlign = textAlign
        )
        // سایهٔ تیره (پایین-راست)
        Text(
            text = text,
            style = base.copy(color = Color.Black.copy(alpha = 0.55f)),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .matchParentSize()
                .offset(x = 1.2.dp, y = 1.2.dp)
        )
        // های‌لایت روشن (بالا-چپ)
        Text(
            text = text,
            style = base.copy(color = Color.White.copy(alpha = 0.45f)),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .matchParentSize()
                .offset(x = (-1).dp, y = (-1).dp)
        )
        // متن اصلی
        Text(
            text = text,
            style = base.copy(color = color),
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * کارت بانکی ایرانی با ظاهر و ابعاد کارت واقعی (نسبت استاندارد ISO/IEC 7810
 * ID-1 ≈ 1.585:1) و افکت برجسته روی شماره کارت، نام دارنده، لوگو و نشان NFC.
 *
 * تغییرات مهم نسبت به نسخهٔ قبل (طبق درخواست کاربر):
 *  • دکمه‌های «ادیت» و «حذف» از روی کارت حذف شدند.
 *  • ادیت / جابه‌جایی / مخفی‌کردنِ کارت با **مکث طولانی روی لوگوی بانک** انجام
 *    می‌شود (یک منوی بی‌سروصدا باز می‌شود؛ هیچ اعلان/توستی نمایش داده نمی‌شود).
 *  • «ارسال عکس»، «ارسال متن» و «شماره شبا» به **زیرِ کارت** منتقل شدند تا خودِ
 *    کارت دقیقاً مثل کارت واقعی و بدون شلوغی بماند.
 *
 * @param onEdit اگر داده شود، گزینهٔ «ویرایش» در منوی مکث طولانی فعال است.
 * @param onMoveUp / [onMoveDown] جابه‌جایی کارت در لیست (مثل کارت‌های واقعی).
 * @param onToggleHide مخفی/نمایشِ کارت.
 */
@Composable
fun IranianBankCard(
    account: BankAccount,
    modifier: Modifier = Modifier,
    clinicName: String = "",
    onEdit: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    onToggleHide: (() -> Unit)? = null,
    showActions: Boolean = true,
    onShowTransactions: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val detectedBank = remember(account.cardNumber, account.iban) {
        IranianBankHelper.detectBank(account.cardNumber) ?: IranianBankHelper.detectBankFromIban(account.iban)
    }

    val gradientBrush = remember(detectedBank) {
        if (detectedBank != null) {
            Brush.linearGradient(
                listOf(
                    detectedBank.primaryColor,
                    detectedBank.secondaryColor,
                    detectedBank.primaryColor.copy(alpha = 0.9f)
                )
            )
        } else {
            Brush.linearGradient(
                listOf(Color(0xFF1E3C72), Color(0xFF2A5298), Color(0xFF1E3C72))
            )
        }
    }

    val formattedCardNumber = remember(account.cardNumber) {
        IranianBankHelper.formatCardNumber(account.cardNumber).ifBlank {
            account.cardNumber.ifBlank { "•••• - •••• - •••• - ••••" }
        }
    }

    val displayBankName = remember(detectedBank, account.bankName) {
        if (account.bankName.isNotBlank()) account.bankName
        else detectedBank?.nameFa ?: "کارت بانکی"
    }
    val displayBankEn = remember(detectedBank) { detectedBank?.nameEn ?: "Debit / Credit Card" }
    val accentGold = detectedBank?.accentColor ?: Color(0xFFFFD700)

    val logoResId = remember(detectedBank) {
        if (!detectedBank?.logoResName.isNullOrEmpty()) {
            context.resources.getIdentifier(detectedBank?.logoResName, "drawable", context.packageName)
        } else 0
    }

    var logoMenuExpanded by remember { mutableStateOf(false) }
    val hasLongPressMenu = onEdit != null || onMoveUp != null || onMoveDown != null || onToggleHide != null

    // ستونِ کلی: خودِ کارت + (زیرِ کارت) شبا و دکمه‌های اشتراک‌گذاری
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // ——— خودِ کارت با ابعاد واقعی ———
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.585f) // نسبت استاندارد کارت بانکی واقعی
                .shadow(10.dp, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(gradientBrush)
                .padding(18.dp)
        ) {
            // دایره‌های تزئینی پس‌زمینه
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 40.dp, y = (-40).dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f))
            )
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-30).dp, y = 30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f))
            )

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ——— سطر بالا: لوگوی بانک (با مکث طولانی) + نام بانک ... نشان NFC ———
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box {
                            // لوگوی بانک با افکت برجسته (سایهٔ زیرین) + میانبر مکث طولانی
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .shadow(4.dp, RoundedCornerShape(10.dp))
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.92f))
                                    .border(1.dp, accentGold.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                                    .then(
                                        if (hasLongPressMenu)
                                            Modifier.pointerInput(Unit) {
                                                detectTapGestures(
                                                    onLongPress = { logoMenuExpanded = true }
                                                )
                                            }
                                        else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (logoResId != 0) {
                                    Image(
                                        painter = painterResource(id = logoResId),
                                        contentDescription = displayBankName,
                                        modifier = Modifier.size(32.dp)
                                    )
                                } else if (!detectedBank?.shortCode.isNullOrEmpty()) {
                                    Text(
                                        text = detectedBank?.shortCode ?: "",
                                        color = detectedBank?.primaryColor ?: Color(0xFF1E3C72),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = AppFonts.titleFamily(),
                                        textAlign = TextAlign.Center
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AccountBalance,
                                        contentDescription = null,
                                        tint = detectedBank?.primaryColor ?: Color(0xFF1E3C72),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            // منوی بی‌سروصدای مکث طولانی روی لوگو
                            DropdownMenu(
                                expanded = logoMenuExpanded,
                                onDismissRequest = { logoMenuExpanded = false }
                            ) {
                                if (onEdit != null) {
                                    DropdownMenuItem(
                                        text = { Text("ویرایش کارت") },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                        onClick = { logoMenuExpanded = false; onEdit() }
                                    )
                                }
                                if (onMoveUp != null) {
                                    DropdownMenuItem(
                                        text = { Text("انتقال به بالا") },
                                        leadingIcon = { Icon(Icons.Default.ArrowUpward, contentDescription = null) },
                                        onClick = { logoMenuExpanded = false; onMoveUp() }
                                    )
                                }
                                if (onMoveDown != null) {
                                    DropdownMenuItem(
                                        text = { Text("انتقال به پایین") },
                                        leadingIcon = { Icon(Icons.Default.ArrowDownward, contentDescription = null) },
                                        onClick = { logoMenuExpanded = false; onMoveDown() }
                                    )
                                }
                                if (onToggleHide != null) {
                                    DropdownMenuItem(
                                        text = { Text(if (account.isHidden) "نمایش کارت" else "مخفی‌کردن کارت") },
                                        leadingIcon = {
                                            Icon(
                                                if (account.isHidden) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                                contentDescription = null
                                            )
                                        },
                                        onClick = { logoMenuExpanded = false; onToggleHide() }
                                    )
                                }
                            }
                        }

                        Column {
                            EmbossedText(
                                text = displayBankName,
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = AppFonts.titleFamily()
                            )
                            Text(
                                text = displayBankEn,
                                fontSize = 10.sp,
                                color = accentGold,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = AppFonts.numberFamily()
                            )
                        }
                    }

                    // نشان NFC (پرداخت بدون تماس) با افکت برجسته
                    Box {
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = "NFC",
                            tint = Color.Black.copy(alpha = 0.4f),
                            modifier = Modifier.size(26.dp).offset(x = 1.dp, y = 1.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Nfc,
                            contentDescription = null,
                            tint = accentGold,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

                // ——— سطر میانی: تراشهٔ EMV ———
                Box(
                    modifier = Modifier
                        .size(width = 46.dp, height = 34.dp)
                        .shadow(3.dp, RoundedCornerShape(6.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFFFFD54F), Color(0xFFFFB300), Color(0xFFFFD54F))
                            )
                        )
                        .border(1.dp, Color(0xFFC67D0A), RoundedCornerShape(6.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(1.dp)
                            .align(Alignment.Center)
                            .background(Color(0xFF8D6E63))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .align(Alignment.Center)
                            .background(Color(0xFF8D6E63))
                    )
                }

                // ——— شمارهٔ ۱۶ رقمی کارت با افکت برجسته (LTR، لمس برای کپی) ———
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val clean = IranianBankHelper.cleanDigits(account.cardNumber)
                                if (clean.isNotEmpty()) copyToClipboard(context, clean, "شماره کارت کپی شد")
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        EmbossedText(
                            text = formattedCardNumber,
                            color = Color.White,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = AppFonts.numberFamily(),
                            letterSpacing = 2.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // ——— سطر پایین: نام دارنده (برجسته) + تاریخ انقضا ———
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "دارنده کارت",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        EmbossedText(
                            text = account.cardHolder.ifBlank { "نامشخص" },
                            color = accentGold,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = AppFonts.titleFamily()
                        )
                    }

                    val expText = remember(account.expireMonth, account.expireYear) {
                        when {
                            account.expireMonth.isNotBlank() && account.expireYear.isNotBlank() ->
                                "${account.expireMonth.padStart(2, '0')} / ${account.expireYear.takeLast(2)}"
                            account.expireMonth.isNotBlank() || account.expireYear.isNotBlank() ->
                                "${account.expireMonth} / ${account.expireYear}"
                            else -> ""
                        }
                    }
                    if (expText.isNotBlank()) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(text = "انقضا", fontSize = 9.sp, color = Color.White.copy(alpha = 0.7f))
                            Text(
                                text = expText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = AppFonts.numberFamily(),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // ——— زیرِ کارت: گردش حساب + شماره حساب ———
        if (onShowTransactions != null || account.accountNumber.isNotBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onShowTransactions != null) {
                    OutlinedButton(
                        onClick = onShowTransactions,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("گردش حساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                if (account.accountNumber.isNotBlank()) {
                    Text(
                        text = "حساب: ${account.accountNumber}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = AppFonts.textFamily(),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // ——— زیرِ کارت: شماره شبا (لمس برای کپی) ———
        if (account.iban.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        copyToClipboard(context, account.iban.trim().uppercase(), "شماره شبا کپی شد")
                    }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "شبا: ${account.iban.trim().uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = AppFonts.textFamily(),
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "Copy IBAN",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // ——— زیرِ کارت: دکمه‌های «ارسال متن» و «ارسال عکس کارت» ———
        if (showActions) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { shareCardText(context, account, displayBankName) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ارسال متن", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { shareCardImage(context, account, detectedBank, displayBankName, clinicName) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("ارسال عکس کارت", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

private fun shareCardText(context: Context, account: BankAccount, displayBankName: String) {
    val cleanCard = IranianBankHelper.cleanDigits(account.cardNumber)
    val shareText = buildString {
        append("💳 اطلاعات کارت بانکی ${displayBankName}\n\n")
        append("شماره کارت:\n")
        if (cleanCard.isNotBlank()) {
            append(IranianBankHelper.formatCardNumber(cleanCard)); append("\n")
        }
        append("نام دارنده:\n")
        if (account.cardHolder.isNotBlank()) append("${account.cardHolder}\n")
        append("بانک صادرکننده:\n")
        append(displayBankName)
        if (account.accountNumber.isNotBlank()) append("\nشماره حساب:\n${account.accountNumber}")
        if (account.iban.isNotBlank()) append("\nشماره شبا:\n${account.iban.trim().uppercase()}")
    }
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareText)
        type = "text/plain"
    }
    context.startActivity(Intent.createChooser(sendIntent, "ارسال اطلاعات حساب بانکی"))
}

private fun shareCardImage(
    context: Context,
    account: BankAccount,
    detectedBank: IranianBankInfo?,
    displayBankName: String,
    clinicName: String
) {
    try {
        val bitmap = IranianBankHelper.generateCardImageBitmap(
            context = context,
            bankInfo = detectedBank,
            cardNumber = account.cardNumber,
            cardHolder = account.cardHolder,
            cvv2 = account.cvv2,
            expireMonth = account.expireMonth,
            expireYear = account.expireYear,
            accountNumber = account.accountNumber,
            iban = account.iban,
            clinicTitle = clinicName
        )
        val imageFile = IranianBankHelper.saveCardImageToFile(context, bitmap, "card_${account.id}.png")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "💳 کارت بانکی ${displayBankName} - ${account.cardHolder}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "اشتراک‌گذاری تصویر کارت بانکی"))
    } catch (e: Exception) {
        Toast.makeText(context, "خطا در تولید عکس کارت: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
    }
}

private fun copyToClipboard(context: Context, text: String, message: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Bank Info", text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
