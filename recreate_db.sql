-- =============================================================================
-- recreate_db.sql
-- Run this script BEFORE starting the Spring Boot app with ddl-auto=create.
-- It drops the entire bdPFE database and recreates it empty so that
-- Hibernate can build the schema cleanly from the current entity classes.
--
-- WHY: Two tables unknown to Hibernate (enquete_satisfaction and
-- rapport_enquete_satisfaction) have FK constraints pointing at 'stage' and
-- 'utilisateur'. MySQL refuses to DROP those tables while the FKs exist,
-- so Hibernate would fail during its drop phase.
--
-- HOW TO RUN (from a terminal):
--   "C:\Program Files\MySQL\MySQL Workbench 8.0 CE\mysql.exe" -u root < recreate_db.sql
--
-- AFTER the app starts successfully, change ddl-auto back to "update".
-- =============================================================================

DROP DATABASE IF EXISTS bdPFE;

CREATE DATABASE bdPFE
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

-- Verify
SELECT SCHEMA_NAME, DEFAULT_CHARACTER_SET_NAME, DEFAULT_COLLATION_NAME
FROM   information_schema.SCHEMATA
WHERE  SCHEMA_NAME = 'bdPFE';
