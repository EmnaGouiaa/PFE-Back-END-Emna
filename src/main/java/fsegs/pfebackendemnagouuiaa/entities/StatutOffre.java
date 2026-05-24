package fsegs.pfebackendemnagouuiaa.entities;

/**
 * Statuts internes du workflow d'une offre de stage.
 * Cote interface utilisateur, ces statuts sont projetes sur un vocabulaire simplifie
 * (Publie, En cours de stage, Termine, Archive) via les mappers du frontend.
 */
public enum StatutOffre {
    EN_ATTENTE,
    PUBLIEE,
    VALIDEE,
    AFFECTEE,
    REFUSEE,
    FERMEE,
    /**
     * @deprecated Conserve uniquement pour la compatibilite avec d'eventuelles lignes
     * historiques en base. Le nouveau modele utilise TERMINEE comme unique etat final.
     * Le code traite ARCHIVEE comme un alias de TERMINEE (affichage et regles d'edition).
     */
    @Deprecated
    ARCHIVEE,

    /**
     * Etat final unique du cycle de vie d'une offre. Pose automatiquement par le systeme
     * quand :
     *   - l'offre est affectee a un etudiant ET le sujet est valide par l'encadrant
     *     academique ET la date de fin du stage est atteinte, OU
     *   - la date de debut de l'offre est depassee sans qu'aucun etudiant ne soit affecte
     *     (anciennement archivee, desormais TERMINEE).
     * Une offre TERMINEE est en lecture seule, mais reste consultable par le Responsable
     * Entreprise et le Responsable des Stages Universitaires.
     */
    TERMINEE
}
