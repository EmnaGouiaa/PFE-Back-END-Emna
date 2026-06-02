package fsegs.pfebackendemnagouuiaa.service;

import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.Role;
import fsegs.pfebackendemnagouuiaa.entities.Utilisateur;
import org.springframework.security.access.AccessDeniedException;

/**
 * Observations personnelles sur les réunions hebdomadaires.
 * L'encadrant académique ou professionnel peut ajouter et modifier l'observation
 * uniquement sur les réunions qu'il a lui-même créées.
 */
public final class WeeklyMeetingObservationPolicy {

    public static final String ERROR_OBSERVATION_CREATOR_ONLY =
            "Seul l'encadrant créateur de la réunion peut consulter ou modifier l'observation.";

    public static final String ERROR_OBSERVATION_ROLE_UNSUPPORTED =
            "Seuls les encadrants académique et professionnel peuvent renseigner une observation.";

    private WeeklyMeetingObservationPolicy() {
    }

    public static void assertCanManageObservation(Utilisateur utilisateur, Reunion reunion) {
        if (reunion == null || reunion.getStage() == null || utilisateur == null) {
            throw new AccessDeniedException(ERROR_OBSERVATION_CREATOR_ONLY);
        }
        if (!MeetingVisibilityRules.isSupervisor(utilisateur)) {
            throw new AccessDeniedException(ERROR_OBSERVATION_ROLE_UNSUPPORTED);
        }
        if (!MeetingVisibilityRules.isMeetingCreator(reunion, utilisateur)) {
            throw new AccessDeniedException(ERROR_OBSERVATION_CREATOR_ONLY);
        }
    }

    public static String readObservationForUser(Reunion reunion, Utilisateur utilisateur) {
        if (reunion == null || utilisateur == null) {
            return null;
        }
        if (!MeetingVisibilityRules.isMeetingCreator(reunion, utilisateur)) {
            return null;
        }

        String observation = readObservationForCreatorType(reunion);
        if (observation != null && !observation.isBlank()) {
            return observation;
        }
        String legacy = reunion.getObservation();
        return legacy != null && !legacy.isBlank() ? legacy.trim() : null;
    }

    public static void writeObservationForUser(Reunion reunion, Utilisateur utilisateur, String observation) {
        assertCanManageObservation(utilisateur, reunion);
        writeObservationForCreatorType(reunion, observation);
    }

    private static String readObservationForCreatorType(Reunion reunion) {
        return switch (normalizeCreatorType(reunion)) {
            case "ACADEMIQUE" -> reunion.getObservationEncadrantAcademique();
            case "PROFESSIONNEL" -> reunion.getObservationEncadrantProfessionnel();
            default -> null;
        };
    }

    private static void writeObservationForCreatorType(Reunion reunion, String observation) {
        switch (normalizeCreatorType(reunion)) {
            case "ACADEMIQUE" -> reunion.setObservationEncadrantAcademique(observation);
            case "PROFESSIONNEL" -> reunion.setObservationEncadrantProfessionnel(observation);
            default -> throw new IllegalArgumentException(
                    "Type d'encadrant créateur invalide pour l'observation de la réunion.");
        }
    }

    private static String normalizeCreatorType(Reunion reunion) {
        if (reunion == null || reunion.getTypeEncadrantCreateur() == null) {
            return "";
        }
        return reunion.getTypeEncadrantCreateur().trim().toUpperCase();
    }
}
