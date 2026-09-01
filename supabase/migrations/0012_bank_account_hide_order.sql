-- ============================================================================
-- DentalPro — Migration 0012: مخفی‌سازی و ترتیبِ کارت‌های بانکی
-- ============================================================================
-- برای «مخفی‌کردن کارت» و «جابه‌جایی کارت مثل کارت‌های واقعی» در صفحهٔ مدیریت
-- حساب‌ها (میانبر مکث طولانی روی لوگوی بانک)، دو ستون به جدول bank_accounts
-- اضافه می‌شود. بدون این ستون‌ها، سینکِ حساب بانکی با خطای PGRST204 («ستون
-- is_hidden یافت نشد») شکست می‌خورد، چون EntityMapper این فیلدها را هم به
-- PostgREST می‌فرستد.
-- ============================================================================

alter table public.bank_accounts
    add column if not exists is_hidden  boolean not null default false,
    add column if not exists sort_order integer not null default 0;

comment on column public.bank_accounts.is_hidden is
    'کارت در صفحهٔ مدیریت حساب‌ها مخفی است (میانبر مکث طولانی روی لوگو)';
comment on column public.bank_accounts.sort_order is
    'ترتیب نمایشِ کارت برای جابه‌جایی دستی (کوچک‌تر بالاتر)';
