-- ============================================================================
-- DentalPro — Migration 0011: دسته‌بندی فایل‌های Storage و جلوگیری از تکرار
-- ============================================================================
-- تا پیش از این هر آپلود مسیرِ «{uid}/{UUID}.jpg» می‌گرفت. دو ایراد داشت:
--
--   ۱. همهٔ فایل‌ها در یک پوشه ریخته می‌شدند — عکس چک، امضای بیمار، آواتار
--      پرسنل و لوگوی مطب همه کنار هم.
--   ۲. چون هر بار یک UUID تازه ساخته می‌شد، ذخیرهٔ دوبارهٔ *همان* فایل یک شیء
--      کاملاً جدید می‌ساخت. یک عکس واحد می‌توانست ده‌ها نسخه با شناسه‌های
--      متفاوت داشته باشد.
--
-- ساختار جدید:
--
--     {uid}/{category}/{sha256}.{ext}
--
--   • {uid}      → دسته‌بندی هر کاربر کاملاً جدا از کاربران دیگر
--   • {category} → cheques | patients | clinic | misc
--   • {sha256}   → هشِ محتوای فایل؛ محتوای یکسان همیشه همان مسیر را می‌دهد،
--                  پس آپلود دوم با x-upsert روی همان شیء می‌نویسد و نسخهٔ
--                  تکراری ساخته نمی‌شود.
--
-- سه پوشهٔ اصلی طبق درخواست:
--   cheques   → تصاویر چک
--   patients  → امضا و پروفایل و مدارک بیماران
--   clinic    → پروفایل پرسنل و پزشک، آواتار، و لوگوی مطب
-- ============================================================================

-- سیاست درج: علاوه بر owner، اولین بخشِ مسیر هم باید دقیقاً auth.uid() باشد.
-- این تضمین می‌کند فایل‌های هر کاربر فقط داخل پوشهٔ خودش بنشیند و هیچ کاربری
-- نتواند در پوشهٔ کاربر دیگر بنویسد.
drop policy if exists "dental_media_insert" on storage.objects;
create policy "dental_media_insert" on storage.objects
    for insert to authenticated
    with check (
        bucket_id in ('patient_media', 'clinic_assets')
        and owner = auth.uid()
        and (storage.foldername(name))[1] = auth.uid()::text
    );

-- سیاست به‌روزرسانی: x-upsert برای بازنویسیِ همان شیء به این نیاز دارد.
drop policy if exists "dental_media_update" on storage.objects;
create policy "dental_media_update" on storage.objects
    for update to authenticated
    using (
        bucket_id in ('patient_media', 'clinic_assets')
        and owner = auth.uid()
    )
    with check (
        bucket_id in ('patient_media', 'clinic_assets')
        and owner = auth.uid()
        and (storage.foldername(name))[1] = auth.uid()::text
    );

-- ---------------------------------------------------------------------------
-- گزارش فایل‌های تکراری (برای پاک‌سازی دستیِ داده‌های قدیمی)
-- ---------------------------------------------------------------------------
-- فایل‌هایی که پیش از این مهاجرت آپلود شده‌اند مسیر قدیمی دارند و ممکن است
-- چند نسخه از یک تصویر باشند. این view آن‌ها را بر اساس اندازه و مالک
-- گروه‌بندی می‌کند تا بشود بازبینی و حذفشان کرد. چیزی را خودکار حذف نمی‌کند.
create or replace view public.storage_duplicate_candidates as
select
    o.bucket_id,
    o.owner,
    (o.metadata ->> 'size')::bigint as size_bytes,
    count(*)                        as copies,
    array_agg(o.name order by o.created_at) as object_paths
from storage.objects o
where o.bucket_id in ('patient_media', 'clinic_assets')
  and o.metadata ? 'size'
group by o.bucket_id, o.owner, (o.metadata ->> 'size')::bigint
having count(*) > 1;

comment on view public.storage_duplicate_candidates is
    'نسخه‌های احتمالاً تکراری در Storage (هم‌مالک و هم‌اندازه) — برای بازبینی دستی. فایل‌های تازه به‌خاطر نام‌گذاری محتوامحور اصلاً تکراری نمی‌شوند.';
