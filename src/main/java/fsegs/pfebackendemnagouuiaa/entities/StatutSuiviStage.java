package fsegs.pfebackendemnagouuiaa.entities;

import java.util.Locale;

/**
 * Statut métier de suivi exposé à l'UI (libellés lisibles), dérivé du {@link StatutStage} technique.
 */
public enum StatutSuiviStage {
    NON_COMMENCE("Non commencé"),
    EN_COURS("En cours"),
    TERMINE("Terminé"),
    REFUSE("Refusé");

    private final String libelle;

    StatutSuiviStage(String libelle) {
        this.libelle = libelle;
    }

    public String getLibelle() {
        return libelle;
    }

    public static StatutSuiviStage from(StatutStage statut) {
        if (statut == null) {
            return NON_COMMENCE;
        }
        return switch (statut) {
            case TERMINE -> TERMINE;
            case REFUSE, ANNULE -> REFUSE;
            case EN_COURS -> EN_COURS;
            case A_VENIR, PAS_COMMENCE -> NON_COMMENCE;
        };
    }

    /**
     * Filtre API {@code GET /api/stages?statutSuivi=...} (valeurs : NON_COMMENCE, EN_COURS, TERMINE, REFUSE).
     */
    public static StatutSuiviStage parseFilter(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "NON_COMMENCE", "A_VENIR", "PAS_COMMENCE" -> NON_COMMENCE;
            case "EN_COURS" -> EN_COURS;
            case "TERMINE" -> TERMINE;
            case "REFUSE", "ANNULE" -> REFUSE;
            default -> null;
        };
    }
}
