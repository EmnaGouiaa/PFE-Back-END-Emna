package fsegs.pfebackendemnagouuiaa.validation;

import java.time.LocalDate;

/**
 * Regles communes de calendrier pour offres et stages.
 *
 * <p>Periode 1 (academique) : debut ∈ [1er fevrier, 31 mai], fin ≤ 1er juin, duree max 3 mois.
 * Periode 2 (ete) : debut ∈ [1er juin, 31 aout], fin ≤ 1er septembre, duree max 2 mois.</p>
 */
public final class StagePeriodValidation {

    public static final int MAX_DURATION_MONTHS_ACADEMIC = 3;
    public static final int MAX_DURATION_MONTHS_SUMMER = 2;
    /** Borne haute globale (saisie / messages generiques). */
    public static final int MAX_DURATION_MONTHS = MAX_DURATION_MONTHS_ACADEMIC;
    public static final int MIN_DURATION_MONTHS = 1;

    private StagePeriodValidation() {
    }

    public enum PeriodKind {
        ACADEMIC("Période 1 (stage académique)"),
        SUMMER("Période 2 (stage été)");

        private final String label;

        PeriodKind(String label) {
            this.label = label;
        }

        public String label() {
            return label;
        }
    }

    public static int maxDurationMonths(PeriodKind kind) {
        return kind == PeriodKind.ACADEMIC
                ? MAX_DURATION_MONTHS_ACADEMIC
                : MAX_DURATION_MONTHS_SUMMER;
    }

    public static LocalDate calculateEndDate(LocalDate dateDebut, int dureeMonths) {
        if (dateDebut == null) {
            throw new IllegalArgumentException("La date de début est obligatoire.");
        }
        if (dureeMonths < MIN_DURATION_MONTHS) {
            throw new IllegalArgumentException(
                    "La durée doit être d'au moins " + MIN_DURATION_MONTHS + " mois.");
        }
        return dateDebut.plusMonths(dureeMonths);
    }

    /**
     * Nombre de mois calendaires entre {@code dateDebut} (inclus) et {@code dateFin} (inclus),
     * aligné sur {@link LocalDate#plusMonths(int)}.
     */
    public static int calculateDurationMonths(LocalDate dateDebut, LocalDate dateFin) {
        if (dateDebut == null || dateFin == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }
        if (dateFin.isBefore(dateDebut)) {
            throw new IllegalArgumentException("La date de fin ne peut pas être antérieure à la date de début.");
        }
        int months = 0;
        while (months < 12 && !dateDebut.plusMonths(months + 1).isAfter(dateFin)) {
            months++;
        }
        return Math.max(MIN_DURATION_MONTHS, months);
    }

    public static void validatePeriod(LocalDate dateDebut, Integer dureeMonths) {
        if (dateDebut == null) {
            throw new IllegalArgumentException("La date de début est obligatoire.");
        }
        if (dureeMonths == null) {
            throw new IllegalArgumentException("La durée du stage est obligatoire.");
        }
        PeriodKind kind = resolvePeriod(dateDebut);
        int maxDur = maxDurationMonths(kind);
        if (dureeMonths < MIN_DURATION_MONTHS || dureeMonths > maxDur) {
            throw new IllegalArgumentException(
                    kind.label() + " : la durée doit être comprise entre "
                            + MIN_DURATION_MONTHS + " et " + maxDur + " mois.");
        }
        validatePeriod(dateDebut, calculateEndDate(dateDebut, dureeMonths), dureeMonths);
    }

    public static void validatePeriod(LocalDate dateDebut, LocalDate dateFin) {
        int duree = calculateDurationMonths(dateDebut, dateFin);
        validatePeriod(dateDebut, dateFin, duree);
    }

    private static void validatePeriod(LocalDate dateDebut, LocalDate dateFin, int dureeMonths) {
        if (dateDebut.isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "La date de début du stage ne peut pas être dans le passé.");
        }

        PeriodKind kind = resolvePeriod(dateDebut);
        int year = dateDebut.getYear();

        LocalDate periodStart = periodStart(kind, year);
        LocalDate periodLastStart = periodLastStart(kind, year);
        LocalDate maxEnd = maxEndDate(kind, year);

        if (dateDebut.isBefore(periodStart) || dateDebut.isAfter(periodLastStart)) {
            throw new IllegalArgumentException(
                    kind.label() + " : la date de début doit être comprise entre le "
                            + formatFr(periodStart) + " et le " + formatFr(periodLastStart) + ".");
        }

        int maxDur = maxDurationMonths(kind);
        if (dureeMonths > maxDur) {
            throw new IllegalArgumentException(
                    kind.label() + " : la durée maximale autorisée est de " + maxDur + " mois.");
        }

        if (dateFin.isAfter(maxEnd)) {
            throw new IllegalArgumentException(
                    kind.label() + " : la date de fin (" + formatFr(dateFin)
                            + ") ne peut pas dépasser le " + formatFr(maxEnd) + ".");
        }
    }

    public static PeriodKind resolvePeriod(LocalDate dateDebut) {
        int month = dateDebut.getMonthValue();
        int day = dateDebut.getDayOfMonth();
        int year = dateDebut.getYear();

        if (!dateDebut.isBefore(LocalDate.of(year, 2, 1))
                && !dateDebut.isAfter(LocalDate.of(year, 5, 31))) {
            return PeriodKind.ACADEMIC;
        }
        if (!dateDebut.isBefore(LocalDate.of(year, 6, 1))
                && !dateDebut.isAfter(LocalDate.of(year, 8, 31))) {
            return PeriodKind.SUMMER;
        }

        if (month >= 2 && month <= 5) {
            return PeriodKind.ACADEMIC;
        }
        if (month >= 6 && month <= 8) {
            return PeriodKind.SUMMER;
        }

        throw new IllegalArgumentException(
                "La date de début doit appartenir à la période 1 (1er février → 31 mai) "
                        + "ou à la période 2 (1er juin → 31 août) de la même année.");
    }

    private static LocalDate periodStart(PeriodKind kind, int year) {
        return kind == PeriodKind.ACADEMIC
                ? LocalDate.of(year, 2, 1)
                : LocalDate.of(year, 6, 1);
    }

    private static LocalDate periodLastStart(PeriodKind kind, int year) {
        return kind == PeriodKind.ACADEMIC
                ? LocalDate.of(year, 5, 31)
                : LocalDate.of(year, 8, 31);
    }

    private static LocalDate maxEndDate(PeriodKind kind, int year) {
        return kind == PeriodKind.ACADEMIC
                ? LocalDate.of(year, 6, 1)
                : LocalDate.of(year, 9, 1);
    }

    private static String formatFr(LocalDate date) {
        return String.format("%02d/%02d/%04d", date.getDayOfMonth(), date.getMonthValue(), date.getYear());
    }
}
