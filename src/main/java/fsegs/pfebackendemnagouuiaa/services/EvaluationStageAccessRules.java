package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.exception.BusinessException;

import java.time.LocalDate;

/**
 * Règle métier : la fiche d'évaluation n'est accessible qu'à partir de la date de fin du stage
 * ({@code date du jour >= dateFinStage}, bornes incluses).
 */
public final class EvaluationStageAccessRules {

    public static final String UNAVAILABLE_MESSAGE =
            "La fiche d'évaluation sera disponible après la fin du stage.";

    private EvaluationStageAccessRules() {
    }

    public static boolean isEvaluationPeriodOpen(Stage stage) {
        LocalDate dateFin = resolveDateFin(stage);
        return dateFin != null && !LocalDate.now().isBefore(dateFin);
    }

    public static void ensureEvaluationPeriodOpen(Stage stage) {
        if (!isEvaluationPeriodOpen(stage)) {
            throw new BusinessException(UNAVAILABLE_MESSAGE);
        }
    }

    public static LocalDate resolveDateFin(Stage stage) {
        if (stage == null) {
            return null;
        }
        return stage.getDateFin();
    }
}
