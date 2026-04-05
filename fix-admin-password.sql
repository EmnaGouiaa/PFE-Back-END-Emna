-- ============================================
-- FIX ADMIN LOGIN - Password Encoding Issue
-- ============================================
-- Run this script to fix ADMIN password encoding

-- 1. Check current ADMIN users
SELECT 
    id, 
    email, 
    nom, 
    prenom, 
    role, 
    compte_valide,
    CASE 
        WHEN password LIKE '$2a$%' THEN 'BCrypt Encoded ✅'
        WHEN password LIKE '$2b$%' THEN 'BCrypt Encoded ✅'
        WHEN password LIKE '$2y$%' THEN 'BCrypt Encoded ✅'
        ELSE 'PLAIN TEXT ❌'
    END as password_status,
    LEFT(password, 30) as password_preview
FROM users 
WHERE role = 'ADMIN';

-- 2. If password is plain text, UPDATE it with BCrypt encoded password
-- Default password: admin123
-- BCrypt hash for 'admin123':
UPDATE users 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBm7FNWYB/MfFJGj1n4.V9cL6uO',
    compte_valide = true
WHERE role = 'ADMIN' 
  AND (password NOT LIKE '$2a$%' 
       AND password NOT LIKE '$2b$%' 
       AND password NOT LIKE '$2y$%');

-- 3. Alternative: Update specific admin email
-- UPDATE users 
-- SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqBm7FNWYB/MfFJGj1n4.V9cL6uO',
--     compte_valide = true
-- WHERE email = 'admin@pfe.tn';

-- 4. Verify the fix
SELECT 
    id, 
    email, 
    role, 
    compte_valide,
    CASE 
        WHEN password LIKE '$2a$%' THEN 'BCrypt Encoded ✅'
        WHEN password LIKE '$2b$%' THEN 'BCrypt Encoded ✅'
        WHEN password LIKE '$2y$%' THEN 'BCrypt Encoded ✅'
        ELSE 'PLAIN TEXT ❌'
    END as password_status
FROM users 
WHERE role = 'ADMIN';
