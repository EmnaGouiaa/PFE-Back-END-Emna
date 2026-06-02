# Cahier de Stage – Règles Métier

Document de référence aligné sur l'implémentation (`CahierStage`, `StageDocumentServiceImpl`, `CahierStagePdfService`).

## 1. Informations générales

Titre, sujet, dates de début/fin, stagiaire, encadrants académique et professionnel, entreprise d'accueil.

## 2. Signatures (4 obligatoires)

Stagiaire, encadrant académique, encadrant professionnel, responsable entreprise.

- Une signature par rôle, horodatage `LocalDateTime` enregistré.
- Statut visible dans l'UI et le PDF.
- PDF autorisé uniquement si les 4 signatures sont présentes.

## 3. Réunions hebdomadaires

Toutes les réunions hebdomadaires du stage, tri chronologique. Colonnes : numéro, date, heure, observation de l'encadrant créateur.

## 4. Trello

Synchronisation automatique ; affichage en 3 colonnes : **A faire**, **En cours**, **Terminé** (UI + PDF).

## 5. Absences

Date, nombre de jours, statut (`JUSTIFIEE` / `NON_JUSTIFIEE`), justification, commentaire.

## 6. Génération PDF

**Conditions :** cahier existant, 4 signatures, date de fin du stage atteinte ou dépassée.

**API :** `GET /api/stages/{id}/documents/cahier-stage/pdf` — messages explicites via `collectLogbookPdfBlockingReasons`.

**Contenu UI :** `GET /api/cahiers-stage/stage/{stageId}/contenu`
