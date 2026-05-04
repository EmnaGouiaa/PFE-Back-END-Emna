package fsegs.pfebackendemnagouuiaa.dto;

import fsegs.pfebackendemnagouuiaa.entities.PartieEvaluation;
import lombok.Data;

@Data
public class CritereEvaluationDto {
    private Long id;
    private String libelle;
    private String description;
    private String categorie;
    private Integer bareme;
    private String commentaireGeneral;
    private PartieEvaluation partie;
    private Long ficheId;
}