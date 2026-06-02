-- Signatures de profil manquantes (comptes demo) — corrige HTTP 400 sur PUT /api/conventions-stage/{id}/signer-*
-- Executer sur bdPFE puis reessayer la signature.

UPDATE utilisateur
SET url_signature = 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='
WHERE (url_signature IS NULL OR TRIM(url_signature) = '')
  AND (
    LOWER(email) LIKE '%@etudiant.tn'
    OR LOWER(email) LIKE '%@fsegs.tn'
    OR LOWER(email) LIKE '%@telnet.tn'
    OR LOWER(email) LIKE '%@sofrecom.tn'
    OR LOWER(email) LIKE '%@techcorp.tn'
    OR LOWER(email) LIKE '%demo.%'
  );

SELECT id, email, role, LENGTH(url_signature) AS signature_len
FROM utilisateur
WHERE LOWER(email) LIKE '%demo.fin.stagiaire%';
