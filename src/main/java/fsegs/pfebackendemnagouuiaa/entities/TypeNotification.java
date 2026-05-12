package fsegs.pfebackendemnagouuiaa.entities;

import jakarta.persistence.EntityNotFoundException;

import java.util.Arrays;

public enum TypeNotification {
    REUNION_FIXEE,
    REUNION_SUIVI,
    VALIDATION_ENTREPRISE,
    AFFECTATION_ENCADRANT_STAGE,
    SIGNATURE_DOSSIER_STAGE,
    DEMANDE_ENTREPRISE_VALIDEE,
    DEMANDE_ENTREPRISE_REFUSEE,
    STAGE_AFFECTE,
    ENQUETE_SATISFACTION,
    OUVERTURE_ESPACES_FIN_STAGE;

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
