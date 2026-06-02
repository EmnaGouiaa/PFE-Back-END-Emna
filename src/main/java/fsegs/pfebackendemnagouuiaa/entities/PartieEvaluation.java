package fsegs.pfebackendemnagouuiaa.entities;

/**
 * Section évaluatrice d'une {@link FicheEvaluation} : détermine quels {@link CritereEvaluation}
 * et quelles notes relèvent de l'encadrant professionnel ou du responsable entreprise.
 */
public enum PartieEvaluation {
    /** Critères et textes saisis par l'encadrant professionnel. */
    ENCADRANT_PROFESSIONNEL,
    /** Critères et textes saisis par le responsable entreprise. */
    RESPONSABLE_ENTREPRISE
}