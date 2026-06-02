package fsegs.pfebackendemnagouuiaa.services;

import fsegs.pfebackendemnagouuiaa.entities.ConventionStage;
import fsegs.pfebackendemnagouuiaa.entities.Stage;
import fsegs.pfebackendemnagouuiaa.entities.StatutStage;
import fsegs.pfebackendemnagouuiaa.entities.StatutValidation;
import fsegs.pfebackendemnagouuiaa.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Politique d'accès au PDF de convention de stage.
 * <p>
 * La convention peut être générée dès la création du stage (hors refus / annulation).
 * La signature est gérée par {@link StageDocumentSignatureRules#ensureConventionSigningAllowed}
 * (stage {@link fsegs.pfebackendemnagouuiaa.entities.StatutStage#EN_COURS} uniquement).
 * L'accès au PDF final exige le sujet validé et toutes les signatures complètes.
 */
@Service
public class ConventionStagePdfAccessService {

    public FinalStageDocumentPdfAccessResult evaluate(Stage stage, ConventionStage convention) {
        List<String> reasons = collectBlockingReasons(stage, convention);
        if (reasons.isEmpty()) {
            return FinalStageDocumentPdfAccessResult.allowed();
        }
        return FinalStageDocumentPdfAccessResult.blocked(reasons, joinReasons(reasons));
    }

    public boolean isPdfAccessAllowed(Stage stage, ConventionStage convention) {
        return evaluate(stage, convention).accessAllowed();
    }

    public void assertPdfAccessAllowed(Stage stage, ConventionStage convention) {
        FinalStageDocumentPdfAccessResult result = evaluate(stage, convention);
        if (!result.accessAllowed()) {
            throw new BusinessException(result.message());
        }
    }

    public List<String> collectBlockingReasons(Stage stage, ConventionStage convention) {
        List<String> reasons = new ArrayList<>();
        if (stage == null) {
            reasons.add("Stage introuvable. Acces PDF convention refuse.");
            return reasons;
        }
        if (stage.getStatut() == StatutStage.REFUSE || stage.getStatut() == StatutStage.ANNULE) {
            reasons.add("La convention n'est pas disponible pour un stage refuse ou annule.");
            return reasons;
        }
        if (stage.getStatutSujet() != StatutValidation.VALIDEE) {
            reasons.add("Le sujet de stage doit etre valide avant l'acces a la convention.");
        }
        if (convention == null) {
            reasons.add("La convention de stage n'est pas encore disponible.");
            return reasons;
        }
        if (!convention.estCompletementSigne()) {
            reasons.add("Toutes les signatures obligatoires de la convention doivent etre completees.");
        }
        return reasons;
    }

    private String joinReasons(List<String> reasons) {
        return reasons.stream()
                .filter(reason -> reason != null && !reason.isBlank())
                .collect(Collectors.joining(" "));
    }
}
