-- Remove legacy synthetic email domain entries created for phone-login users.
-- Safe to run multiple times.

UPDATE users
SET email = phone
WHERE email LIKE '%@phone.anushabazaar.local'
  AND phone IS NOT NULL
  AND TRIM(phone) <> '';

UPDATE auth_sessions s
JOIN users u ON u.id = s.user_id
SET s.principal_email = u.email
WHERE s.principal_email LIKE '%@phone.anushabazaar.local';
