package fsegs.pfebackendemnagouuiaa.entities;

/**
 * Statut d'un stage. Les transitions A_VENIR ↔ EN_COURS ↔ TERMINE sont calculees
 * automatiquement par le systeme a partir des dates (cf.
 * {@code StageServiceImpl.appliquerStatutMetier}). L'etat ANNULE est manuel.
 * REFUSE declenche une suppression immediate du stage (plus de persistance en base).
 *
 * <p>Utilisé par {@code StageServiceImpl}, {@code StageRepository} et les contrôleurs stage.</p>
 */
public enum StatutStage {
    /**
     * @deprecated Conserve pour compatibilite des donnees historiques. Le nouveau modele
     * utilise {@link #A_VENIR} pour "stage prevu, pas encore commence". Cote affichage,
     * PAS_COMMENCE est traite comme un alias de A_VENIR.
     */
    @Deprecated
    PAS_COMMENCE,
    /** Aujourd'hui strictement avant la date de debut (affichage : non commencé). */
    A_VENIR,
    /** Stage annulé avant ou pendant le déroulement. */
    ANNULE,
    /** Stage refusé (ex. sujet ou affectation rejetée). */
    REFUSE,
    /** Date de debut atteinte et date de fin pas encore depassee. */
    EN_COURS,
    /** Date de fin atteinte ou depassee. */
    TERMINE
}
