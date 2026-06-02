package fsegs.pfebackendemnagouuiaa.entities;

/**
 * Rôle applicatif d'un {@link Utilisateur}, mappé en autorité Spring {@code ROLE_<nom>}.
 * Persisté en colonne {@code role} (chaîne) sur la table utilisateur.
 *
 * <p>Utilisé par la sécurité ({@code ServiceAuthentification}), les contrôles
 * {@code @PreAuthorize} et la création de comptes ({@code UtilisateurServiceImpl}).</p>
 */
public enum Role {
    /** Administration globale du système. */
    ADMINISTRATEUR,
    /** Encadrant terrain en entreprise ({@link EncadrantProfessionnel}). */
    ENCADRANT_PROFESSIONNEL,
    /** Encadrant universitaire ({@link EncadrantAcademique}). */
    ENCADRANT_ACADEMIQUE,
    /** Agent du service des stages (droits opérationnels limités). */
    AGENT_STAGE,
    /** Responsable du service des stages universitaire ({@link ResponsableServiceStages}). */
    RESPONSABLE_STAGE,
    /** Représentant entreprise ({@link ResponsableEntreprise}). */
    RESPONSABLE_ENTREPRISE,
    /** Étudiant en stage ({@link Stagiaire}). */
    STAGIAIRE
}