package fsegs.pfebackendemnagouuiaa.entities;

/**
 * Statut d'un stage. Les transitions A_VENIR ↔ EN_COURS ↔ TERMINE sont calculees
 * automatiquement par le systeme a partir des dates (cf.
 * {@code StageServiceImpl.appliquerStatutMetier}). Les etats ANNULE et REFUSE sont
 * manuels et verrouillent toute auto-transition.
 */
public enum StatutStage {
    /**
     * @deprecated Conserve pour compatibilite des donnees historiques. Le nouveau modele
     * utilise {@link #A_VENIR} pour "stage prevu, pas encore commence". Cote affichage,
     * PAS_COMMENCE est traite comme un alias de A_VENIR.
     */
    @Deprecated
    PAS_COMMENCE,
    /** Aujourd'hui < date de debut prevue. */
    A_VENIR,
    ANNULE,
    REFUSE,
    /** Date de debut atteinte et date de fin pas encore depassee. */
    EN_COURS,
    /** Date de fin atteinte ou depassee. */
    TERMINE
}
