package fsegs.pfebackendemnagouuiaa.service;

import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Réunion finale : seul l'horaire ({@code heure}) est modifiable après création automatique.
 */
public final class FinalMeetingUpdatePolicy {

    public static final String ERROR_TIME_ONLY =
            "Pour une réunion finale, seul l'horaire peut être modifié.";

    public static final String ERROR_OBSERVATION_FORBIDDEN =
            "Les observations ne sont pas autorisées pour une réunion finale.";

    private FinalMeetingUpdatePolicy() {
    }

    /**
     * Vérifie que la requête ne tente pas de modifier des champs verrouillés (hors heure).
     */
    public static void assertLockedFieldsUnchanged(ReunionFinale entity,
                                                   Long requestedStageId,
                                                   String requestedNumReunion,
                                                   String requestedCompteRendu,
                                                   Set<Long> requestedParticipantIds) {
        if (entity == null) {
            throw new IllegalArgumentException(ERROR_TIME_ONLY);
        }

        if (requestedStageId != null
                && entity.getStage() != null
                && entity.getStage().getId() != null
                && !Objects.equals(requestedStageId, entity.getStage().getId())) {
            throw new IllegalArgumentException(ERROR_TIME_ONLY);
        }

        if (requestedNumReunion != null
                && entity.getNumReunion() != null
                && !Objects.equals(requestedNumReunion.trim(), entity.getNumReunion().trim())) {
            throw new IllegalArgumentException(ERROR_TIME_ONLY);
        }

        if (requestedCompteRendu != null) {
            String current = normalizeNullable(requestedCompteRendu);
            String existing = normalizeNullable(entity.getCompteRendu());
            if (!Objects.equals(current, existing)) {
                throw new IllegalArgumentException(ERROR_TIME_ONLY);
            }
        }

        if (requestedParticipantIds != null && !requestedParticipantIds.isEmpty()) {
            Set<Long> existingIds = entity.getParticipants() == null
                    ? Set.of()
                    : entity.getParticipants().stream()
                    .filter(Objects::nonNull)
                    .map(Utilisateur::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!existingIds.equals(requestedParticipantIds.stream().filter(Objects::nonNull).collect(Collectors.toSet()))) {
                throw new IllegalArgumentException(ERROR_TIME_ONLY);
            }
        }
    }

    /**
     * La date et l'observation ne doivent pas changer ; seule l'heure peut différer.
     */
    public static void assertOnlyHeureMayChange(ReunionFinale entity,
                                                LocalDate requestedDate,
                                                LocalTime requestedHeure,
                                                String requestedObservation) {
        if (entity == null) {
            throw new IllegalArgumentException(ERROR_TIME_ONLY);
        }

        if (requestedDate != null && entity.getDate() != null && !Objects.equals(requestedDate, entity.getDate())) {
            throw new IllegalArgumentException(ERROR_TIME_ONLY);
        }

        if (requestedObservation != null) {
            String requested = normalizeNullable(requestedObservation);
            String existing = normalizeNullable(entity.getObservation());
            if (!Objects.equals(requested, existing)) {
                throw new IllegalArgumentException(ERROR_TIME_ONLY);
            }
        }

        requireHeure(requestedHeure);
    }

    public static void requireHeure(LocalTime requestedHeure) {
        if (requestedHeure == null) {
            throw new IllegalArgumentException("L'heure de la réunion finale est obligatoire.");
        }
    }

    private static String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
