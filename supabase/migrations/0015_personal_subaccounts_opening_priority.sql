-- 0015 — حساب اشخاص: مبلغ اولیه (افتتاحیه) + شمارهٔ اولویت نمایش
-- ستون‌ها به personal_sub_accounts اضافه می‌شوند؛ اپ (نسخهٔ Room 16،
-- MIGRATION_15_16) همین دو ستون را با همین نام‌های camelCase انتظار دارد
-- (قرارداد بقیهٔ جداول: ستون‌های camelCase و نگاشت jsonKeysToCamel در سینک).
alter table personal_sub_accounts
    add column if not exists "openingBalance" bigint not null default 0;

alter table personal_sub_accounts
    add column if not exists "priority" integer not null default 0;

-- مبلغ اولیهٔ شخص (openingBalance) فقط ماندهٔ نمایشی «حساب اشخاص» را می‌سازد؛
-- هیچ سند بانکی/دفتری خودکار نمی‌سازد، پس سیاست RLS موجود کافی است.
