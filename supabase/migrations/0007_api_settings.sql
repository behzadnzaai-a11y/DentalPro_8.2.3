-- ============================================================================
-- DentalPro — Migration 0007: نگهداری تنظیمات و کلیدهای API روی سرور (api_settings)
-- ============================================================================
-- طبق درخواست: «api ها در سرور ذخیره و فراخوانی بشن».
--
-- تا پیش از این، کلیدهای Gemini/Groq فقط روی خودِ گوشی (SecureSecretStore) ذخیره
-- می‌شدند؛ در نتیجه با تعویض دستگاه یا نصب مجدد، کاربر باید همه را دوباره وارد
-- می‌کرد و بین دستگاه‌ها هم هم‌سان نبودند. حالا هر کاربر یک ردیف در این جدول دارد
-- و اپ هنگام اجرا آن را از سرور می‌خواند و پس از هر ذخیره، روی سرور می‌نویسد.
--
-- امنیت:
--  * RLS فعال است و هر کاربر فقط ردیف خودش را می‌بیند (owner_id = auth.uid()).
--    هیچ کاربری کلید کاربر دیگر را نمی‌تواند بخواند — حتی با anon key.
--  * کلیدها به‌صورت متن ذخیره می‌شوند ولی فقط از پشت RLS و روی TLS قابل دسترسی‌اند.
--    نسخهٔ رمزنگاری‌شده روی دستگاه (SecureSecretStore) هم به‌عنوان کش آفلاین باقی
--    می‌ماند تا اپ بدون اینترنت هم کار کند.
--  * service_role هرگز داخل اپ نیست.
-- ============================================================================

-- ---------------------------------------------------------------------------
-- ۱) جدول
-- ---------------------------------------------------------------------------
create table if not exists public.api_settings (
    id                bigint generated always as identity primary key,
    sync_id           text    not null unique,            -- شناسهٔ یکتای ردیف (هم‌راستا با بقیهٔ جدول‌ها)
    owner_id          text    not null default '',        -- auth.uid() صاحب ردیف
    gemini_api_key    text    not null default '',        -- می‌تواند چند کلید جداشده با کاما باشد
    gemini_model      text    not null default '',
    groq_api_key      text    not null default '',
    active_provider   text    not null default 'gemini',  -- 'gemini' یا 'groq'
    updated_at        timestamptz not null default now(),
    created_at        timestamptz not null default now()
);

-- هر کاربر فقط یک ردیف تنظیمات دارد.
create unique index if not exists idx_api_settings_owner
    on public.api_settings (owner_id);

-- ---------------------------------------------------------------------------
-- ۲) امنیت سطری — فقط صاحب ردیف
-- ---------------------------------------------------------------------------
alter table public.api_settings enable row level security;

drop policy if exists "api_settings_owner_rows" on public.api_settings;
create policy "api_settings_owner_rows" on public.api_settings
    for all to authenticated
    using (owner_id = auth.uid()::text)
    with check (owner_id = auth.uid()::text);

-- ---------------------------------------------------------------------------
-- ۳) به‌روزرسانی خودکار updated_at
-- ---------------------------------------------------------------------------
create or replace function public.touch_api_settings_updated_at()
returns trigger
language plpgsql
as $$
begin
    new.updated_at = now();
    return new;
end;
$$;

drop trigger if exists trg_api_settings_touch on public.api_settings;
create trigger trg_api_settings_touch
    before update on public.api_settings
    for each row execute function public.touch_api_settings_updated_at();
