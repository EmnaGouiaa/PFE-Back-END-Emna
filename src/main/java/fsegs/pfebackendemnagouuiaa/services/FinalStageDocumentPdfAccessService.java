package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.entities.CahierStage;
import fsegs.pfebackendemnagouuiaa.entities.FicheEvaluation;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Politique d'accès aux PDF de fin de stage (cahier et fiche d'évaluation).
 */
@Service
public class FinalStageDocumentPdfAccessService {

    public FinalStageDocumentPdfAccessResult evaluate(Stage stage, CahierStage cahierStage) {
        List<String> reasons = collectLogbookPdfBlockingReasons(stage, cahierStage);
        if (reasons.isEmpty()) {
            return FinalStageDocumentPdfAccessResult.allowed();
        }
        return FinalStageDocumentPdfAccessResult.blocked(reasons, joinReasons(reasons));
    }

    public FinalStageDocumentPdfAccessResult evaluate(Stage stage, FicheEvaluation fiche) {
        List<String> reasons = collectEvaluationPdfBlockingReasons(stage, fiche);
        if (reasons.isEmpty()) {
            return FinalStageDocumentPdfAccessResult.allowed();
        }
        return FinalStageDocumentPdfAccessResult.blocked(reasons, joinReasons(reasons));
    }

    public boolean isPdfAccessAllowed(Stage stage, CahierStage cahierStage) {
        return evaluate(stage, cahierStage).accessAllowed();
    }

    public boolean isPdfAccessAllowed(Stage stage, FicheEvaluation fiche) {
        return evaluate(stage, fiche).accessAllowed();
    }

    public void assertPdfAccessAllowed(Stage stage, CahierStage cahierStage) {
        FinalStageDocumentPdfAccessResult result = evaluate(stage, cahierStage);
        if (!result.accessAllowed()) {
            throw new BusinessException(result.message());
        }
    }

    public void assertPdfAccessAllowed(Stage stage, FicheEvaluation fiche) {
        FinalStageDocumentPdfAccessResult result = evaluate(stage, fiche);
        if (!result.accessAllowed()) {
            throw new BusinessException(result.message());
        }
    }

    public List<String> collectLogbookPdfBlockingReasons(Stage stage, CahierStage cahierStage) {
        List<String> reasons = new ArrayList<>();
        if (stage == null) {
            reasons.add("Stage introuvable.");
            return reasons;
        }
        if (stage.getStatut() == StatutStage.REFUSE || stage.getStatut() == StatutStage.ANNULE) {
            reasons.add("Le cahier de stage n'est pas disponible pour un stage refuse ou annule.");
            return reasons;
        }
        if (!isStageEndDateReached(stage)) {
            reasons.add("Le PDF du cahier de stage est accessible a partir de la date de fin du stage.");
        }
        if (cahierStage == null) {
            reasons.add("Le cahier de stage n'est pas encore disponible.");
            return reasons;
        }
        if (!cahierStage.estCompletementSigne()) {
            reasons.add("Toutes les signatures obligatoires du cahier de stage doivent etre completees.");
        }
        return reasons;
    }

    public List<String> collectEvaluationPdfBlockingReasons(Stage stage, FicheEvaluation fiche) {
        List<String> reasons = new ArrayList<>();
        if (stage == null) {
            reasons.add("Stage introuvable.");
            return reasons;
        }
        if (!EvaluationStageAccessRules.isEvaluationPeriodOpen(stage)) {
            reasons.add(EvaluationStageAccessRules.UNAVAILABLE_MESSAGE);
        }
        if (fiche == null) {
            reasons.add("La fiche d'evaluation n'est pas encore disponible.");
            return reasons;
        }
        if (!fiche.estCompletementSigne()) {
            reasons.add("Toutes les signatures obligatoires de la fiche d'evaluation doivent etre completees.");
        }
        return reasons;
    }

    public static boolean isStageEndDateReached(Stage stage) {
        LocalDate dateFin = EvaluationStageAccessRules.resolveDateFin(stage);
        return dateFin != null && !LocalDate.now().isBefore(dateFin);
    }

    private String joinReasons(List<String> reasons) {
        return reasons.stream()
                .filter(reason -> reason != null && !reason.isBlank())
                .collect(Collectors.joining(" "));
    }
}
