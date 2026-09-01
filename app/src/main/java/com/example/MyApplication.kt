package com.example

import android.app.Application
import android.util.Log
import com.example.util.Supabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyApplication : Application() {

    // اسکوپِ کاربردیِ سطح‌اپ برای کارهای گرم‌کردنِ استارت — روی Dispatchers.IO
    // اجرا می‌شود تا Thread اصلی هنگام باز شدن اپ آزاد بماند.
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // فقط راه‌اندازیِ سبک روی Thread اصلی؛ بقیه به پس‌زمینه می‌رود.
        Supabase.init(applicationContext)

        appScope.launch {
            // ۱) گرم‌کردنِ دیتابیس رمزنگاری‌شده (SQLCipher) خارج از Thread اصلی.
            //    ساختِ سنگینِ کلید/مهاجرت اینجا انجام می‌شود تا وقتی ViewModel
            //    به دیتابیس نیاز دارد، سینگلتون آماده باشد و روی UI مکث نیفتد.
            try {
                com.example.data.AppDatabase.getDatabase(applicationContext)
            } catch (e: Throwable) {
                Log.w("MyApplication", "DB warm-up skipped: ${e.message}")
            }

            // ۲) گرم‌کردنِ فونت‌های اپ (کپی از assets به کش) خارج از Thread اصلی،
            //    تا اولین ترکیب‌بندیِ Compose منتظرِ استخراج فونت نماند.
            try {
                com.example.reports.AppFonts.fontSet(applicationContext)
            } catch (e: Throwable) {
                Log.w("MyApplication", "Font warm-up skipped: ${e.message}")
            }

            // ۳) بررسی اتصال Supabase (اختیاری، فقط برای لاگ).
            try {
                val pingResult = Supabase.ping()
                Log.d("SupabasePing", "Supabase ping result: $pingResult")
                val acc = Supabase.currentUser()
                Log.d("SupabasePing", "Session active for: ${acc.email.ifBlank { acc.id }}")
            } catch (e: Exception) {
                Log.d("SupabasePing", "Supabase connection checked: ${e.message}")
            }
        }
    }
}
