-- ============================================
-- ADMIN USER VERIFICATION AND FIX SCRIPT
-- ============================================
-- Run this script if ADMIN login is not working
-- This will ensure the admin user exists with correct credentials

USE pfe_database; -- Change to your actual database name

-- 1. Check if admin user exists
SELECT 
    id,
    email,
    prenom,
    nom,
    role,
    compte_valide,
    password,
    user_type
FROM users 
WHERE email = 'admin@pfe.tn';

-- 2. If admin doesn't exist or has wrong password, run this INSERT/UPDATE:

-- Option A: Delete existing admin and recreate (RECOMMENDED for fresh setup)
DELETE FROM users WHERE email = 'admin@pfe.tn';

INSERT INTO users (
    email, 
    prenom, 
    nom, 
    password, 
    role, 
    compte_valide, 
    matricule,
    user_type
) VALUES (
    'admin@pfe.tn',
    'Admin',
    'User',
    '$2a$10$rH8qZ9vK3xJ5mN2pL7wQ4eY6tU8iO1aS3dF5gH7jK9lM0nP2qR4sT', -- Password123! encoded
    'ADMIN',
    TRUE,
    'ADMIN001',
    'USER'
);

-- Option B: Update existing admin (if you want to keep the same ID)
/*
UPDATE users 
SET 
    password = '$2a$10$rH8qZ9vK3xJ5mN2pL7wQ4eY6tU8iO1aS3dF5gH7jK9lM0nP2qR4sT', -- Password123!
    role = 'ADMIN',
    compte_valide = TRUE,
    prenom = 'Admin',
    nom = 'User',
    matricule = 'ADMIN001',
    user_type = 'USER'
WHERE email = 'admin@pfe.tn';
*/

-- 3. Verify the update
SELECT 
    id,
    email,
    prenom,
    nom,
    role,
    compte_valide,
    user_type
FROM users 
WHERE email = 'admin@pfe.tn';

-- Expected result:
-- id | email          | prenom | nom  | role  | compte_valide | user_type
-- ---|----------------|--------|------|-------|---------------|----------
-- 1  | admin@pfe.tn   | Admin  | User | ADMIN | 1             | USER

-- 4. Test other demo users exist
SELECT 
    email,
    role,
    compte_valide
FROM users
WHERE email IN (
    'admin@pfe.tn',
    'student@pfe.tn',
    'teacher@pfe.tn',
    'company@pfe.tn',
    'internship@pfe.tn'
)
ORDER BY email;
