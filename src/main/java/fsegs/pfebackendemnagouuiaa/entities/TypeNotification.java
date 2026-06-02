package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.EntityNotFoundException;

import java.util.Arrays;

/**
 * Catégorie métier d'une {@link Notification}, utilisée pour le routage et les modèles de message.
 * Persistée en {@code type_notification} (chaîne ENUM).
 */
public enum TypeNotification {
    /** Convocation à une réunion planifiée. */
    REUNION_FIXEE,
    /** Rappel ou compte rendu de suivi de réunion. */
    REUNION_SUIVI,
    /** Entreprise validée par l'administration universitaire. */
    VALIDATION_ENTREPRISE,
    /** Encadrant de stage (service universitaire) affecté. */
    AFFECTATION_ENCADRANT_STAGE,
    /** Encadrant académique assigné au stagiaire. */
    AFFECTATION_ENCADRANT_ACADEMIQUE,
    /** Changement d'encadrant académique. */
    MODIFICATION_ENCADRANT_ACADEMIQUE,
    /** Encadrant professionnel assigné au stage. */
    AFFECTATION_ENCADRANT_PROFESSIONNEL,
    /** Changement d'encadrant professionnel. */
    MODIFICATION_ENCADRANT_PROFESSIONNEL,
    /** Demande de signature sur un document du dossier. */
    SIGNATURE_DOSSIER_STAGE,
    /** Demande de création de compte entreprise acceptée. */
    DEMANDE_ENTREPRISE_VALIDEE,
    /** Demande de création de compte entreprise refusée. */
    DEMANDE_ENTREPRISE_REFUSEE,
    /** Stagiaire affecté à une offre ou un stage. */
    STAGE_AFFECTE,
    /** Invitation à répondre à l'enquête de satisfaction. */
    ENQUETE_SATISFACTION,
    /** Publication de l'enquête par le responsable des stages. */
    PUBLICATION_ENQUETE,
    /** Rapport de stage déposé par le stagiaire. */
    DEPOT_RAPPORT,
    /** Ouverture des espaces de fin de stage (évaluation, etc.). */
    OUVERTURE_ESPACES_FIN_STAGE,
    /** Sujet de stage validé par l'encadrant académique. */
    VALIDATION_SUJET,
    /** Sujet de stage refusé. */
    REFUS_SUJET,
    /** Transition automatique ou manuelle du {@link StatutStage}. */
    CHANGEMENT_STATUT_STAGE;

    /**
     * Résout une constante à partir de son nom (insensible à la casse).
     *
     * @param value nom de l'énumération
     * @return la constante correspondante
     * @throws jakarta.persistence.EntityNotFoundException si {@code value} est vide ou inconnu
     */
    public static TypeNotification fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new EntityNotFoundException("TypeNotification est obligatoire.");
        }

        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("TypeNotification introuvable : " + value));
    }
}
