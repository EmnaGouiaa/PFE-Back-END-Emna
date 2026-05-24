package fsegs.pfebackendemnagouuiaa.entities;

/**
 * Représente le cycle de vie d'un document de stage (convention, cahier, fiche d'évaluation).
 *
 * <ul>
 *   <li>{@link #BROUILLON}            – Le document a été initialisé mais n'est pas encore signé.</li>
 *   <li>{@link #EN_ATTENTE_SIGNATURES} – Au moins une signature obligatoire manque encore.</li>
 *   <li>{@link #SIGNATURES_COMPLETES}  – Toutes les signatures obligatoires sont présentes.</li>
 *   <li>{@link #DISPONIBLE_IMPRESSION} – Le document est prêt à être généré en PDF et téléchargé.</li>
 * </ul>
 *
 * La transition de {@code SIGNATURES_COMPLETES} à {@code DISPONIBLE_IMPRESSION} est automatique :
 * aucune validation manuelle par le responsable des stages n'est requise.
 */
public enum StatutDocument {

    /** Document créé, formulaire non encore renseigné ou partiellement rempli. */
    BROUILLON,

    /** Au moins une signature obligatoire est encore manquante. */
    EN_ATTENTE_SIGNATURES,

    /** Toutes les signatures obligatoires sont présentes. */
    SIGNATURES_COMPLETES,

    /**
     * Toutes les conditions sont réunies (signatures complètes + données valides) :
     * le PDF peut être généré et téléchargé par tous les participants concernés.
     */
    DISPONIBLE_IMPRESSION;

    /**
     * Retourne le libellé d'affichage destiné à l'interface utilisateur.
     */
    public String libelle() {
        return switch (this) {
            case BROUILLON -> "Brouillon";
            case EN_ATTENTE_SIGNATURES -> "En attente de signatures";
            case SIGNATURES_COMPLETES -> "Signatures complètes";
            case DISPONIBLE_IMPRESSION -> "Disponible à l'impression";
        };
    }
}
