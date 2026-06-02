package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageDocumentsOverviewDto {
    private Long stageId;
    private String stageTitre;
    private String stageStatut;
    /** Date de fin du stage (règle d'accès PDF : accessible à partir de cette date). */
    private java.time.LocalDate dateFinStage;
    private String stagiaireNom;
    private String entrepriseNom;
    private String encadrantAcademiqueNom;
    private String encadrantProfessionnelNom;
    private StageDocumentStatusDto convention;
    private StageDocumentStatusDto ficheEvaluation;
    private StageDocumentStatusDto cahierStage;
}
