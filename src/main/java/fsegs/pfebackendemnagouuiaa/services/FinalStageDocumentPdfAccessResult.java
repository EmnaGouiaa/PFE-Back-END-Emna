package fsegs.pfebackendemnagouuiaa.services;

import java.util.List;

/**
 * Résultat de la politique d'accès aux PDF finaux (cahier de stage, fiche d'évaluation).
 */
public record FinalStageDocumentPdfAccessResult(
        boolean accessAllowed,
        List<String> blockingReasons,
        String message
) {
    public static FinalStageDocumentPdfAccessResult allowed() {
        return new FinalStageDocumentPdfAccessResult(true, List.of(), "");
    }

    public static FinalStageDocumentPdfAccessResult blocked(List<String> blockingReasons, String message) {
        return new FinalStageDocumentPdfAccessResult(false, List.copyOf(blockingReasons), message);
    }
}
