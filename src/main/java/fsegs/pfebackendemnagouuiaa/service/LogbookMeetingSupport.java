package fsegs.pfebackendemnagouuiaa.service;

import fsegs.pfebackendemnagouuiaa.entities.Reunion;
import fsegs.pfebackendemnagouuiaa.entities.ReunionFinale;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * Réunions hebdomadaires et observations pour le cahier de stage / PDF.
 */
public final class LogbookMeetingSupport {

    private LogbookMeetingSupport() {
    }

    public static boolean isWeeklyMeeting(Reunion reunion) {
        return reunion != null && !(reunion instanceof ReunionFinale);
    }

    public static Stream<Reunion> weeklyMeetingsSorted(List<Reunion> reunions) {
        if (reunions == null) {
            return Stream.empty();
        }
        return reunions.stream()
                .filter(LogbookMeetingSupport::isWeeklyMeeting)
                .sorted(Comparator.comparing(Reunion::getDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Reunion::getHeure, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Reunion::getId, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    /**
     * Observation de l'encadrant créateur de la réunion (champs séparés académique / professionnel).
     */
    public static String resolveCreatorObservation(Reunion reunion) {
        if (reunion == null) {
            return "";
        }
        String creatorType = reunion.getTypeEncadrantCreateur() != null
                ? reunion.getTypeEncadrantCreateur().trim().toUpperCase()
                : "";
        if ("ACADEMIQUE".equals(creatorType)) {
            return firstNonBlank(reunion.getObservationEncadrantAcademique(), reunion.getObservation());
        }
        if ("PROFESSIONNEL".equals(creatorType)) {
            return firstNonBlank(reunion.getObservationEncadrantProfessionnel(), reunion.getObservation());
        }
        return firstNonBlank(
                reunion.getObservationEncadrantAcademique(),
                reunion.getObservationEncadrantProfessionnel(),
                reunion.getObservation()
        );
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return "";
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
