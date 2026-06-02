package fsegs.pfebackendemnagouuiaa.entities;

/**
 * Résultat d'une validation (sujet de stage, demande côté service des stages, etc.).
 * Utilisé sur {@link Stage#statutSujet} et {@link DemandeCreationCompteEntreprise#statutResponsableStages}.
 */
public enum StatutValidation {
    /** En attente de décision. */
    EN_ATTENTE,
    /** Accepté / validé. */
    VALIDEE,
    /** Refusé avec motif éventuel en commentaire. */
    REFUSEE
}