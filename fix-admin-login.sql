-- Fix ADMIN login issues
-- Run this SQL script to ensure all ADMIN users have compte_valide = true

-- Check current state of ADMIN users
SELECT id, email, nom, prenom, role, compte_valide 
FROM users 
WHERE role = 'ADMIN';

-- Fix: Set compte_valide = true for all ADMIN users (if null or false)
UPDATE users 
SET compte_valide = true 
WHERE role = 'ADMIN' 
  AND (compte_valide = false OR compte_valide IS NULL);

-- Alternative: If you have a specific admin email, use this:
-- UPDATE users 
-- SET compte_valide = true 
-- WHERE email = 'admin@yourdomain.com';

-- Verify the fix
SELECT id, email, nom, prenom, role, compte_valide 
FROM users 
WHERE role = 'ADMIN';
