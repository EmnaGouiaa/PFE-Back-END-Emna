package fsegs.pfebackendemnagouuiaa.entities;

/**
 * Identifiant de la partie signataire sur un document ({@link Signature}).
 * Distinct de {@link Role} : un même utilisateur peut signer sous un rôle documentaire précis.
 *
 * <p>Utilisé par {@link ConventionStage}, {@link CahierStage} et {@link FicheEvaluation}
 * pour vérifier l'exhaustivité des signatures ({@code estSignePar}, {@code estCompletementSigne}).</p>
 */
public enum RoleSignature {
    /** Encadrant académique du stage. */
    ENCADRANT_ACADEMIQUE,
    /** Encadrant professionnel en entreprise. */
    ENCADRANT_PROFESSIONNEL,
    /** Représentant / responsable de l'entreprise d'accueil. */
    RESPONSABLE_ENTREPRISE,
    /** Signataire côté établissement (convention uniquement, 5e signature). */
    RESPONSABLE_UNIVERSITAIRE,
    /** Stagiaire concerné. */
    STAGIAIRE
}
