package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.EntityNotFoundException;

import java.util.Arrays;

public enum TypeNotification {
    REUNION_FIXEE,
    REUNION_SUIVI,
    VALIDATION_ENTREPRISE,
    AFFECTATION_ENCADRANT_STAGE,
    AFFECTATION_ENCADRANT_ACADEMIQUE,
    MODIFICATION_ENCADRANT_ACADEMIQUE,
    AFFECTATION_ENCADRANT_PROFESSIONNEL,
    MODIFICATION_ENCADRANT_PROFESSIONNEL,
    SIGNATURE_DOSSIER_STAGE,
    DEMANDE_ENTREPRISE_VALIDEE,
    DEMANDE_ENTREPRISE_REFUSEE,
    STAGE_AFFECTE,
    ENQUETE_SATISFACTION,
    PUBLICATION_ENQUETE,
    DEPOT_RAPPORT,
    OUVERTURE_ESPACES_FIN_STAGE,
    VALIDATION_SUJET,
    REFUS_SUJET,
    CHANGEMENT_STATUT_STAGE;

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
