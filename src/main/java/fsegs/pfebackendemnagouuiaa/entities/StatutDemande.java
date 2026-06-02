package fsegs.pfebackendemnagouuiaa.entities;

/**
 * Statut global d'une {@link DemandeCreationCompteEntreprise} (circuit entreprise / admin).
 */
public enum StatutDemande {
    /** Demande déposée, non traitée. */
    EN_ATTENTE,
    /** Demande acceptée — création entreprise et stage possibles. */
    VALIDEE,
    /** Demande rejetée. */
    REFUSEE
}