package com.example.ai

import android.content.Context
import android.util.Log
import com.example.security.SecureSecretStore
import com.example.util.Supabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * ذخیره و فراخوانی کلیدهای API از روی **سرور** (جدول `api_settings` در Supabase).
 *
 * طبق درخواست «api ها در سرور ذخیره و فراخوانی بشن»:
 *  * [pullFromServer] هنگام اجرای اپ (و پس از ورود به حساب) تنظیمات را از سرور
 *    می‌خواند و در [SecureSecretStore] به‌عنوان **کش آفلاین** می‌نشاند، تا اپ بدون
 *    اینترنت هم دقیقاً همان کلیدها را در اختیار داشته باشد.
 *  * [pushToServer] پس از هر ذخیره در صفحهٔ تنظیمات، مقادیر را روی سرور می‌نویسد.
 *
 * چرا کش محلی هم نگه می‌داریم: استخراج چک ممکن است در لحظه‌ای اجرا شود که شبکه
 * قطع است؛ بدون کش، قابلیت به‌کل از کار می‌افتاد. منبع حقیقت همیشه سرور است و هر
 * pull موفق، کش را بازنویسی می‌کند.
 *
 * امنیت: جدول با RLS محافظت می‌شود (`owner_id = auth.uid()`)، پس هر کاربر فقط
 * ردیف خودش را می‌بیند. کش روی دستگاه با AndroidKeyStore رمزنگاری می‌شود.
 */
object ServerApiSettings {

    private const val TAG = "ServerApiSettings"
    private val TABLE = Supabase.TABLE_API_SETTINGS

    /** آخرین باری که با موفقیت از سرور خوانده شد (برای نمایش در UI). */
    @Volatile
    var lastSyncedAt: Long? = null
        private set

    /** آخرین خطای همگام‌سازی — اگر null باشد یعنی آخرین تلاش موفق بود. */
    @Volatile
    var lastError: String? = null
        private set

    /** شناسهٔ ردیفِ این کاربر؛ چون هر کاربر یک ردیف دارد، از خود ownerId می‌سازیمش. */
    private suspend fun syncIdForCurrentUser(): String? {
        val uid = Supabase.currentUserIdOrNull() ?: return null
        return "api-settings-$uid"
    }

    /**
     * خواندن تنظیمات از سرور و نشاندن آن در کش محلی.
     *
     * @return true اگر ردیفی از سرور خوانده و اعمال شد.
     */
    suspend fun pullFromServer(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val syncId = syncIdForCurrentUser() ?: run {
                lastError = "کاربر وارد حساب نشده است"
                return@withContext false
            }
            val row = Supabase.fetchRow(TABLE, syncId)
            if (row == null) {
                // هنوز چیزی روی سرور نیست — اگر کاربر از قبل کلید محلی دارد،
                // همان را بالا می‌فرستیم تا سرور منبع حقیقت شود.
                lastError = null
                pushToServer(context)
                return@withContext false
            }

            fun str(name: String): String =
                row.get(name)?.takeIf { !it.isJsonNull }?.asString.orEmpty()

            val gemini = str("gemini_api_key")
            val groq = str("groq_api_key")
            val model = str("gemini_model")
            val provider = str("active_provider")

            // کش محلی را با مقادیر سرور بازنویسی کن (سرور منبع حقیقت است).
            if (gemini.isNotBlank()) {
                SecureSecretStore.put(context, AiProviderSettings.KEY_GEMINI_API_KEY, gemini)
            } else {
                SecureSecretStore.remove(context, AiProviderSettings.KEY_GEMINI_API_KEY)
            }
            if (groq.isNotBlank()) {
                SecureSecretStore.put(context, AiProviderSettings.KEY_GROQ_API_KEY, groq)
            } else {
                SecureSecretStore.remove(context, AiProviderSettings.KEY_GROQ_API_KEY)
            }
            if (model.isNotBlank() && model in AiProviderSettings.GEMINI_MODELS) {
                SecureSecretStore.put(context, AiProviderSettings.KEY_GEMINI_MODEL, model)
            }
            if (provider == "groq" || provider == "gemini") {
                SecureSecretStore.put(context, AiProviderSettings.KEY_ACTIVE_PROVIDER, provider)
            }

            lastSyncedAt = System.currentTimeMillis()
            lastError = null
            Log.i(TAG, "API settings pulled from server")
            true
        } catch (e: Exception) {
            lastError = e.message ?: "خطای نامشخص"
            Log.w(TAG, "pullFromServer failed: ${e.message}")
            false
        }
    }

    /**
     * نوشتن تنظیمات فعلی روی سرور. پس از هر «ذخیره» در صفحهٔ تنظیمات صدا زده می‌شود.
     *
     * @return true اگر با موفقیت روی سرور نوشته شد.
     */
    suspend fun pushToServer(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val syncId = syncIdForCurrentUser() ?: run {
                lastError = "کاربر وارد حساب نشده است"
                return@withContext false
            }
            val payload = mapOf<String, Any>(
                "geminiApiKey" to (AiProviderSettings.getGeminiApiKey(context) ?: ""),
                "geminiModel" to AiProviderSettings.getGeminiModel(context),
                "groqApiKey" to (AiProviderSettings.getGroqApiKey(context) ?: ""),
                "activeProvider" to when (AiProviderSettings.getActiveProvider(context)) {
                    AiProviderSettings.Provider.GROQ -> "groq"
                    AiProviderSettings.Provider.GEMINI -> "gemini"
                }
            )
            Supabase.upsertRow(TABLE, syncId, payload)
            lastSyncedAt = System.currentTimeMillis()
            lastError = null
            Log.i(TAG, "API settings pushed to server")
            true
        } catch (e: Exception) {
            lastError = e.message ?: "خطای نامشخص"
            Log.w(TAG, "pushToServer failed: ${e.message}")
            false
        }
    }
}
