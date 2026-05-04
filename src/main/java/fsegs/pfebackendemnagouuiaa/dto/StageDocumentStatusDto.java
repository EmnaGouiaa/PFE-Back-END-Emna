package fsegs.pfebackendemnagouuiaa.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StageDocumentStatusDto {
    private String code;
    private String libelle;
    private Long documentId;
    private Boolean disponible;
    private Boolean genere;
    private Boolean generationAutorisee;
    private String statut;
    private String raisonAbsence;
    private Boolean signeeParResponsableUniversitaire;
    private String dateSignatureResponsableUniversitaire;
}
