-- One-shot migration for Firebase phone-OTP login.
-- Run on the dev/prod database AFTER first deploy of the phone-OTP feature.
-- Hibernate's ddl-auto=update does not reliably add UNIQUE constraints to
-- pre-existing columns or relax NOT NULL on existing columns, so apply manually.

-- 1. Allow NULL on email/password for phone-only accounts going forward.
ALTER TABLE users MODIFY email   VARCHAR(255) NULL;
ALTER TABLE users MODIFY password VARCHAR(255) NULL;

-- 2. Tighten phone column type and enforce uniqueness.
--    Skip if duplicate phone values currently exist (find and resolve them first):
--      SELECT phone, COUNT(*) FROM users WHERE phone IS NOT NULL GROUP BY phone HAVING COUNT(*) > 1;
ALTER TABLE users MODIFY phone VARCHAR(32) NULL;
ALTER TABLE users ADD UNIQUE KEY uk_users_phone (phone);

-- 3. Allow video-only banners (no desktop image required).
ALTER TABLE banners MODIFY image_url VARCHAR(500) NULL;
